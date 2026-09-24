import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MsalService } from '@azure/msal-angular';
import { Pedidos360Service } from '../services/pedidos360.service';
import { Producto } from '../models/producto.model';

@Component({
    selector: 'app-catalog',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './catalog.component.html'
})
export class CatalogComponent implements OnInit {
    productos: Producto[] = [];
    nuevoProducto: Producto = { nombre: '', precio: 0, stock: 0 };
    esAdminOp: boolean = false; // Bandera de permisos

    private msalService = inject(MsalService);

    constructor(
        private apiService: Pedidos360Service,
        private cdr: ChangeDetectorRef 
    ) { }

    ngOnInit(): void {
        this.verificarRolUsuario();
        this.cargarProductos();
    }

    verificarRolUsuario(): void {
        const cuenta = this.msalService.instance.getActiveAccount();
        if (cuenta && cuenta.username) {
            const email = cuenta.username.toLowerCase();
            // Si el correo es el tuyo, o contiene admin u operador, tiene privilegios de gestión
            this.esAdminOp = email.includes('admin') || email.includes('operador') || email.includes('feña');
        }
    }

    cargarProductos(): void {
        this.apiService.obtenerProductos().subscribe({
            next: (data) => {
                this.productos = data;
                this.cdr.detectChanges(); 
            },
            error: (err) => console.error('Error al cargar catálogo', err)
        });
    }

    guardarProducto(): void {
        if (!this.esAdminOp) return;
        this.apiService.crearProducto(this.nuevoProducto).subscribe({
            next: () => {
                this.cargarProductos(); 
                this.nuevoProducto = { nombre: '', precio: 0, stock: 0 }; 
                document.getElementById('cerrarModalBtn')?.click(); 
            },
            error: (err) => console.error('Error al crear producto', err)
        });
    }

    cambiarStock(producto: Producto, cantidad: number): void {
        if (!this.esAdminOp) {
            alert('Acceso denegado: Los clientes no pueden modificar el stock.');
            return;
        }

        const nuevoStock = producto.stock + cantidad;
        if (nuevoStock < 0) return; 
        
        const productoActualizado = { ...producto, stock: nuevoStock };
        
        if (producto.id !== undefined) {
            this.apiService.actualizarProducto(producto.id, productoActualizado).subscribe({
                next: () => {
                    producto.stock = nuevoStock; 
                    this.cdr.detectChanges();    
                },
                error: (err) => console.error('Error al actualizar stock', err)
            });
        }
    }
}