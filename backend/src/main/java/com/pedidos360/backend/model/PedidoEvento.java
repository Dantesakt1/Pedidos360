package com.pedidos360.backend.model;

import java.time.LocalDateTime;

public record PedidoEvento(
    Long pedidoId,
    String estado,
    String clienteEmail,
    boolean falloNotificaciones,
    boolean falloCocina,
    boolean falloDocumentos,
    LocalDateTime timestamp
) {}