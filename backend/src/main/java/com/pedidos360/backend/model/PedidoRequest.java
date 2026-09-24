package com.pedidos360.backend.model;
import java.util.List;

import lombok.Data;

@Data
public class PedidoRequest {
    private String clienteId;
    private List<Long> productoIds; // IDs de los productos elegidos del catálogo
}