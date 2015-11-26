package ru.gzheyts.warehouse.rest.service;

/**
 * @author gzheyts
 */
public class NoSuchArticleException extends Exception {
    public NoSuchArticleException() {
        super("No such article");
    }
}
