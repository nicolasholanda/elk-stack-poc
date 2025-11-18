# ELK Stack POC

A Spring Boot application demonstrating ELK (Elasticsearch, Logstash, Kibana) stack integration with comprehensive logging, monitoring, and visualization capabilities.

## Project Overview

This POC (Proof of Concept) showcases:
- Spring Boot REST API for managing Users and Orders
- Centralized logging with ELK Stack
- Custom Kibana dashboards for monitoring and analytics
- Load testing and stress testing capabilities
- Comprehensive integration tests

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.5.7
- **Logging**: Logback, SLF4J with HTTP interceptors
- **Monitoring Stack**: 
  - Elasticsearch (search and analytics)
  - Logstash (log processing and aggregation)
  - Kibana (visualization and exploration)
- **Testing**: JUnit 5, MockMvc, TestRestTemplate
- **Load Testing**: K6
- **Database**: H2 (in-memory for testing), PostgreSQL (production-ready migrations)

## Project Structure

```
elk-stack-poc/
├── src/
│   ├── main/
│   │   ├── java/com/github/nicolasholanda/elk_stack_poc/
│   │   │   ├── controller/          # REST endpoints
│   │   │   ├── model/               # JPA entities
│   │   │   ├── repository/          # Data access layer
│   │   │   ├── service/             # Business logic
│   │   │   ├── config/              # Spring configurations
│   │   │   └── ElkStackPocApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback-spring.xml   # Logging configuration
│   │       └── db/migration/        # Flyway migrations
│   └── test/
│       └── java/com/github/nicolasholanda/elk_stack_poc/
│           └── controller/          # Integration tests
├── kibana-provisioning/
│   └── dashboards/                  # Pre-built Kibana dashboards
├── docker-compose.yml               # ELK Stack services
├── logstash.conf                    # Logstash pipeline configuration
├── k6-load-test.js                  # Load test scenarios
├── k6-stress-test.js                # Stress test scenarios
└── pom.xml
```

## API Endpoints

### Users
- `POST /api/users` - Create a new user
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users` - Get all users

### Orders
- `POST /api/orders` - Create a new order
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders` - Get all orders

## Setup and Installation

### Prerequisites
- Java 21+
- Docker and Docker Compose
- Maven
- K6 (optional, for load testing)

### Running the Application

1. **Start ELK Stack**
```bash
docker-compose up -d
```

This will start:
- Elasticsearch on `http://localhost:9200`
- Kibana on `http://localhost:5601`
- Logstash on port 5000

2. **Build and run the application**
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

3. **Verify the application is running**
```bash
curl http://localhost:8080/actuator/health
```

## Logging and Monitoring

### Application Logs
- Logs are automatically sent to Logstash via HTTP
- Configure log levels in `application.properties`
- HTTP interceptors track request/response details

### Kibana Dashboards

Access Kibana at `http://localhost:5601` to view pre-built dashboards:

- **API Overview Dashboard** - Request volume, response times, endpoints usage
- **Performance Metrics Dashboard** - System metrics, request latency distribution
- **Error Analysis Dashboard** - Error rates, exception tracking, error patterns
- **Business KPIs Dashboard** - User and order analytics

### Log Fields
All logs include:
- `timestamp` - Request time
- `level` - Log level (INFO, ERROR, WARN)
- `logger` - Logger name
- `message` - Log message
- `http.method` - HTTP method
- `http.url` - Request URL
- `http.status_code` - Response status
- `http.response_time_ms` - Response time in milliseconds

## Testing

### Unit and Integration Tests

Run integration tests:
```bash
./mvnw test
```

Test classes:
- `UserControllerIT` - User endpoint integration tests
- `OrderControllerIT` - Order endpoint integration tests

Tests cover:
- Happy path scenarios (successful operations)
- Error scenarios (404 not found)
- Data validation
- Foreign key constraints

### Load Testing

Run K6 load tests:
```bash
k6 run k6-load-test.js
```

Run K6 stress tests:
```bash
k6 run k6-stress-test.js
```

## Database

### Schema
The application uses Flyway for database migrations:
- `V1__Create_users_table.sql` - Users table with email and phone
- `V2__Create_orders_table.sql` - Orders table with foreign key to users

### Development
For development, the application uses H2 in-memory database. Connection details are in `application.properties`.

## Configuration Files

### application.properties
- Server port and context path
- Database connection settings
- Logback configuration reference
- Actuator endpoints

### logback-spring.xml
- Console and HTTP appenders
- Log level configuration per package
- HTTP interceptor settings for sending logs to Logstash

### docker-compose.yml
- Elasticsearch container with proper heap settings
- Kibana with pre-provisioned dashboards
- Logstash with pipeline configuration

### logstash.conf
- HTTP input on port 5000
- JSON parsing and field mapping
- Elasticsearch output configuration

## Development

### Adding New Endpoints
1. Create a new controller in `controller/`
2. Create corresponding service in `service/`
3. Add repository if needed in `repository/`
4. Create integration tests in `test/`

### Adding New Dashboards
1. Create dashboard JSON in `kibana-provisioning/dashboards/`
2. Update docker-compose.yml to provision the dashboard
3. Restart containers

### Modifying Logs
- Update `logback-spring.xml` for appender configuration
- Add `@Slf4j` annotation to classes that need logging
- Use log levels appropriately (DEBUG, INFO, WARN, ERROR)

## Troubleshooting

### No logs appearing in Kibana
1. Verify Elasticsearch is running: `curl http://localhost:9200`
2. Check Logstash logs: `docker logs elk-stack-poc-logstash-1`
3. Verify application is sending logs: Check application logs

### Elasticsearch connection refused
1. Ensure Docker containers are running: `docker ps`
2. Check Elasticsearch health: `curl http://localhost:9200/_cluster/health`
3. Restart containers: `docker-compose restart`

### Integration tests failing
1. Ensure H2 database is properly configured
2. Check foreign key constraints for Order/User relationships
3. Run with `./mvnw test -X` for debug output

## Performance Considerations

- Log sampling can be configured in Logback for high-traffic scenarios
- Elasticsearch index rotation is recommended for long-running deployments
- Use Kibana Index Lifecycle Management (ILM) for log retention policies

## Contributing

When adding new features:
1. Add corresponding integration tests
2. Update logging to include relevant context
3. Consider monitoring and dashboarding implications
4. Document API changes in README

## License

This is a POC project for educational purposes.

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Elasticsearch Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Kibana Guide](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [K6 Load Testing](https://k6.io/docs/)

