# vcc-assignment-restaurant-service

Standalone Java Spring Boot restaurant service with an H2 in-memory database.

APIs:
- `POST /api/restaurants`
- `GET /api/restaurants`
- `GET /api/restaurants/{id}`
- `PUT /api/restaurants/{id}`
- `DELETE /api/restaurants/{id}`
- `POST /api/restaurants/{restaurantId}/orders`
- `GET /api/restaurants/{restaurantId}/orders`
- `GET /api/restaurants/{restaurantId}/orders/{orderId}`
- `PUT /api/restaurants/{restaurantId}/orders/{orderId}`

The restaurant-level order endpoint forwards order data to the order service.

Run:

```bash
mvn spring-boot:run
```

Test:

```bash
mvn -Dmaven.repo.local=.m2 test
```

## AWS ECS Deployment

This service is ready to run on AWS ECS Fargate.

Included deployment assets:
- `Dockerfile` for container image builds
- `.github/workflows/deploy-ecs.yml` for GitHub Actions based ECR + ECS deployment
- `ecs/task-definition.json` as the ECS task definition template

Recommended AWS setup:
- ECR repository named `restaurant-service`
- ECS cluster named `applications`
- ECS service named `restaurant-service`
- Application Load Balancer health check path: `/actuator/health/readiness`
- CloudWatch log group: `/ecs/restaurant-service`
- `ORDER_SERVICE_BASE_URL` set to the internal DNS or Service Connect endpoint for the order service

Required GitHub secrets:
- `AWS_REGION`
- `AWS_ROLE_ARN`

Notes:
- The app still uses H2 in-memory storage by default, so data resets when the task restarts.
- For production persistence, switch `SPRING_DATASOURCE_*` environment variables to an RDS database.
