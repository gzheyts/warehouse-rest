package ru.gzheyts.warehouse.rest.service;

import ru.gzheyts.warehouse.rest.model.GoodPayload;
import ru.gzheyts.warehouse.rest.model.LoadResponse;
import ru.gzheyts.warehouse.rest.model.ShipResponse;
import ru.gzheyts.warehouse.rest.model.StatResponse;

/**
 * @author gzheyts
 */
public interface GoodService {


    LoadResponse loadGoods(final GoodPayload payload);

    ShipResponse shipGoods(final GoodPayload payload) throws NoSuchArticleException;

    StatResponse queryStatistics(final Long articleId, final Long timestamp);
}
