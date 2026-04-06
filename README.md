# vcc-assignment-restaurant-service

Standalone Java Spring Boot restaurant service with an H2 in-memory database.

APIs:
- `POST /api/restaurants`
- `GET /api/restaurants`
- `GET /api/restaurants/{id}`
- `PUT /api/restaurants/{id}`
- `DELETE /api/restaurants/{id}`
- `POST /api/restaurants/{restaurantId}/orders`

The restaurant-level order endpoint forwards order data to the order service.

Run:

```bash
mvn spring-boot:run
```

Test:

```bash
mvn -Dmaven.repo.local=.m2 test
```
