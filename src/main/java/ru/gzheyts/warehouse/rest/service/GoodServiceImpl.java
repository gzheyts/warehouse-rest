package ru.gzheyts.warehouse.rest.service;

import ru.gzheyts.warehouse.rest.model.GoodPayload;
import ru.gzheyts.warehouse.rest.model.LoadResponse;
import ru.gzheyts.warehouse.rest.model.ShipResponse;
import ru.gzheyts.warehouse.rest.model.StatResponse;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * @author gzheyts
 */

@Service
@Transactional
public class GoodServiceImpl implements GoodService {
    public static final Calendar tzUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private static final String QUERY_INSERT_GOOD = "INSERT INTO good (loadtime, articleid, shipped) VALUES (?, ?, ?)";

    private static final String QUERY_FETCH_ARTICLE_AMOUNT = "SELECT\n" +
            "  articleid,\n" +
            "  count(articleid)\n" +
            "FROM good\n" +
            "WHERE NOT shipped AND articleid IN (:list)\n" +
            "GROUP BY articleid ";

    private static final String QUERY_FETCH_GOOD_IDS_TO_SHIP = "SELECT\n" +
            "  id,\n" +
            "  articleid,\n" +
            "  loadtime\n" +
            "FROM good\n" +
            "WHERE articleid = ? AND NOT shipped\n" +
            "ORDER BY loadtime\n" +
            "LIMIT ? ";

    private static final String QUERY_SHIP_GOODS_BY_IDS = "UPDATE good\n" +
            "SET shipped = TRUE, shiptime = :shipTime\n" +
            "WHERE id IN (:goods) ";

    private static final String QUERY_FETCH_STATISTIC = "SELECT\n" +
            "  articleid,\n" +
            "  sum(CASE when ((shiptime is null) or (shiptime > to_timestamp(:timestamp)))\n" +
            "    THEN 1\n" +
            "      ELSE 0 END)                                                AS available_size,\n" +
            "  sum(CASE WHEN (shipped and shiptime < to_timestamp(:timestamp))\n" +
            "    THEN (:timestamp0 - (extract(EPOCH FROM loadtime)))\n" +
            "      ELSE 0 END) AS stock_time,\n" +
            "  sum(CASE WHEN (shipped and shiptime < to_timestamp(:timestamp))\n" +
            "    THEN 1\n" +
            "      ELSE 0 END)                                                AS stock_size\n" +
            "FROM good\n" +
            "WHERE articleid = :articleId AND loadtime <= to_timestamp(:timestamp)\n" +
            "GROUP BY articleid;\n";

    private final Logger logger = Logger.getLogger(GoodServiceImpl.class);


    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    public void configure(DataSource dataSource) {
        namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

    }

