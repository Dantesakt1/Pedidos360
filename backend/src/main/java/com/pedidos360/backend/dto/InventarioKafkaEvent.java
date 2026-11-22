package com.pedidos360.backend.dto;

import java.time.Instant;
import java.util.UUID;

public class InventarioKafkaEvent {
    private String eventId = UUID.randomUUID().toString();
    private String tipoEvento;
    private Long productoId;
    private String nombreProducto;
    private Integer stockAnterior;
    private Integer stockActual;
    private String timestamp = Instant.now().toString();
    private boolean simularFallo = false;

    public InventarioKafkaEvent() {}

    public InventarioKafkaEvent(String tipoEvento, Long productoId, String nombreProducto, Integer stockAnterior, Integer stockActual, boolean simularFallo) {
        this.eventId = UUID.randomUUID().toString();
        this.tipoEvento = tipoEvento;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.stockAnterior = stockAnterior;
        this.stockActual = stockActual;
        this.timestamp = Instant.now().toString();
        this.simularFallo = simularFallo;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public Integer getStockAnterior() { return stockAnterior; }
    public void setStockAnterior(Integer stockAnterior) { this.stockAnterior = stockAnterior; }

    public Integer getStockActual() { return stockActual; }
    public void setStockActual(Integer stockActual) { this.stockActual = stockActual; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isSimularFallo() { return simularFallo; }
    public void setSimularFallo(boolean simularFallo) { this.simularFallo = simularFallo; }
}