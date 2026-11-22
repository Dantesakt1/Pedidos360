package com.pedidos360.backend.kafka;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.pedidos360.backend.config.KafkaConfig;
import com.pedidos360.backend.dto.InventarioKafkaEvent;
import com.pedidos360.backend.dto.PedidoKafkaEvent;

@Service
public class KafkaEventConsumers {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumers.class);
    private final List<String> historialEventos = new CopyOnWriteArrayList<>();

    private void registrarHistorial(String registro) {
        historialEventos.add(0, registro);
        if (historialEventos.size() > 50) {
            historialEventos.remove(historialEventos.size() - 1);
        }
    }

    public List<String> getHistorialEventos() {
        return historialEventos;
    }

    // CONSUMER GROUP 1: Auditoría de Pedidos
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PEDIDOS_EVENTOS,
            groupId = "auditoria-pedidos-group",
            concurrency = "3"
    )
    public void consumirAuditoriaPedidos(ConsumerRecord<String, PedidoKafkaEvent> record) {
        PedidoKafkaEvent evento = record.value();
        if (evento != null && evento.isSimularFallo()) {
            log.warn("[KAFKA AUDITORIA] Simulando fallo para evento {} -> iniciando reintentos hacia DLT...", evento.getEventId());
            throw new RuntimeException("Fallo simulado en procesamiento de auditoría para probar DLT");
        }
        String msg = String.format("[auditoria-pedidos-group] P:%d O:%d | Pedido #%s -> %s ($%.0f)",
                record.partition(), record.offset(),
                evento != null ? evento.getPedidoId() : "N/A",
                evento != null ? evento.getEstado() : "N/A",
                evento != null && evento.getTotal() != null ? evento.getTotal() : 0.0);
        log.info(msg);
        registrarHistorial(msg);
    }

    // CONSUMER GROUP 2: Analítica y Métricas (grupo independiente sobre el mismo tópico)
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PEDIDOS_EVENTOS,
            groupId = "analitica-ventas-group"
    )
    public void consumirAnaliticaVentas(ConsumerRecord<String, PedidoKafkaEvent> record) {
        PedidoKafkaEvent evento = record.value();
        String msg = String.format("[analitica-ventas-group] P:%d O:%d | Métrica registrada tipo=%s total=$%.0f",
                record.partition(), record.offset(),
                evento != null ? evento.getTipoEvento() : "N/A",
                evento != null && evento.getTotal() != null ? evento.getTotal() : 0.0);
        log.info(msg);
        registrarHistorial(msg);
    }

    // CONSUMER GROUP 3: Alertas de Stock e Inventario
    @KafkaListener(
            topics = KafkaConfig.TOPIC_INVENTARIO_EVENTOS,
            groupId = "alertas-inventario-group",
            concurrency = "3"
    )
    public void consumirInventario(ConsumerRecord<String, InventarioKafkaEvent> record) {
        InventarioKafkaEvent evento = record.value();
        if (evento != null && evento.isSimularFallo()) {
            log.warn("[KAFKA INVENTARIO] Simulando fallo para producto {} -> iniciando reintentos hacia DLT...", evento.getProductoId());
            throw new RuntimeException("Fallo simulado en inventario para probar DLT");
        }
        String alerta = (evento != null && evento.getStockActual() != null && evento.getStockActual() <= 5)
                ? " [ALERTA STOCK BAJO]"
                : "";
        String msg = String.format("[alertas-inventario-group] P:%d O:%d | %s (%s) Stock: %s -> %s%s",
                record.partition(), record.offset(),
                evento != null ? evento.getNombreProducto() : "N/A",
                evento != null ? evento.getTipoEvento() : "N/A",
                evento != null ? evento.getStockAnterior() : "-",
                evento != null ? evento.getStockActual() : "-",
                alerta);
        log.info(msg);
        registrarHistorial(msg);
    }

    // CONSUMIDOR DE DEAD LETTER TOPICS (DLT)
    @KafkaListener(
            topics = {KafkaConfig.TOPIC_PEDIDOS_DLT, KafkaConfig.TOPIC_INVENTARIO_DLT},
            groupId = "dlt-monitor-group"
    )
    public void consumirDeadLetterTopics(ConsumerRecord<String, Object> record) {
        String msg = String.format("[DLT MONITOR] Mensaje fallido capturado en %s (P:%d O:%d): %s",
                record.topic(), record.partition(), record.offset(), record.value());
        log.error(msg);
        registrarHistorial(msg);
    }
}