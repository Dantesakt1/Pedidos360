package com.pedidos360.backend.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.pedidos360.backend.dto.PedidoKafkaEvent;
import com.pedidos360.backend.kafka.KafkaEventProducer;
import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.model.PedidoRequest;
import com.pedidos360.backend.service.PedidoService;

@RestController
@RequestMapping("/api/orders")
public class PedidoController {

    private final PedidoService pedidoService;
    private final KafkaEventProducer kafkaEventProducer;

    public PedidoController(PedidoService pedidoService, KafkaEventProducer kafkaEventProducer) {
        this.pedidoService = pedidoService;
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.obtenerTodos();
    }

    @PostMapping
    public Pedido crearPedido(@RequestBody PedidoRequest request) {
        Pedido guardado = pedidoService.crearPedido(request);
        kafkaEventProducer.publicarEventoPedido(new PedidoKafkaEvent(
                "PEDIDO_CREADO",
                guardado.getId(),
                guardado.getClienteId(),
                guardado.getEstado().name(),
                guardado.getTotal(),
                false
        ));
        return guardado;
    }

    @PutMapping("/{id}/status")
    public Pedido cambiarEstado(@PathVariable("id") Long id, @RequestBody CambioEstadoRequest request) {
        Pedido actualizado = pedidoService.cambiarEstado(
                id,
                request.getEstado(),
                Boolean.TRUE.equals(request.getFalloNotificaciones()),
                Boolean.TRUE.equals(request.getFalloCocina()),
                Boolean.TRUE.equals(request.getFalloDocumentos())
        );
        kafkaEventProducer.publicarEventoPedido(new PedidoKafkaEvent(
                "ESTADO_ACTUALIZADO",
                actualizado.getId(),
                actualizado.getClienteId(),
                actualizado.getEstado().name(),
                actualizado.getTotal(),
                false
        ));
        return actualizado;
    }

    public static class CambioEstadoRequest {
        private String estado;
        private Boolean falloNotificaciones = false;
        private Boolean falloCocina = false;
        private Boolean falloDocumentos = false;

        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }

        public Boolean getFalloNotificaciones() { return falloNotificaciones; }
        public void setFalloNotificaciones(Boolean falloNotificaciones) { this.falloNotificaciones = falloNotificaciones; }

        public Boolean getFalloCocina() { return falloCocina; }
        public void setFalloCocina(Boolean falloCocina) { this.falloCocina = falloCocina; }

        public Boolean getFalloDocumentos() { return falloDocumentos; }
        public void setFalloDocumentos(Boolean falloDocumentos) { this.falloDocumentos = falloDocumentos; }
    }
}