    @Transactional
    @Override
    public LoadResponse loadGoods(final GoodPayload payload) {
        final long timeInMillis = Instant.now().getMillis();

        LoadResponse response = new LoadResponse(timeInMillis);
        for (final Map.Entry<Long, Integer> entry : payload.getGoods().entrySet()) {
            namedJdbcTemplate.getJdbcOperations().batchUpdate(QUERY_INSERT_GOOD,
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setTimestamp(1, new Timestamp(timeInMillis), tzUTC);
                            ps.setLong(2, entry.getKey());
                            ps.setBoolean(3, false);
                        }

                        @Override
                        public int getBatchSize() {
                            return entry.getValue();
                        }
                    });
        }

        return response;
    }

    @Transactional
    @Override
    public ShipResponse shipGoods(final GoodPayload payload) throws NoSuchArticleException {

        long timeInMillis = Instant.now().getMillis();

        ShipResponse response = new ShipResponse(timeInMillis);

        response = namedJdbcTemplate.query(QUERY_FETCH_ARTICLE_AMOUNT,
                new MapSqlParameterSource("list", payload.getGoods().keySet()),
                new DiffExtractor(payload.getGoods(), response));

        if (response.getNoEnoughGoods() == null) {
            doShip(payload, timeInMillis, response);
            response.setStatus(ShipResponse.ShipStatus.SHIPPED);
        } else {
            response.setStatus(ShipResponse.ShipStatus.NOT_SHIPPED);
        }

        return response;
    }

    private void doShip(final GoodPayload payload, long timeInMillis, final ShipResponse response) throws NoSuchArticleException {
        final List<Long> goodsToShip = new ArrayList<>();

            /* fetch good to ship ids */
        for (Map.Entry<Long, Integer> entry : payload.getGoods().entrySet()) {
            Collection<Long> goodIds = namedJdbcTemplate.getJdbcOperations().query(QUERY_FETCH_GOOD_IDS_TO_SHIP,
                    new Object[]{/* article id */ entry.getKey(), /* goods count */ entry.getValue()},
                    new ShipStatCollector(response)
            );

            goodsToShip.addAll(goodIds);
        }

        if (goodsToShip.isEmpty()) {
            throw new NoSuchArticleException();
        }

        logger.info("remove good ids: " + goodsToShip);

        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("shipTime", new Timestamp(timeInMillis), Types.TIMESTAMP);
        mapSqlParameterSource.addValue("goods", goodsToShip);
        namedJdbcTemplate.update(QUERY_SHIP_GOODS_BY_IDS, mapSqlParameterSource);
    }

    @Transactional
    @Override
    public StatResponse queryStatistics(final Long articleId, final Long timestamp) {

        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

        mapSqlParameterSource.addValue("timestamp0", timestamp * 100);
        mapSqlParameterSource.addValue("timestamp", timestamp);
        mapSqlParameterSource.addValue("articleId", articleId );

        return namedJdbcTemplate.queryForObject(QUERY_FETCH_STATISTIC,
                mapSqlParameterSource,
                new RowMapper<StatResponse>() {
                    @Override
                    public StatResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
                        int available_size = rs.getInt(2);// available_size
                        long stock_time = rs.getLong(3);// stock_time
                        int stock_size = rs.getInt(4);// stock_size
                        return new StatResponse(available_size, stock_size == 0 ? 0 : Math.round((stock_time / stock_size) / 100L));
                    }
                }
        );

    }


    private static class DiffExtractor implements ResultSetExtractor<ShipResponse> {

        private ShipResponse response;
        private Map<Long, Integer> requestCount;

        public DiffExtractor(Map<Long, Integer> requestCount, ShipResponse response) {
            this.response = response;
            this.requestCount = requestCount;
        }

        @Override
        public ShipResponse extractData(ResultSet rs) throws SQLException, DataAccessException {
            while (rs.next()) {
                long articleId = rs.getLong(1);
                int diff = requestCount.get(articleId) - rs.getInt(2);

                if (diff > 0) {
                    setArticleDiff(articleId, diff);
                }
            }
            return response;
        }

        private void setArticleDiff(Long articleId, Integer diff) {
            if (this.response.getNoEnoughGoods() == null) {
                this.response.setNoEnoughGoods(new HashMap<Long, Integer>());
            }
            this.response.getNoEnoughGoods().put(articleId, diff);
        }
    }

    private static class ShipStatCollector implements ResultSetExtractor<Collection<Long>> {

        private ShipResponse response;

        public ShipStatCollector(ShipResponse response) {
            this.response = response;
        }

        @Override
        public Collection<Long> extractData(ResultSet rs) throws SQLException, DataAccessException {
            List<Long> goodIds = new ArrayList<>(rs.getFetchSize());

            while (rs.next()) {
                incGoodCount(/* article id */ rs.getLong(2), /* timestamp */ rs.getTimestamp(3).getTime());
                goodIds.add(/* good id*/ rs.getLong(1));
            }

            return goodIds;
        }

        private void incGoodCount(Long articleId, Long time) {
            if (this.response.getGoods() == null) {
                this.response.setGoods(new HashMap<Long, Map<Long, Integer>>());
            }
            if (this.response.getGoods().get(articleId) == null) {
                this.response.getGoods().put(articleId, new HashMap<Long, Integer>());
            }

            Integer currCount = this.response.getGoods().get(articleId).get(time);
            this.response.getGoods().get(articleId).put(time, currCount == null ? 1 : ++currCount);
        }
    }

}
