package com.pww.jmeter.amqp;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * JMeter Java Request sampler that publishes AMQP messages (AMQP 0-9-1).
 *
 * Add this jar to JMETER_HOME/lib/ext, then in JMeter:
 * Thread Group -> Sampler -> Java Request
 * and choose this class.
 */
public class AmqpPublishSampler extends AbstractJavaSamplerClient {

    private static final String PARAM_URI = "amqp_uri";
    private static final String PARAM_USERNAME = "username";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_EXCHANGE = "exchange";
    private static final String PARAM_ROUTING_KEY = "routing_key";
    private static final String PARAM_MESSAGE_BODY = "message_body";
    private static final String PARAM_MESSAGE_SIZE_BYTES = "message_size_bytes";
    private static final String PARAM_CONTENT_TYPE = "content_type";
    private static final String PARAM_PERSISTENT = "persistent";
    private static final String PARAM_TIMEOUT_MS = "connect_timeout_ms";
    private static final Logger log = LoggingManager.getLoggerForClass();

    private Connection connection;
    private Channel channel;

    @Override
    public Arguments getDefaultParameters() {
        Arguments args = new Arguments();
        args.addArgument(PARAM_URI, "amqp://localhost:5672");
        args.addArgument(PARAM_USERNAME, "");
        args.addArgument(PARAM_PASSWORD, "");
        args.addArgument(PARAM_EXCHANGE, "");
        args.addArgument(PARAM_ROUTING_KEY, "test.key");
        args.addArgument(PARAM_MESSAGE_BODY, "");
        args.addArgument(PARAM_MESSAGE_SIZE_BYTES, "256");
        args.addArgument(PARAM_CONTENT_TYPE, "application/json");
        args.addArgument(PARAM_PERSISTENT, "false");
        args.addArgument(PARAM_TIMEOUT_MS, "5000");
        return args;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        String uri = context.getParameter(PARAM_URI);
        String username = context.getParameter(PARAM_USERNAME);
        String password = context.getParameter(PARAM_PASSWORD);
        int timeoutMs = parseInt(context.getParameter(PARAM_TIMEOUT_MS), 5000);

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(uri);

            if (username != null && !username.isBlank()) {
                factory.setUsername(username);
            }
            if (password != null && !password.isBlank()) {
                factory.setPassword(password);
            }

            factory.setConnectionTimeout(timeoutMs);

            this.connection = factory.newConnection("jmeter-amqp-sampler");
            this.channel = connection.createChannel();
        } catch (Exception e) {
            log.error("AMQP setupTest failed: " + e.getMessage(), e);
            this.connection = null;
            this.channel = null;
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.setSampleLabel("AMQP Publish");

        String exchange = context.getParameter(PARAM_EXCHANGE);
        String routingKey = context.getParameter(PARAM_ROUTING_KEY);
        String messageBody = context.getParameter(PARAM_MESSAGE_BODY);
        int messageSize = parseInt(context.getParameter(PARAM_MESSAGE_SIZE_BYTES), 256);
        String contentType = context.getParameter(PARAM_CONTENT_TYPE);
        boolean persistent = parseBoolean(context.getParameter(PARAM_PERSISTENT), false);

        // Use custom message body if provided, otherwise generate random bytes
        byte[] payload;
        if (messageBody != null && !messageBody.isEmpty()) {
            payload = messageBody.getBytes(StandardCharsets.UTF_8);
        } else {
            payload = buildPayload(messageSize);
        }

        // Default content type based on payload type
        if (contentType == null || contentType.isEmpty()) {
            contentType = (messageBody != null && !messageBody.isEmpty())
                ? "application/json"
                : "application/octet-stream";
        }

        String correlationId = UUID.randomUUID().toString();

        try {
            if (channel == null || !channel.isOpen()) {
                throw new IllegalStateException("AMQP channel is not open (setupTest likely failed).");
            }

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .correlationId(correlationId)
                    .contentType(contentType)
                    .deliveryMode(persistent ? 2 : 1)
                    .build();

            result.sampleStart();

            channel.basicPublish(
                    exchange == null ? "" : exchange,
                    routingKey,
                    props,
                    payload
            );

            result.sampleEnd();

            result.setSuccessful(true);
            result.setResponseCode("200");
            result.setResponseMessage("Published " + payload.length + " bytes, correlationId=" + correlationId);
            result.setResponseData(("correlationId=" + correlationId).getBytes(StandardCharsets.UTF_8));
            result.setDataType(SampleResult.TEXT);
        } catch (Exception e) {
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseCode("500");
            result.setResponseMessage("Publish failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            log.error("AMQP publish failed: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            if (channel != null && channel.isOpen()) channel.close();
        } catch (Exception ignored) { }
        try {
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception ignored) { }
    }

    private static int parseInt(String s, int defaultVal) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return defaultVal; }
    }

    private static boolean parseBoolean(String s, boolean defaultVal) {
        if (s == null) return defaultVal;
        String v = s.trim().toLowerCase();
        if (v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y")) return true;
        if (v.equals("false") || v.equals("0") || v.equals("no") || v.equals("n")) return false;
        return defaultVal;
    }

    private static byte[] buildPayload(int size) {
        if (size <= 0) size = 1;
        byte[] buf = new byte[size];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) (i % 251);
        }
        return buf;
    }
}