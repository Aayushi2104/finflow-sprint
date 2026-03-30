package com.finflow.application_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "finflow.exchange";
    public static final String STATUS_QUEUE = "application.status.queue";
    public static final String STATUS_ROUTING_KEY = "application.status.update";

    @Bean
    public TopicExchange applicationExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue applicationStatusQueue() {
        return new Queue(STATUS_QUEUE);
    }

    @Bean
    public Binding applicationStatusBinding() {
        return BindingBuilder.bind(applicationStatusQueue())
                .to(applicationExchange())
                .with(STATUS_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
