package com.pedidos360.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DIRECT_EXCHANGE = "pedidos360.direct.exchange";
    public static final String TOPIC_EXCHANGE = "pedidos360.topic.exchange";
    public static final String DLX_EXCHANGE = "pedidos360.dlx";

    public static final String Q_NOTIFICACIONES = "pedidos360.notificaciones.queue";
    public static final String Q_COCINA = "pedidos360.cocina.queue";
    public static final String Q_DOCUMENTOS = "pedidos360.documentos.queue";

    // Declarar los Exchanges
    @Bean DirectExchange directExchange() { return new DirectExchange(DIRECT_EXCHANGE, true, false); }
    @Bean TopicExchange topicExchange() { return new TopicExchange(TOPIC_EXCHANGE, true, false); }
    @Bean DirectExchange dlxExchange() { return new DirectExchange(DLX_EXCHANGE, true, false); }

    // Método auxiliar para crear colas principales vinculadas automáticamente a su DLQ
    private Queue crearColaPrincipal(String nombreCola) {
        return QueueBuilder.durable(nombreCola)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", nombreCola + ".dlq")
                .build();
    }

    // Declarar las Colas Principales
    @Bean Queue notificacionesQueue() { return crearColaPrincipal(Q_NOTIFICACIONES); }
    @Bean Queue cocinaQueue() { return crearColaPrincipal(Q_COCINA); }
    @Bean Queue documentosQueue() { return crearColaPrincipal(Q_DOCUMENTOS); }

    // Declarar las Colas DLQ (Dead Letter Queues)
    @Bean Queue notificacionesDlq() { return QueueBuilder.durable(Q_NOTIFICACIONES + ".dlq").build(); }
    @Bean Queue cocinaDlq() { return QueueBuilder.durable(Q_COCINA + ".dlq").build(); }
    @Bean Queue documentosDlq() { return QueueBuilder.durable(Q_DOCUMENTOS + ".dlq").build(); }

    // Bindings: Conectar el Direct Exchange con las Colas Principales usando Routing Keys
    @Bean Binding bindNotificaciones(DirectExchange directExchange, Queue notificacionesQueue) {
        return BindingBuilder.bind(notificacionesQueue).to(directExchange).with("notificaciones");
    }
    @Bean Binding bindCocina(DirectExchange directExchange, Queue cocinaQueue) {
        return BindingBuilder.bind(cocinaQueue).to(directExchange).with("cocina");
    }
    @Bean Binding bindDocumentos(DirectExchange directExchange, Queue documentosQueue) {
        return BindingBuilder.bind(documentosQueue).to(directExchange).with("documentos");
    }

    // Conectar el DLX con las Colas DLQ
    @Bean Binding bindNotificacionesDlq(DirectExchange dlxExchange, Queue notificacionesDlq) {
        return BindingBuilder.bind(notificacionesDlq).to(dlxExchange).with(Q_NOTIFICACIONES + ".dlq");
    }
    @Bean Binding bindCocinaDlq(DirectExchange dlxExchange, Queue cocinaDlq) {
        return BindingBuilder.bind(cocinaDlq).to(dlxExchange).with(Q_COCINA + ".dlq");
    }
    @Bean Binding bindDocumentosDlq(DirectExchange dlxExchange, Queue documentosDlq) {
        return BindingBuilder.bind(documentosDlq).to(dlxExchange).with(Q_DOCUMENTOS + ".dlq");
    }

    // 6. Binding Topic: Solo para demostrar el uso del Topic Exchange con patrón "pedido.*"
    @Bean Binding bindTopicNotificaciones(TopicExchange topicExchange, Queue notificacionesQueue) {
        return BindingBuilder.bind(notificacionesQueue).to(topicExchange).with("pedido.*");
    }
}