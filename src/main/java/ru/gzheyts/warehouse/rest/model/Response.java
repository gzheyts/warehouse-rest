package ru.gzheyts.warehouse.rest.model;

import java.io.Serializable;

/**
 * @author gzheyts
 */
public class Response implements Serializable{
    private Long timestamp;

    public Response(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
