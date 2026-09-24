package com.pedidos360.backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pedidos360.backend.model.EstadoPedido;
import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.model.PedidoRequest;
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

    @Transactional
    public Pedido crearPedido(PedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(request.getClienteId());
        pedido.setEstado(EstadoPedido.CREADO);

        if (request.getProductoIds() != null && !request.getProductoIds().isEmpty()) {
            List<Producto> productos = productoRepository.findAllById(request.getProductoIds());
            pedido.setProductos(productos);
        }

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cambiarEstado(Long pedidoId, String nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // REGLA DE NEGOCIO: Si pasa a ACEPTADO desde CREADO, descontamos el stock real acumulado
        if ("ACEPTADO".equals(nuevoEstado) && pedido.getEstado() == EstadoPedido.CREADO) {
            if (pedido.getProductos() != null && !pedido.getProductos().isEmpty()) {
                
                // Agrupamos los IDs para contar cuántas unidades se pidieron de cada producto exacto
                Map<Long, Long> conteoProductos = pedido.getProductos().stream()
                        .collect(Collectors.groupingBy(Producto::getId, Collectors.counting()));

                for (Map.Entry<Long, Long> entry : conteoProductos.entrySet()) {
                    Long productoId = entry.getKey();
                    int cantidadPedida = entry.getValue().intValue();

                    Producto producto = productoRepository.findById(productoId)
                            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                    int stockActual = producto.getStock() != null ? producto.getStock() : 0;
                    if (stockActual < cantidadPedida) {
                        throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre() + " (Disponibles: " + stockActual + ")");
                    }

                    // Descontamos la cantidad exacta de una sola vez
                    producto.setStock(stockActual - cantidadPedida);
                    productoRepository.save(producto);
                }
            }
        }

        pedido.setEstado(EstadoPedido.valueOf(nuevoEstado));
        return pedidoRepository.save(pedido);
    }
}