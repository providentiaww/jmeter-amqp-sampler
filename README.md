# JMeter AMQP Sampler

A JMeter Java Request sampler plugin for publishing messages to AMQP 0-9-1 brokers (RabbitMQ).

## Overview

This plugin provides a high-performance AMQP publisher for JMeter load testing. It allows you to:

- Publish messages to RabbitMQ exchanges
- Configure message size, persistence, and routing
- Measure publish throughput and latency

## Installation

1. Build the plugin:
   ```bash
   mvn clean package
   ```

2. Copy the shaded JAR to JMeter:
   ```bash
   cp target/jmeter-amqp-sampler-0.1.0-SNAPSHOT.jar $JMETER_HOME/lib/ext/
   ```

3. Restart JMeter

## Usage

1. In JMeter, add a **Thread Group**
2. Add a **Java Request** sampler (Add → Sampler → Java Request)
3. Select `com.pww.jmeter.amqp.AmqpPublishSampler` as the classname
4. Configure the parameters:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `amqp_uri` | `amqp://localhost:5672` | AMQP connection URI |
| `username` | (empty) | RabbitMQ username (optional if in URI) |
| `password` | (empty) | RabbitMQ password (optional if in URI) |
| `exchange` | (empty) | Exchange name (empty = default exchange) |
| `routing_key` | `test.key` | Routing key for the message |
| `message_size_bytes` | `256` | Size of the message payload in bytes |
| `persistent` | `false` | Whether to mark messages as persistent |
| `connect_timeout_ms` | `5000` | Connection timeout in milliseconds |

## Example Configuration

For testing against a local RabbitMQ instance:

```
amqp_uri: amqp://localhost:5672
username: guest
password: guest
exchange: my-exchange
routing_key: test.routing.key
message_size_bytes: 1024
persistent: false
```

## Building

Requirements:
- Java 17+
- Maven 3.6+

```bash
# Build the plugin
mvn clean package

# The shaded JAR will be at:
# target/jmeter-amqp-sampler-0.1.0-SNAPSHOT.jar
```

## Architecture

The sampler uses the RabbitMQ Java client (AMQP 0-9-1 protocol):

- **setupTest()**: Creates a single connection and channel per thread
- **runTest()**: Publishes a message and records timing
- **teardownTest()**: Closes the channel and connection

This design ensures efficient connection reuse during load tests.

## Compatibility

- JMeter 5.6.3+
- RabbitMQ 3.x (AMQP 0-9-1)
- Java 17+

## License

Apache License 2.0 - See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Related Projects

- [Apache JMeter](https://jmeter.apache.org/)
- [RabbitMQ Java Client](https://www.rabbitmq.com/java-client.html)
- [Fulcrum](https://github.com/providentiaww/fulcrum) - Message-driven observability platform
