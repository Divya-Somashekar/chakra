package com.chakra.service;

import com.chakra.domain.Restaurant;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RestaurantService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Add a restaurant
    public void addRestaurant(Restaurant restaurant) {
        redisTemplate.opsForGeo().add(
                "restaurants",
                new RedisGeoCommands.GeoLocation<>(
                        restaurant.getName(),
                        new org.springframework.data.geo.Point(
                                restaurant.getLongitude(),
                                restaurant.getLatitude()
                        )
                )
        );
    }

    // Find nearby restaurants within radiusKm km
    public List<String> findNearby(double lat, double lon, double radiusKm) {
        return redisTemplate.opsForGeo()
                .radius(
                        "restaurants",
                        new org.springframework.data.geo.Circle(
                                new org.springframework.data.geo.Point(lon, lat),
                                new org.springframework.data.geo.Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS)
                        )
                )
                .getContent()
                .stream()
                .map(r -> r.getContent().getName().toString())
                .collect(Collectors.toList());
    }
}