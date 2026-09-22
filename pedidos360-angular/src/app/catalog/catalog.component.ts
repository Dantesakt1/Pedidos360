import { Component, OnInit } from '@angular/core';
import { Pedidos360Service } from '../services/pedidos360.service';
import { Producto } from '../models/producto.model';

@Component({
    selector: 'app-catalog',
    templateUrl: './catalog.component.html'
})
export class CatalogComponent implements OnInit {
    productos: Producto[] = [];

    constructor(private apiService: Pedidos360Service) { }

    ngOnInit(): void {
        this.cargarProductos();
    }

    cargarProductos(): void {
        this.apiService.obtenerProductos().subscribe({
            next: (data) => this.productos = data,
            error: (err) => console.error('Error al cargar catálogo', err)
        });
    }
}