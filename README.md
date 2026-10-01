# chakra

A small Spring Boot REST service for geospatial restaurant lookup. Restaurants are
stored as points in a Redis [GEO](https://redis.io/docs/latest/develop/data-types/geospatial/)
set, so "what's near me?" is answered by Redis rather than in application code.

## Requirements

- **Java 21** (the Gradle toolchain will download it if it is missing)
- **Docker** — Redis runs in a container, started automatically by the app

## Quick start

```bash
./gradlew bootRun
```

That's the whole workflow. The `spring-boot-docker-compose` dependency reads
`compose.yaml`, starts the Redis container, waits for its healthcheck, and wires the
connection details into the app. The service listens on **http://localhost:8080**.

To run on a different port:

```bash
./gradlew bootRun --args='--server.port=8081'
```

## API

### Add a restaurant

```
POST /restaurant/add
```

```bash
curl -X POST http://localhost:8080/restaurant/add \
  -H 'Content-Type: application/json' \
  -d '{"name":"Dosa Palace","latitude":12.9716,"longitude":77.5946}'
```

Returns a plain-text confirmation:

```
Restaurant added: Dosa Palace
```

### Find nearby restaurants

```
GET /restaurant/nearby?lat={lat}&lon={lon}&radiusKm={km}
```

All three parameters are required; omitting any of them returns `400`.

```bash
curl "http://localhost:8080/restaurant/nearby?lat=12.9716&lon=77.5946&radiusKm=10"
```

Returns a JSON array of names:

```json
["Dosa Palace","Biryani House"]
```

### Try it end to end

```bash
for r in '{"name":"Dosa Palace","latitude":12.9716,"longitude":77.5946}' \
         '{"name":"Biryani House","latitude":12.9780,"longitude":77.6400}' \
         '{"name":"Mumbai Cafe","latitude":19.0760,"longitude":72.8777}'; do
  curl -s -X POST http://localhost:8080/restaurant/add \
    -H 'Content-Type: application/json' -d "$r"; echo
done

curl -s "http://localhost:8080/restaurant/nearby?lat=12.9716&lon=77.5946&radiusKm=1"     # ["Dosa Palace"]
curl -s "http://localhost:8080/restaurant/nearby?lat=12.9716&lon=77.5946&radiusKm=10"    # + Biryani House
curl -s "http://localhost:8080/restaurant/nearby?lat=12.9716&lon=77.5946&radiusKm=2000"  # + Mumbai Cafe
```

## Redis

`compose.yaml` defines a single `redis:8-alpine` service on port 6379 with a named
volume, so data survives a restart.

The app manages the container lifecycle by default: it starts on `bootRun` and **stops
when the app stops**. To leave Redis running between runs, add to `application.yml`:

```yaml
spring:
  docker:
    compose:
      lifecycle-management: start-only
```

You can also drive it by hand:

```bash
docker compose up -d     # start
docker compose down      # stop
docker compose down -v   # stop and delete the data volume
```

### Pointing at an external Redis

`application.yml` carries the connection settings:

```yaml
spring:
  data:
    redis:
      host: "localhost"
      port: 6379
```

Note that while `spring-boot-docker-compose` is on the classpath it supplies the
connection details and these properties are ignored. To target an external or
production Redis, remove that `developmentOnly` dependency from `build.gradle` (or
delete `compose.yaml`) and these values take effect.

## Tests

```bash
./gradlew test
```

The suite currently contains a single `contextLoads` smoke test. It does not need
Redis — the connection is created lazily, so nothing dials out during startup.

## Layout

```
src/main/java/com/chakra/
├── ChakraApplication.java              entry point
├── config/RedisConfig.java             RedisTemplate with String serializers
├── controller/RestaurantController.java  the two endpoints
├── domain/Restaurant.java             name + latitude + longitude (Lombok @Data)
└── service/RestaurantService.java     Redis GEO add / radius query
```

All restaurants live under the single Redis key `restaurants`.

## Known limitations

This is a minimal service. Worth knowing before building on it:

- **No input validation.** Latitude and longitude are not range-checked, and a
  malformed body yields a `400` from Jackson rather than a helpful message.
- **Names are identities.** A restaurant's name is its Redis member key, so adding the
  same name twice moves the existing point instead of creating a second entry.
- **No read-back, update, or delete.** You can add and search, nothing else.
- **`nearby` returns names only** — no coordinates and no distances, though Redis
  computes them.
- **Result order is not guaranteed.** The query does not ask Redis to sort, and Redis
  returns matches unsorted without an explicit `ASC`/`DESC`. Results happen to come
  back nearest-first in practice, but do not rely on it; pass a sort if you need it.
- **If Redis is unreachable the endpoints return a bare `500`.** There is no
  connection-failure handling or health endpoint.
