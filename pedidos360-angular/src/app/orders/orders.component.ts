import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // 1. Importación obligatoria
import { Pedidos360Service } from '../services/pedidos360.service';
import { Pedido } from '../models/pedido.model';

@Component({
    selector: 'app-orders',
    standalone: true, // 2. Indicar que es un componente independiente
    imports: [CommonModule], // 3. Importar el módulo que contiene 'date', 'ngFor' y 'ngIf'
    templateUrl: './orders.component.html'
})
export class OrdersComponent implements OnInit {
    pedidos: Pedido[] = [];

    constructor(private apiService: Pedidos360Service) { }

    ngOnInit(): void {
        this.cargarPedidos();
    }

    cargarPedidos(): void {
        this.apiService.obtenerPedidos().subscribe({
            next: (data) => this.pedidos = data,
            error: (err) => console.error('Error al cargar pedidos', err)
        });
    }

    avanzarEstado(pedido: Pedido, nuevoEstado: string): void {
        if (pedido.id) {
            this.apiService.cambiarEstadoPedido(pedido.id, nuevoEstado).subscribe({
                next: () => {
                    alert(`Pedido actualizado a ${nuevoEstado}`);
                    this.cargarPedidos(); // Recargar la lista
                },
                error: (err) => alert('Error: No se cumple la regla de negocio. ' + err.error.message)
            });
        }
    }
}