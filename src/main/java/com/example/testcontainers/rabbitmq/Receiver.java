package com.example.testcontainers.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

// @Component
public class Receiver {

    /*private CountDownLatch latch = new CountDownLatch(1);

    @RabbitListener(queues = RabbitMQConfig.queueName)
    public void receive(String payload, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        System.out.println(payload);
        channel.basicAck(tag, false);

        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }*/

}
