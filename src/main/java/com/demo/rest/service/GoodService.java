package com.demo.rest.service;

import com.demo.rest.model.GoodPayload;
import com.demo.rest.model.LoadResponse;
import com.demo.rest.model.ShipResponse;
import com.demo.rest.model.StatResponse;

/**
 * @author gzheyts
 */
public interface GoodService {


    LoadResponse loadGoods(final GoodPayload payload);

    ShipResponse shipGoods(final GoodPayload payload) throws NoSuchArticleException;

    StatResponse queryStatistics(final Long articleId, Long timestamp);
}
