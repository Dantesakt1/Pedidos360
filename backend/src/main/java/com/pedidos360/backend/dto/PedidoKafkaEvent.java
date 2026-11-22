package com.pedidos360.backend.dto;

import java.time.Instant;
import java.util.UUID;

public class PedidoKafkaEvent {
    private String eventId = UUID.randomUUID().toString();
    private String tipoEvento;
    private Long pedidoId;
    private String clienteId;
    private String estado;
    private Double total;
    private String timestamp = Instant.now().toString();
    private boolean simularFallo = false;

    public PedidoKafkaEvent() {}

    public PedidoKafkaEvent(String tipoEvento, Long pedidoId, String clienteId, String estado, Double total, boolean simularFallo) {
        this.eventId = UUID.randomUUID().toString();
        this.tipoEvento = tipoEvento;
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.estado = estado;
        this.total = total;
        this.timestamp = Instant.now().toString();
        this.simularFallo = simularFallo;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isSimularFallo() { return simularFallo; }
    public void setSimularFallo(boolean simularFallo) { this.simularFallo = simularFallo; }
}