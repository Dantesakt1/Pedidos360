package com.pedidos360.backend.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.pedidos360.backend.config.KafkaConfig;
import com.pedidos360.backend.dto.InventarioKafkaEvent;
import com.pedidos360.backend.dto.PedidoKafkaEvent;

@Service
public class KafkaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarEventoPedido(PedidoKafkaEvent evento) {
        String key = evento.getPedidoId() != null ? String.valueOf(evento.getPedidoId()) : evento.getEventId();
        kafkaTemplate.send(KafkaConfig.TOPIC_PEDIDOS_EVENTOS, key, evento)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[KAFKA PRODUCER] -> Tópico: {} | Partición: {} | Offset: {} | Key: {} | Evento: {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                key,
                                evento.getTipoEvento());
                    } else {
                        log.error("[KAFKA PRODUCER ERROR] Falló envío de evento pedido {}: {}", key, ex.getMessage());
                    }
                });
    }

    public void publicarEventoInventario(InventarioKafkaEvent evento) {
        String key = evento.getProductoId() != null ? String.valueOf(evento.getProductoId()) : evento.getEventId();
        kafkaTemplate.send(KafkaConfig.TOPIC_INVENTARIO_EVENTOS, key, evento)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[KAFKA PRODUCER] -> Tópico: {} | Partición: {} | Offset: {} | Key: {} | Evento: {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                key,
                                evento.getTipoEvento());
                    } else {
                        log.error("[KAFKA PRODUCER ERROR] Falló envío de evento inventario {}: {}", key, ex.getMessage());
                    }
                });
    }
}