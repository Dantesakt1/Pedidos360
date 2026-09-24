import { Component, OnInit, ChangeDetectorRef, NgZone, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms';
import { MsalService } from '@azure/msal-angular';
import { Pedidos360Service } from '../services/pedidos360.service';
import { Pedido } from '../models/pedido.model';
import { Producto } from '../models/producto.model';

@Component({
    selector: 'app-orders',
    standalone: true, 
    imports: [CommonModule, FormsModule],
    templateUrl: './orders.component.html'
})
export class OrdersComponent implements OnInit {
    pedidos: Pedido[] = [];
    productosCatalogo: Producto[] = [];
    esAdminOp: boolean = false; // Bandera para diferenciar si es Admin u Operador
    
    // Estructura para el formulario de creación
    nuevoPedido = {
        clienteId: '',
        items: [] as { productoId: number, cantidad: number }[]
    };

    // Producto seleccionado temporalmente en el desplegable del modal
    productoSeleccionadoId: number | null = null;
    cantidadSeleccionada: number = 1;

    private ngZone = inject(NgZone);
    private msalService = inject(MsalService);

    constructor(
        private apiService: Pedidos360Service,
        private cdr: ChangeDetectorRef 
    ) { }

    ngOnInit(): void {
        this.verificarRolUsuario();
        this.cargarPedidos();
        this.cargarCatalogo();
    }

    verificarRolUsuario(): void {
        const cuenta = this.msalService.instance.getActiveAccount();
        if (cuenta && cuenta.username) {
            const email = cuenta.username.toLowerCase();
            // Si el correo contiene "admin", "operador" o es tu cuenta personal, tiene plenos poderes.
            // Si entra con un usuario de cliente (ej: cliente@...), se queda en false.
            this.esAdminOp = email.includes('admin') || email.includes('operador') || email.includes('feña');
        }
    }

    cargarPedidos(): void {
        this.apiService.obtenerPedidos().subscribe({
            next: (data) => {
                this.ngZone.run(() => {
                    this.pedidos = data;
                    this.cdr.detectChanges(); 
                });
            },
            error: (err) => console.error('Error al cargar pedidos', err)
        });
    }

    cargarCatalogo(): void {
        this.apiService.obtenerProductos().subscribe({
            next: (data) => {
                this.productosCatalogo = data;
                this.cdr.detectChanges();
            },
            error: (err) => console.error('Error al cargar catálogo', err)
        });
    }

    // Agregar producto a la lista del pedido actual con su cantidad
    agregarItemAlPedido(): void {
        if (!this.productoSeleccionadoId) return;
        
        const prodId = Number(this.productoSeleccionadoId);
        const cant = Number(this.cantidadSeleccionada);

        // Ver si ya estaba agregado para sumar la cantidad
        const existente = this.nuevoPedido.items.find(i => i.productoId === prodId);
        if (existente) {
            existente.cantidad += cant;
        } else {
            this.nuevoPedido.items.push({ productoId: prodId, cantidad: cant });
        }

        // Resetear selección temporal
        this.productoSeleccionadoId = null;
        this.cantidadSeleccionada = 1;
        this.cdr.detectChanges();
    }

    quitarItem(index: number): void {
        this.nuevoPedido.items.splice(index, 1);
        this.cdr.detectChanges();
    }

    // Obtener nombre del producto para mostrarlo en la tabla del modal
    getNombreProducto(id: number): string {
        const p = this.productosCatalogo.find(prod => prod.id === id);
        return p ? p.nombre : 'Desconocido';
    }

    crearPedido(): void {
        const productoIdsPlano: number[] = [];
        this.nuevoPedido.items.forEach(item => {
            for (let i = 0; i < item.cantidad; i++) {
                productoIdsPlano.push(item.productoId);
            }
        });

        const payload = {
            clienteId: this.nuevoPedido.clienteId,
            productoIds: productoIdsPlano
        };

        this.apiService.crearPedido(payload as any).subscribe({
            next: () => {
                this.ngZone.run(() => {
                    this.cargarPedidos(); 
                    this.nuevoPedido = { clienteId: '', items: [] }; 
                    document.getElementById('cerrarModalPedidoBtn')?.click(); 
                });
            },
            error: (err) => console.error('Error al crear pedido', err)
        });
    }

    avanzarEstado(pedido: Pedido, nuevoEstado: string): void {
        if (!this.esAdminOp) {
            alert('Acceso denegado: Su usuario de Cliente no tiene permisos operacionales.');
            return;
        }

        if (pedido.id) {
            this.apiService.cambiarEstadoPedido(pedido.id, nuevoEstado).subscribe({
                next: () => {
                    this.ngZone.run(() => {
                        this.cargarPedidos(); 
                    });
                },
                error: (err) => alert('Error de regla de negocio: ' + (err.error?.message || 'No se pudo actualizar el estado'))
            });
        }
    }
}