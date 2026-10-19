package com.pedidos360.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend.config.RabbitMQConfig;
import com.pedidos360.backend.model.PedidoEvento;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PedidoEventoProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PedidoEventoProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(String routingKey, PedidoEvento evento) {
        try {
            String mensajeJson = objectMapper.writeValueAsString(evento);
            rabbitTemplate.convertAndSend(RabbitMQConfig.DIRECT_EXCHANGE, routingKey, mensajeJson);
            System.out.println("Mensaje publicado a [" + routingKey + "] -> " + mensajeJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible serializar el evento", e);
        }
    }

    public void publicarTareas(PedidoEvento evento) {
        publicar("notificaciones", evento);
        publicar("cocina", evento);
        publicar("documentos", evento);
    }
}