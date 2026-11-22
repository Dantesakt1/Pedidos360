package com.pedidos360.backend.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.pedidos360.backend.dto.InventarioKafkaEvent;
import com.pedidos360.backend.kafka.KafkaEventProducer;
import com.pedidos360.backend.model.Producto;
import com.pedidos360.backend.repository.ProductoRepository;

@RestController
@RequestMapping("/api/catalog/products")
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final KafkaEventProducer kafkaEventProducer;

    public ProductoController(ProductoRepository productoRepository, KafkaEventProducer kafkaEventProducer) {
        this.productoRepository = productoRepository;
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @GetMapping
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    @PostMapping
    public Producto crearProducto(@RequestBody Producto producto) {
        Producto guardado = productoRepository.save(producto);
        kafkaEventProducer.publicarEventoInventario(new InventarioKafkaEvent(
                "PRODUCTO_CREADO",
                guardado.getId(),
                guardado.getNombre(),
                0,
                guardado.getStock(),
                false
        ));
        return guardado;
    }

    @PutMapping("/{id}")
    public Producto actualizarProducto(@PathVariable Long id, @RequestBody Producto productoActualizado) {
        return productoRepository.findById(id).map(producto -> {
            int stockAnterior = producto.getStock() != null ? producto.getStock() : 0;
            producto.setNombre(productoActualizado.getNombre());
            producto.setPrecio(productoActualizado.getPrecio());
            producto.setStock(productoActualizado.getStock());
            Producto actualizado = productoRepository.save(producto);

            kafkaEventProducer.publicarEventoInventario(new InventarioKafkaEvent(
                    "STOCK_ACTUALIZADO",
                    actualizado.getId(),
                    actualizado.getNombre(),
                    stockAnterior,
                    actualizado.getStock(),
                    false
            ));
            return actualizado;
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }
}