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

Metrics:
- `GET /actuator/health`
- `GET /actuator/prometheus`

The restaurant-level order endpoint forwards order data to the order service.

Run:

```bash
mvn spring-boot:run
```

Test:

```bash
mvn -Dmaven.repo.local=.m2 test
```

## Local Monitoring

Prometheus and Grafana are configured for local use in:
- `docker-compose.monitoring.yml`
- `monitoring/prometheus/prometheus.yml`
- `monitoring/grafana/...`

When Docker is available and both services are running locally:

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

Then open:
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

Grafana login:
- username: `admin`
- password: `admin`

## Performance Testing

Performance load, stress, chaos, and Grafana dashboard assets are available in:
- `performance/k6/incremental-load.js`
- `performance/scripts/run-k6-incremental.sh`
- `performance/scripts/run-stress-ng-peak.sh`
- `performance/scripts/run-chaos-mesh.sh`
- `performance/grafana/k6-load-testing-dashboard.json`

See `performance/README.md` for usage.

## AWS ECS Deployment

This service is ready to run on AWS ECS Fargate through CloudFormation.

Included deployment assets:
- `Dockerfile` for container image builds
- `.github/workflows/deploy-ecs.yml` for GitHub Actions based ECR + CloudFormation deployment
- `aws/cloudformation/ecs-fargate-service.yml` to provision the ECS service, ALB target group and listener rule, CloudWatch log group, autoscaling policies, alarms, and dashboard
- `ecs/task-definition.json` as a lower-level task definition reference

CloudWatch coverage:
- Log group: `/ecs/restaurant-service`
- Dashboard: `restaurant-service-operations`
- Alarms for high CPU, high memory, unhealthy targets, and target 5xx responses

Required GitHub secrets:
- `AWS_REGION`
- `AWS_ROLE_ARN`
- `AWS_VPC_ID`
- `AWS_SUBNET_IDS`
- `AWS_SECURITY_GROUP_IDS`
- `AWS_ALB_LISTENER_ARN`
- `AWS_ALB_FULL_NAME`
- `AWS_ECS_EXECUTION_ROLE_ARN`
- `AWS_RESTAURANT_TASK_ROLE_ARN`
- `AWS_ALARM_TOPIC_ARN`
- `ORDER_SERVICE_BASE_URL`

The GitHub Actions workflow will:
1. build and push the Docker image to ECR
2. deploy the CloudFormation stack
3. update ECS with the new image
4. keep logs, alarms, and dashboards in CloudWatch

Manual stack deployment example:

```bash
aws cloudformation deploy \
  --stack-name restaurant-service \
  --template-file aws/cloudformation/ecs-fargate-service.yml \
  --parameter-overrides \
    ImageUri=<account-id>.dkr.ecr.<region>.amazonaws.com/restaurant-service:<tag> \
    VpcId=<vpc-id> \
    SubnetIds=<subnet-1>,<subnet-2> \
    SecurityGroupIds=<sg-id> \
    ListenerArn=<listener-arn> \
    LoadBalancerFullName=app/<alb-name>/<alb-id> \
    TaskExecutionRoleArn=<execution-role-arn> \
    TaskRoleArn=<task-role-arn> \
    OrderServiceBaseUrl=http://order-service.internal:8082
```

Notes:
- CloudWatch helps monitor service health and deployment state, but it does not preserve application data.
- The app still uses H2 in-memory storage by default, so data resets when the task restarts.
- For persistent restaurant or order state in AWS, switch `SPRING_DATASOURCE_*` environment variables to an RDS database.
