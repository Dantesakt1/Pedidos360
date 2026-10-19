package com.pedidos360.backend.model;

public record PedidoEvento(
    Long pedidoId,
    String estado,
    String clienteEmail,
    boolean falloNotificaciones,
    boolean falloCocina,
    boolean falloDocumentos,
    String timestamp 
) {}