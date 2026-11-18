package com.pedidos360.backend.model;

import java.util.List;

public class PedidoRequest {
    private String clienteId;
    private List<Long> productoIds;
    private List<ItemRequest> items;

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public List<Long> getProductoIds() { return productoIds; }
    public void setProductoIds(List<Long> productoIds) { this.productoIds = productoIds; }

    public List<ItemRequest> getItems() { return items; }
    public void setItems(List<ItemRequest> items) { this.items = items; }

    public static class ItemRequest {
        private Long productoId;
        private Integer cantidad;

        public Long getProductoId() { return productoId; }
        public void setProductoId(Long productoId) { this.productoId = productoId; }

        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }
}