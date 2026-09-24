package com.pedidos360.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.model.PedidoRequest;
import com.pedidos360.backend.service.PedidoService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
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
    public Pedido crearPedido(@RequestBody PedidoRequest request) {
        return pedidoService.crearPedido(request);
    }

    @PutMapping("/{id}/status")
    public Pedido cambiarEstado(@PathVariable Long id, @RequestBody CambioEstadoRequest request) {
        return pedidoService.cambiarEstado(id, request.getEstado());
    }
}

class CambioEstadoRequest {
    private String estado;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}