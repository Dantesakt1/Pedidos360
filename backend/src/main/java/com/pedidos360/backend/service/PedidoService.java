package com.pedidos360.backend.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pedidos360.backend.model.*;
import com.pedidos360.backend.repository.PedidoRepository;
import com.pedidos360.backend.repository.ProductoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final PedidoEventoProducer producer;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository,
            PedidoEventoProducer producer) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.producer = producer;
    }

    public List<Pedido> obtenerTodos() {
        return pedidoRepository.findAll();
    }

    @Transactional
    public Pedido crearPedido(PedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(request.getClienteId());
        pedido.setEstado(EstadoPedido.CREADO);

        Map<Long, Integer> cantidadesPorProducto = new HashMap<>();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (PedidoRequest.ItemRequest itemReq : request.getItems()) {
                cantidadesPorProducto.merge(itemReq.getProductoId(), itemReq.getCantidad(), Integer::sum);
            }
        } else if (request.getProductoIds() != null && !request.getProductoIds().isEmpty()) {
            for (Long prodId : request.getProductoIds()) {
                cantidadesPorProducto.merge(prodId, 1, Integer::sum);
            }
        }

        double totalCalculado = 0.0;
        for (Map.Entry<Long, Integer> entry : cantidadesPorProducto.entrySet()) {
            Producto producto = productoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado ID: " + entry.getKey()));

            int cantidad = entry.getValue();
            double precio = producto.getPrecio() != null ? producto.getPrecio() : 0.0;
            double subtotal = precio * cantidad;

            PedidoItem item = new PedidoItem();
            item.setPedido(pedido);
            item.setProducto(producto);
            item.setCantidad(cantidad);
            item.setSubtotal(subtotal);

            pedido.getItems().add(item);
            totalCalculado += subtotal;
        }

        pedido.setTotal(totalCalculado);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cambiarEstado(Long pedidoId, String nuevoEstado, boolean falloNotif, boolean falloCocina, boolean falloDocs) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        EstadoPedido estadoDestino;
        try {
            estadoDestino = EstadoPedido.valueOf(nuevoEstado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido: " + nuevoEstado);
        }

        if (estadoDestino == EstadoPedido.ACEPTADO && pedido.getEstado() == EstadoPedido.CREADO) {
            if (pedido.getItems() != null) {
                for (PedidoItem item : pedido.getItems()) {
                    Producto producto = productoRepository.findById(item.getProducto().getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

                    int stockActual = producto.getStock() != null ? producto.getStock() : 0;
                    int cantidadPedida = item.getCantidad() != null ? item.getCantidad() : 1;

                    if (stockActual < cantidadPedida) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Stock insuficiente para: " + producto.getNombre() + " (Disponibles: " + stockActual + ")");
                    }

                    producto.setStock(stockActual - cantidadPedida);
                    productoRepository.save(producto);
                }
            }
        }

        pedido.setEstado(estadoDestino);
        Pedido guardado = pedidoRepository.save(pedido);

        // Publicar en RabbitMQ
        if (estadoDestino == EstadoPedido.ACEPTADO) {
            PedidoEvento evento = new PedidoEvento(
                    guardado.getId(),
                    guardado.getEstado().name(),
                    guardado.getClienteId() != null ? guardado.getClienteId() : "cliente@pedidos360.cl",
                    falloNotif, falloCocina, falloDocs,
                    LocalDateTime.now().toString()
            );
            producer.publicarTareas(evento);
        } else {
            PedidoEvento eventoEstado = new PedidoEvento(
                    guardado.getId(),
                    guardado.getEstado().name(),
                    guardado.getClienteId() != null ? guardado.getClienteId() : "cliente@pedidos360.cl",
                    false, false, false,
                    LocalDateTime.now().toString()
            );
            producer.publicarTopic("pedido." + estadoDestino.name().toLowerCase(), eventoEstado);
        }

        return guardado;
    }
}