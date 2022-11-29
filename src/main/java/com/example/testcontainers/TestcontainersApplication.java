package com.example.testcontainers;

import com.example.testcontainers.rabbitmq.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestcontainersApplication implements CommandLineRunner {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static void main(String[] args) {
        SpringApplication.run(TestcontainersApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        /*rabbitTemplate.convertAndSend(RabbitMQConfig.exchangeName, "message_routing_key", "Hellow, World!");

        var queueMessageCount = rabbitTemplate.execute(channel -> {
                return channel.queueDeclare(RabbitMQConfig.queueName,true,false,false,null).getMessageCount();
        });

        System.out.println(queueMessageCount);

        var queuedMessage = rabbitTemplate.receiveAndConvert(RabbitMQConfig.queueName);

        System.out.println(queuedMessage);*/
    }
}
