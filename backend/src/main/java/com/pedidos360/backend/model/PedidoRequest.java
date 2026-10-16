package com.pedidos360.backend.model;

import java.util.List;

import lombok.Data;

@Data
public class PedidoRequest {
    private String clienteId;
    private List<Long> productoIds;

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public List<Long> getProductoIds() {
        return productoIds;
    }

    public void setProductoIds(List<Long> productoIds) {
        this.productoIds = productoIds;
    }
}