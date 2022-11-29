package com.example.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import com.example.testcontainers.rabbitmq.RabbitMQConfig;
import com.example.testcontainers.rabbitmq.Receiver;
import com.rabbitmq.client.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TestcontainersApplicationTests {

    //static final String topicExchangeName = "spring-boot-exchange";
    //static final String queueName = "spring-boot";

    CountDownLatch latch = new CountDownLatch(1);
    List<String> messages = new ArrayList<>();

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));

    @Container
    private static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq").withTag("3-management-alpine"));/*.withQueue(RabbitMQConfig.queueName, false, true, Collections.emptyMap())
            .withQueue(RabbitMQConfig.deadLetterQueueName, false, true, Collections.emptyMap())
            .withExchange(RabbitMQConfig.exchangeName, ExchangeTypes.DIRECT)
            .withExchange(RabbitMQConfig.deadLetterExchangeName, ExchangeTypes.FANOUT)
            .withBinding(RabbitMQConfig.exchangeName, RabbitMQConfig.queueName, Collections.emptyMap(), "message_routing_key", "queue");*/

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @BeforeAll
    static void setUp() {
//        kafkaContainer = new KafkaContainer(
//                DockerImageName.parse("confluentinc/cp-kafka"));
//        kafkaContainer.start();
//
//        String bootstrapServers = kafkaContainer.getBootstrapServers();
//        System.out.println("Bootstrap servers " + bootstrapServers);
//
//        System.setProperty("app.kafka.bootstrapAddress", bootstrapServers);
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);

        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    //@Test
    void contextLoads() {
    }

    @Test
    void rabbitmqAckTest() throws InterruptedException {
        rabbitTemplate.convertAndSend(RabbitMQConfig.exchangeName, "spring-boot-key", "Hello!");

        latch.await(5, TimeUnit.SECONDS);

        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", RabbitMQConfig.deadLetterExchangeName);
        args.put("x-dead-letter-routing-key", "deadLetter");

        var queueMessageCount = rabbitTemplate.execute(channel -> {
            return channel.queueDeclare(RabbitMQConfig.queueName, true, false, false, args).getMessageCount();
        });

        var deadLetterQueueMessageCount = rabbitTemplate.execute(channel -> {
            return channel.queueDeclare(RabbitMQConfig.deadLetterQueueName, true, false, false, null).getMessageCount();
        });

        var queuedMessage = rabbitTemplate.receiveAndConvert(RabbitMQConfig.queueName);
        var deadLetterQueuedMessage = rabbitTemplate.receiveAndConvert(RabbitMQConfig.deadLetterQueueName);

        System.out.println(queueMessageCount);
        System.out.println(deadLetterQueueMessageCount);
        System.out.println(queuedMessage);
        System.out.println(deadLetterQueuedMessage);

        Assertions.assertEquals(0, deadLetterQueueMessageCount);
        //Assertions.assertTrue(isRabbitMQMessageReceived());
        //Assertions.assertTrue(queueMessageCount == 1 && queuedMessage == "Hello!");

        //assertThat(latch.await(10, TimeUnit.SECONDS));

        //await().atMost(5, TimeUnit.SECONDS).until(isRabbitMQMessageReceived(), is("OK"));
    }

    @Test
    void rabbitmqNackTest() throws InterruptedException {
        rabbitTemplate.convertAndSend(RabbitMQConfig.exchangeName, "spring-boot-key", "Error");

        latch.await(5, TimeUnit.SECONDS);

        var deadLetterQueueMessageCount = rabbitTemplate.execute(channel -> {
            return channel.queueDeclare(RabbitMQConfig.deadLetterQueueName, true, false, false, null).getMessageCount();
        });

        var deadLetterQueuedMessage = rabbitTemplate.receiveAndConvert(RabbitMQConfig.deadLetterQueueName);

        System.out.println(deadLetterQueueMessageCount);
        System.out.println(deadLetterQueuedMessage);

        Assertions.assertEquals(1, deadLetterQueueMessageCount);
    }

    //@Test
    void kafkaTest() {
        kafkaTemplate.send("tests", "Hello, World!");
        kafkaTemplate.send("tests", "First Message");
        kafkaTemplate.send("tests", "Second Message");
        kafkaTemplate.send("tests", "Third Message");

        //Assertions.assertEquals(4, KafkaListener.messages.size());
        await().atMost(5, TimeUnit.SECONDS).until(() -> messages.size(), is(4));
    }

    @RabbitListener(queues = RabbitMQConfig.queueName, ackMode = "MANUAL")
    void receive(String payload, @Header(AmqpHeaders.CHANNEL) Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        System.out.println("Received message : " + payload);

        if (payload.equals("Hello!")) {
            channel.basicAck(tag, false);
        } else {
            channel.basicNack(tag, false, false);
        }

        latch.countDown();
    }

    @KafkaListener(topics = "tests", groupId = "group_id")
    void listenGroupFoo(String message) throws IOException {
        System.out.println("Received message in group Test with topic test: " + message);

        messages.add(message);
    }

    private Callable<Boolean> isMessageConsumed() {
        return () -> kafkaTemplate.receive("tests", 1, 1).value().contains("Hello, World!");
    }
}
