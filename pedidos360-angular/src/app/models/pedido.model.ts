import { Producto } from './producto.model';

export interface PedidoItem {
    id?: number;
    producto: Producto;
    cantidad: number;
    subtotal: number;
}

export interface Pedido {
    id?: number;
    clienteId: string;
    estado: string;
    fechaCreacion: string;
    total: number;
    items: PedidoItem[];
}