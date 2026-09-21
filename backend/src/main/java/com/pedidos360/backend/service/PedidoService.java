package com.pedidos360.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pedidos360.backend.model.EstadoPedido;
import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.model.Producto;
import com.pedidos360.backend.repository.PedidoRepository;
import com.pedidos360.backend.repository.ProductoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    public List<Pedido> obtenerTodos() {
        return pedidoRepository.findAll();
    }

    public Pedido crearPedido(Pedido pedido) {
        pedido.setEstado(EstadoPedido.CREADO);
        return pedidoRepository.save(pedido);
    }

    // Método central que maneja las reglas de negocio exigidas
    @Transactional
    public Pedido cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // Regla 1: No se puede DESPACHAR si está recién CREADO o CANCELADO (debe pasar por ACEPTADO primero)
        if (nuevoEstado == EstadoPedido.DESPACHADO && 
           (pedido.getEstado() == EstadoPedido.CREADO || pedido.getEstado() == EstadoPedido.CANCELADO)) {
            throw new RuntimeException("Validación fallida: El pedido no puede ser DESPACHADO sin haber sido ACEPTADO previamente.");
        }

        // Regla 2: Al ACEPTAR un pedido, el stock de los productos asociados debe disminuir
        if (nuevoEstado == EstadoPedido.ACEPTADO && pedido.getEstado() == EstadoPedido.CREADO) {
            List<Producto> productos = pedido.getProductos();
            for (Producto p : productos) {
                if (p.getStock() <= 0) {
                    throw new RuntimeException("Validación fallida: Sin stock suficiente para el producto " + p.getNombre());
                }
                // Descontamos 1 unidad de stock por producto
                p.setStock(p.getStock() - 1);
                productoRepository.save(p); 
            }
        }

        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }
}