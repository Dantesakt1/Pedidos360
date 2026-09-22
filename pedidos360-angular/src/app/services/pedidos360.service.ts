import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pedido } from '../models/pedido.model';
import { Producto } from '../models/producto.model';

@Injectable({
    providedIn: 'root'
})
export class Pedidos360Service {
    // Cambia el puerto si tu Spring Boot corre en otro distinto
    private apiUrlOrders = 'http://localhost:8080/api/orders';
    private apiUrlCatalog = 'http://localhost:8080/api/catalog/products';

    constructor(private http: HttpClient) { }

    // --- MÓDULO CATÁLOGO ---
    obtenerProductos(): Observable<Producto[]> {
        return this.http.get<Producto[]>(this.apiUrlCatalog);
    }

    crearProducto(producto: Producto): Observable<Producto> {
        return this.http.post<Producto>(this.apiUrlCatalog, producto);
    }

    // --- MÓDULO PEDIDOS ---
    obtenerPedidos(): Observable<Pedido[]> {
        return this.http.get<Pedido[]>(this.apiUrlOrders);
    }

    crearPedido(pedido: Pedido): Observable<Pedido> {
        return this.http.post<Pedido>(this.apiUrlOrders, pedido);
    }

    cambiarEstadoPedido(id: number, estado: string): Observable<Pedido> {
        return this.http.put<Pedido>(`${this.apiUrlOrders}/${id}/status`, { estado });
    }
}