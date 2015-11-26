package ru.gzheyts.warehouse.rest.controller;

import ru.gzheyts.warehouse.rest.model.GoodPayload;
import ru.gzheyts.warehouse.rest.model.LoadResponse;
import ru.gzheyts.warehouse.rest.model.ShipResponse;
import ru.gzheyts.warehouse.rest.model.StatResponse;
import ru.gzheyts.warehouse.rest.service.GoodService;
import ru.gzheyts.warehouse.rest.service.NoSuchArticleException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/warehouse")
public class WarehouseController {


    static final Logger logger = Logger.getLogger(WarehouseController.class);

    private final GoodService goodService;

    @Autowired
    public WarehouseController(GoodService goodService) {
        this.goodService = goodService;
    }


    @RequestMapping(value = "/load", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpEntity<LoadResponse> load(@RequestBody final GoodPayload payload) {

        return new ResponseEntity<>(goodService.loadGoods(payload), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/ship", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpEntity<ShipResponse> ship(@RequestBody final GoodPayload payload) throws NoSuchArticleException{

        return new ResponseEntity<>(goodService.shipGoods(payload), HttpStatus.OK);
    }

    @RequestMapping(value = "/stat", method = RequestMethod.GET)
    public HttpEntity<StatResponse> stat(@RequestParam(value = "article") final Long article,
                                         @RequestParam(value = "timestamp") final Long timestamp) {

        return new ResponseEntity<>(goodService.queryStatistics(article, timestamp), HttpStatus.OK);
    }


    @ExceptionHandler(Exception.class)
    public HttpEntity<String> handleException(final Exception ex) {
        logger.error(ex.getMessage(), ex);
        if (ex instanceof NoSuchArticleException) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
