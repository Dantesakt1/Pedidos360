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
    private final ObjectMapper objectMapper;

    public PedidoEventoProducer(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicar(String routingKey, PedidoEvento evento) {
        try {
            // Convertimos el evento a JSON para mandarlo
            String mensajeJson = objectMapper.writeValueAsString(evento);
            // Publicamos al Exchange Directo
            rabbitTemplate.convertAndSend(RabbitMQConfig.DIRECT_EXCHANGE, routingKey, mensajeJson);
            System.out.println("Mensaje publicado a [" + routingKey + "] -> " + mensajeJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible serializar el evento", e);
        }
    }

    // Método que llamaremos desde PedidoService para disparar las 3 tareas
    public void publicarTareas(PedidoEvento evento) {
        publicar("notificaciones", evento);
        publicar("cocina", evento);
        publicar("documentos", evento);
    }
}