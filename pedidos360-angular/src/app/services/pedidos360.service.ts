import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pedido } from '../models/pedido.model';
import { Producto } from '../models/producto.model';
import { environment } from '../environments/environment'; // Asegúrate de que la ruta sea correcta

@Injectable({
  providedIn: 'root',
})
export class Pedidos360Service {
  // Las URLs ahora se arman dinámicamente según estés en local o producción
  private apiUrlOrders = `${environment.apiBaseUrl}/api/orders`;
  private apiUrlCatalog = `${environment.apiBaseUrl}/api/catalog/products`;

  constructor(private http: HttpClient) {}

  // --- MÓDULO CATÁLOGO ---
  obtenerProductos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.apiUrlCatalog);
  }

  crearProducto(producto: Producto): Observable<Producto> {
    return this.http.post<Producto>(this.apiUrlCatalog, producto);
  }

  // --- MÓDULO PEDIDOS ---
  obtenerPedidos() {
    return this.http.get<any[]>(`${environment.apiBaseUrl}/api/orders`);
  }

  crearPedido(pedido: Pedido): Observable<Pedido> {
    return this.http.post<Pedido>(this.apiUrlOrders, pedido);
  }

  cambiarEstadoPedido(id: number, estado: string): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.apiUrlOrders}/${id}/status`, { estado });
  }

  actualizarProducto(id: number, producto: any) {
    return this.http.put(`${environment.apiBaseUrl}/api/catalog/products/${id}`, producto);
  }
}
