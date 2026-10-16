package com.pedidos360.backend.service;

import java.io.IOException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend.config.RabbitMQConfig;
import com.pedidos360.backend.model.PedidoEvento;
import com.rabbitmq.client.Channel;

@Service
public class DominioConsumers {

    private final ObjectMapper objectMapper;

    public DominioConsumers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Consumidor de Notificaciones
    @RabbitListener(queues = RabbitMQConfig.Q_NOTIFICACIONES)
    public void procesarNotificacion(String mensajeJson, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            PedidoEvento evento = objectMapper.readValue(mensajeJson, PedidoEvento.class);
            
            // Forzamos el fallo si la flag viene en true (Ideal para demostrar en tu video EP4)
            if (evento.falloNotificaciones()) {
                System.err.println("[NOTIFICACIONES] ❌ Fallo simulado para pedido ID: " + evento.pedidoId());
                // NACK sin requeue: RabbitMQ lo manda a pedidos360.notificaciones.queue.dlq
                channel.basicNack(tag, false, false);
                return;
            }

            System.out.println("[NOTIFICACIONES] ✅ Procesado con éxito. Correo enviado para pedido ID: " + evento.pedidoId());
            // ACK: Confirmamos que se procesó bien y se borra de la cola
            channel.basicAck(tag, false);

        } catch (Exception e) {
            System.err.println("[NOTIFICACIONES] Error inesperado: " + e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }

    // Consumidor de Cocina
    @RabbitListener(queues = RabbitMQConfig.Q_COCINA)
    public void procesarCocina(String mensajeJson, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            PedidoEvento evento = objectMapper.readValue(mensajeJson, PedidoEvento.class);
            
            if (evento.falloCocina()) {
                System.err.println("[COCINA] ❌ Fallo simulado para pedido ID: " + evento.pedidoId());
                channel.basicNack(tag, false, false);
                return;
            }

            System.out.println("[COCINA] ✅ Ticket de cocina generado para pedido ID: " + evento.pedidoId());
            channel.basicAck(tag, false);

        } catch (Exception e) {
            channel.basicNack(tag, false, false);
        }
    }

    // Consumidor de Documentos
    @RabbitListener(queues = RabbitMQConfig.Q_DOCUMENTOS)
    public void procesarDocumentos(String mensajeJson, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            PedidoEvento evento = objectMapper.readValue(mensajeJson, PedidoEvento.class);
            
            if (evento.falloDocumentos()) {
                System.err.println("[DOCUMENTOS] ❌ Fallo simulado para pedido ID: " + evento.pedidoId());
                channel.basicNack(tag, false, false);
                return;
            }

            System.out.println("[DOCUMENTOS] ✅ Boleta emitida para pedido ID: " + evento.pedidoId());
            channel.basicAck(tag, false);

        } catch (Exception e) {
            channel.basicNack(tag, false, false);
        }
    }
}