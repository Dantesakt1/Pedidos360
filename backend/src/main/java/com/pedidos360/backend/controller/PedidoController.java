package com.pedidos360.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pedidos360.backend.model.EstadoPedido;
import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.service.PedidoService;

@RestController
@RequestMapping("/api/orders")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.obtenerTodos();
    }

    @PostMapping
    public Pedido crearPedido(@RequestBody Pedido pedido) {
        return pedidoService.crearPedido(pedido);
    }

    // Usamos un DTO simple o un String para recibir el nuevo estado
    @PutMapping("/{id}/status")
    public Pedido cambiarEstado(@PathVariable Long id, @RequestBody CambioEstadoRequest request) {
        return pedidoService.cambiarEstado(id, request.getEstado());
    }
}

// Clase auxiliar para recibir el JSON del cambio de estado
class CambioEstadoRequest {
    private EstadoPedido estado;

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }
}