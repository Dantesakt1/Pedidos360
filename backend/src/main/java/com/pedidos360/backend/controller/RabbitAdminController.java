package com.pedidos360.backend.controller;

import java.util.Map;
import org.springframework.amqp.core.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rabbit")
public class RabbitAdminController {

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminController(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    @PostMapping("/queues")
    public ResponseEntity<?> crearCola(@RequestBody ColaRequest request) {
        if (request == null || request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre de la cola no puede estar vacío"));
        }
        String nombre = request.getNombre().trim();
        amqpAdmin.declareQueue(new Queue(nombre, true));
        return ResponseEntity.ok(Map.of("mensaje", "Cola '" + nombre + "' creada en el clúster RabbitMQ"));
    }

    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<?> eliminarCola(@PathVariable("nombre") String nombre) {
        amqpAdmin.deleteQueue(nombre.trim());
        return ResponseEntity.ok(Map.of("mensaje", "Cola '" + nombre + "' eliminada del clúster"));
    }

    @PostMapping("/exchanges")
    public ResponseEntity<?> crearExchange(@RequestBody ExchangeRequest request) {
        if (request == null || request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre del exchange es obligatorio"));
        }
        String nombre = request.getNombre().trim();
        String tipo = request.getTipo() != null ? request.getTipo().trim() : "direct";

        Exchange exchange = "topic".equalsIgnoreCase(tipo)
                ? new TopicExchange(nombre, true, false)
                : new DirectExchange(nombre, true, false);

        amqpAdmin.declareExchange(exchange);
        return ResponseEntity.ok(Map.of("mensaje", "Exchange '" + nombre + "' (" + tipo + ") creado"));
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<?> eliminarExchange(@PathVariable("nombre") String nombre) {
        amqpAdmin.deleteExchange(nombre.trim());
        return ResponseEntity.ok(Map.of("mensaje", "Exchange '" + nombre + "' eliminado"));
    }

    @PostMapping("/bindings")
    public ResponseEntity<?> crearBinding(@RequestBody BindingRequest request) {
        Binding binding = new Binding(
                request.getCola().trim(),
                Binding.DestinationType.QUEUE,
                request.getExchange().trim(),
                request.getRoutingKey().trim(),
                null
        );
        amqpAdmin.declareBinding(binding);
        return ResponseEntity.ok(Map.of("mensaje", "Binding creado: " + request.getExchange() + " -> [" + request.getRoutingKey() + "] -> " + request.getCola()));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<?> eliminarBinding(@RequestBody BindingRequest request) {
        Binding binding = new Binding(
                request.getCola().trim(),
                Binding.DestinationType.QUEUE,
                request.getExchange().trim(),
                request.getRoutingKey().trim(),
                null
        );
        amqpAdmin.removeBinding(binding);
        return ResponseEntity.ok(Map.of("mensaje", "Binding eliminado"));
    }

    public static class ColaRequest {
        private String nombre;
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }

    public static class ExchangeRequest {
        private String nombre;
        private String tipo;
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
    }

    public static class BindingRequest {
        private String cola;
        private String exchange;
        private String routingKey;
        public String getCola() { return cola; }
        public void setCola(String cola) { this.cola = cola; }
        public String getExchange() { return exchange; }
        public void setExchange(String exchange) { this.exchange = exchange; }
        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    }
}