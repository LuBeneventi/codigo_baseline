export interface User {
  id: number;
  email: string;
  name: string;
  phone: string;
  role: string;
  passwordHash?: string;
}

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stock: number;
  imageUrl?: string;
}

export interface OrderItem {
  id?: number;
  productoId: number;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  subtotal?: number;
}

export interface Order {
  id: number;
  clienteId: string;
  clienteNombre: string;
  clienteEmail: string;
  clienteRut?: string;
  clienteDireccion?: string;
  total: number;
  estado: string;
  fechaCreacion: string;
  items?: OrderItem[];
}

export interface EnrichedOrderItem {
  id: number;
  productoId: number;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  productDetail?: Product;
}

export interface EnrichedOrder {
  id: number;
  clienteId: string;
  clienteNombre: string;
  clienteEmail: string;
  clienteRut?: string;
  clienteDireccion?: string;
  total: number;
  estado: string;
  fechaCreacion: string;
  fechaActualizacion?: string;
  items: EnrichedOrderItem[];
}
