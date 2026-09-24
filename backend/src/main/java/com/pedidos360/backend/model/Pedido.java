package com.pedidos360.backend.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore; // Importación agregada para solucionar el error 500

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clienteId; // ID del usuario extraído del token de Entra ID
    
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado = EstadoPedido.CREADO;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Relación con los productos para gestionar el catálogo y el stock
    @JsonIgnore // Esto evita la recursión infinita al enviar los datos a Angular
    @ManyToMany
    @JoinTable(
        name = "pedido_productos",
        joinColumns = @JoinColumn(name = "pedido_id"),
        inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private List<Producto> productos;
}