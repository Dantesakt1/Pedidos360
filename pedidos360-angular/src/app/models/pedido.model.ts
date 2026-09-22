import { Producto } from './producto.model';

export interface Pedido {
    id?: number;
    clienteId: string;
    estado: string; // CREADO, ACEPTADO, EN_PREPARACION, DESPACHADO, ENTREGADO, CANCELADO
    fechaCreacion: string;
    productos: Producto[];
}