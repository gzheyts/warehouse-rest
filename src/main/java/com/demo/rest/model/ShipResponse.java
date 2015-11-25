package com.demo.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

/**
 * @author gzheyts
 */
public class ShipResponse extends Response {

    public enum ShipStatus {
        SHIPPED("shipped"), NOT_SHIPPED("not shipped");

        private String value;
        private ShipStatus(String status) {
            this.value = status;
        }

        @JsonCreator
        public ShipStatus fromString(String text) {
            if (text != null) {
                for (ShipStatus s : ShipStatus.values()) {
                    if (text.equalsIgnoreCase(s.value)) {
                        return s;
                    }
                }
            }
            return null;
        }

        @JsonValue
        public String toValue() {
            return value;
        }
    }

    private ShipStatus status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<Long, Map<Long, Integer>> goods;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<Long, Integer> noEnoughGoods;


    public ShipResponse(Long timestamp) {
        super(timestamp);    }

    public ShipStatus getStatus() {
        return status;
    }

    public void setStatus(ShipStatus status) {
        this.status = status;
    }

    public Map<Long, Map<Long, Integer>> getGoods() {
        return goods;
    }

    public void setGoods(Map<Long, Map<Long, Integer>> goods) {
        this.goods = goods;
    }


    public Map<Long, Integer> getNoEnoughGoods() {
        return noEnoughGoods;
    }

    public void setNoEnoughGoods(Map<Long, Integer> noEnoughGoods) {
        this.noEnoughGoods = noEnoughGoods;
    }
}
