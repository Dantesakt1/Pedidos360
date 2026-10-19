package com.pedidos360.backend.controller;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
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
    public ResponseEntity<String> crearCola(@RequestParam String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre de la cola no puede estar vacío");
        }
        amqpAdmin.declareQueue(new Queue(nombre, true));
        return ResponseEntity.ok("Cola '" + nombre + "' creada exitosamente en el clúster");
    }

    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<String> eliminarCola(@PathVariable String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre de la cola no puede estar vacío");
        }
        amqpAdmin.deleteQueue(nombre);
        return ResponseEntity.ok("Cola '" + nombre + "' eliminada del clúster");
    }
}
