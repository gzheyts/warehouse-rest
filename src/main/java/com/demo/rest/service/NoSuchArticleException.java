package com.demo.rest.service;

/**
 * @author gzheyts
 */
public class NoSuchArticleException extends Exception {
    public NoSuchArticleException() {
        super("No such article");
    }
}
