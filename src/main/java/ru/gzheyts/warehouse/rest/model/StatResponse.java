package ru.gzheyts.warehouse.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author gzheyts
 */
public class StatResponse extends Response {

    @JsonProperty("in_stock_count")
    private long count;

    @JsonProperty("average_in_stock_time")
    private long time;

    public StatResponse() {
        super(null);
    }

    public StatResponse(long count, long time) {
        this();
        this.count = count;
        this.time = time;
    }


    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
