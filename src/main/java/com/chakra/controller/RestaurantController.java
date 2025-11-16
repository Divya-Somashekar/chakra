package com.chakra.controller;

import com.chakra.domain.Restaurant;
import com.chakra.service.RestaurantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/restaurant")
public class RestaurantController {

    private final RestaurantService service;

    public RestaurantController(RestaurantService service) {
        this.service = service;
    }

    @PostMapping("/add")
    public String addRestaurant(@RequestBody Restaurant restaurant) {
        service.addRestaurant(restaurant);
        return "Restaurant added: " + restaurant.getName();
    }

    @GetMapping("/nearby")
    public List<String> nearbyRestaurants(@RequestParam double lat,
                                          @RequestParam double lon,
                                          @RequestParam double radiusKm) {
        return service.findNearby(lat, lon, radiusKm);
    }
}
