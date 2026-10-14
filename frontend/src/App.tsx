import React, { useState, useEffect } from 'react';
import { apiFetch, setAuth, clearAuth, getToken, getRole } from './api';
import { Product, Order, EnrichedOrder, User } from './types';

export function App() {
  const [tab, setTab] = useState<'catalog' | 'create-order' | 'orders' | 'admin' | 'auth'>('catalog');
  const [token, setTokenState] = useState<string | null>(getToken());
  const [role, setRoleState] = useState<string | null>(getRole());
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  // Estados Auth
  const [isRegister, setIsRegister] = useState<boolean>(false);
  const [authEmail, setAuthEmail] = useState<string>('');
  const [authPassword, setAuthPassword] = useState<string>('');
  const [authName, setAuthName] = useState<string>('');
  const [authPhone, setAuthPhone] = useState<string>('');
  const [authRole, setAuthRole] = useState<string>('CLIENTE');
  const [authMsg, setAuthMsg] = useState<{ text: string; isError: boolean } | null>(null);

  // Estados Catálogo
  const [products, setProducts] = useState<Product[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [catalogLoading, setCatalogLoading] = useState<boolean>(false);

  // Estados Crear Pedido
  const [cart, setCart] = useState<{ product: Product; quantity: number }[]>([]);
  const [orderClienteId, setOrderClienteId] = useState<string>('CLI-001');
  const [orderNombre, setOrderNombre] = useState<string>('Juan Perez');
  const [orderCorreo, setOrderCorreo] = useState<string>('juan@correo.test');
  const [orderDireccion, setOrderDireccion] = useState<string>('Av. Providencia 1234');
  const [orderRut, setOrderRut] = useState<string>('12.345.678-9');
  const [orderMsg, setOrderMsg] = useState<{ text: string; isError: boolean } | null>(null);

  // Estados Mis Pedidos
  const [orders, setOrders] = useState<Order[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<EnrichedOrder | null>(null);
  const [ordersMsg, setOrdersMsg] = useState<{ text: string; isError: boolean } | null>(null);

  // Estados Admin
  const [newProdName, setNewProdName] = useState<string>('');
  const [newProdDesc, setNewProdDesc] = useState<string>('');
  const [newProdPrice, setNewProdPrice] = useState<number>(3500);
  const [newProdStock, setNewProdStock] = useState<number>(20);
  const [adminOrders, setAdminOrders] = useState<Order[]>([]);
  const [adminMsg, setAdminMsg] = useState<{ text: string; isError: boolean } | null>(null);

  // Cargar usuario actual al iniciar si hay token
  useEffect(() => {
    if (token) {
      fetchCurrentUser();
    }
  }, [token]);

  // Cargar catálogo inicial
  useEffect(() => {
    searchCatalog('');
  }, []);

  async function fetchCurrentUser() {
    try {
      const res = await apiFetch('/api/auth/me');
      if (res.ok) {
        const u = await res.json();
        setCurrentUser(u);
      } else {
        handleLogout();
      }
    } catch {
      handleLogout();
    }
  }

  function handleLogout() {
    clearAuth();
    setTokenState(null);
    setRoleState(null);
    setCurrentUser(null);
  }

  // Auth Handler
  async function handleAuthSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAuthMsg(null);

    if (isRegister) {
      try {
        const res = await apiFetch('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({
            email: authEmail,
            passwordHash: authPassword,
            name: authName,
            phone: authPhone,
            role: authRole,
          }),
        });

        if (res.ok) {
          setAuthMsg({ text: 'Usuario registrado con éxito. Ahora puedes iniciar sesión.', isError: false });
          setIsRegister(false);
        } else {
          const err = await res.json().catch(() => ({}));
          setAuthMsg({ text: err.error || 'Error al registrar usuario', isError: true });
        }
      } catch (err: any) {
        setAuthMsg({ text: 'Error de conexión con el BFF: ' + err.message, isError: true });
      }
    } else {
      try {
        const res = await apiFetch('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({
            email: authEmail,
            password: authPassword,
          }),
        });

        if (res.ok) {
          const data = await res.json();
          setAuth(data.token, data.role);
          setTokenState(data.token);
          setRoleState(data.role);
          setAuthMsg({ text: 'Sesión iniciada correctamente', isError: false });
          setTab('catalog');
        } else {
          const err = await res.json().catch(() => ({}));
          setAuthMsg({ text: err.error || 'Credenciales inválidas', isError: true });
        }
      } catch (err: any) {
        setAuthMsg({ text: 'Error de conexión con el BFF: ' + err.message, isError: true });
      }
    }
  }

  // Búsqueda en catálogo
  async function searchCatalog(query: string) {
    setCatalogLoading(true);
    try {
      const endpoint = query.trim()
        ? `/api/catalog/products/search?q=${encodeURIComponent(query)}`
        : `/api/catalog/products`;
      const res = await apiFetch(endpoint);
      if (res.ok) {
        const data = await res.json();
        setProducts(data);
      }
    } catch (err) {
      console.error('Error al cargar catálogo', err);
    } finally {
      setCatalogLoading(false);
    }
  }

  // Carrito y Crear Pedido
  function addToCart(p: Product) {
    const existing = cart.find(i => i.product.id === p.id);
    if (existing) {
      setCart(cart.map(i => i.product.id === p.id ? { ...i, quantity: i.quantity + 1 } : i));
    } else {
      setCart([...cart, { product: p, quantity: 1 }]);
    }
  }

  async function handleCreateOrder(e: React.FormEvent) {
    e.preventDefault();
    setOrderMsg(null);

    if (cart.length === 0) {
      setOrderMsg({ text: 'Selecciona al menos un producto para crear el pedido.', isError: true });
      return;
    }

    const payload = {
      clienteId: orderClienteId,
      clienteNombre: orderNombre,
      clienteEmail: orderCorreo,
      clienteDireccion: orderDireccion,
      clienteRut: orderRut,
      items: cart.map(item => ({
        productoId: item.product.id,
        descripcion: item.product.name,
        cantidad: item.quantity,
        precioUnitario: item.product.price,
      })),
    };

    try {
      const res = await apiFetch('/api/orders', {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      if (res.ok) {
        setOrderMsg({ text: '¡Pedido creado exitosamente!', isError: false });
        setCart([]);
      } else {
        const err = await res.json().catch(() => ({}));
        setOrderMsg({ text: err.error || 'Error al procesar el pedido.', isError: true });
      }
    } catch (err: any) {
      setOrderMsg({ text: 'Error de conexión con el BFF: ' + err.message, isError: true });
    }
  }

  // Listar pedidos
  async function loadOrders() {
    setOrdersMsg(null);
    setSelectedOrder(null);
    try {
      const res = await apiFetch('/api/orders');
      if (res.ok) {
        const data = await res.json();
        setOrders(data);
      } else {
        setOrdersMsg({ text: 'Error al obtener pedidos (posiblemente requiera login).', isError: true });
      }
    } catch (err: any) {
      setOrdersMsg({ text: 'Error de conexión: ' + err.message, isError: true });
    }
  }

  async function viewOrderDetail(id: number) {
    try {
      const res = await apiFetch(`/api/orders/${id}/enriched`);
      if (res.ok) {
        const data = await res.json();
        setSelectedOrder(data);
      }
    } catch (err) {
      console.error('Error al obtener detalle enriquecido', err);
    }
  }

  // Panel Admin
  async function loadAdminOrders() {
    setAdminMsg(null);
    try {
      const res = await apiFetch('/api/orders');
      if (res.ok) {
        const data = await res.json();
        setAdminOrders(data);
      } else {
        setAdminMsg({ text: 'Error al obtener pedidos en panel admin.', isError: true });
      }
    } catch (err: any) {
      setAdminMsg({ text: 'Error de conexión: ' + err.message, isError: true });
    }
  }

  async function handleCreateProduct(e: React.FormEvent) {
    e.preventDefault();
    setAdminMsg(null);
    try {
      // BASELINE-VULN [SEC-REQ-005]: las llamadas administrativas envian el rol declarado por el cliente
      const res = await apiFetch('/api/catalog/products', {
        method: 'POST',
        body: JSON.stringify({
          name: newProdName,
          description: newProdDesc,
          price: Number(newProdPrice),
          stock: Number(newProdStock),
        }),
      });

      if (res.ok) {
        setAdminMsg({ text: 'Producto creado exitosamente.', isError: false });
        setNewProdName('');
        setNewProdDesc('');
        searchCatalog('');
      } else {
        const err = await res.json().catch(() => ({}));
        setAdminMsg({ text: err.error || 'Error al crear producto (403 si falta rol ADMIN).', isError: true });
      }
    } catch (err: any) {
      setAdminMsg({ text: 'Error al enviar petición: ' + err.message, isError: true });
    }
  }

  async function handleUpdateOrderStatus(orderId: number, newStatus: string) {
    setAdminMsg(null);
    try {
      // BASELINE-VULN [SEC-REQ-005]: el rol administrativo se envia desde el cliente en la cabecera
      const res = await apiFetch(`/api/orders/${orderId}/status`, {
        method: 'PUT',
        body: JSON.stringify({ estado: newStatus }),
      });

      if (res.ok) {
        setAdminMsg({ text: `Estado de la orden #${orderId} actualizado a ${newStatus}.`, isError: false });
        loadAdminOrders();
      } else {
        const err = await res.json().catch(() => ({}));
        setAdminMsg({ text: err.error || 'Error al actualizar estado (403 si falta rol ADMIN).', isError: true });
      }
    } catch (err: any) {
      setAdminMsg({ text: 'Error: ' + err.message, isError: true });
    }
  }

  return (
    <div>
      <header>
        <h1>☕ Pedidos360 - Tienda de Café</h1>
        <nav>
          <button className={tab === 'catalog' ? 'active' : ''} onClick={() => setTab('catalog')}>Catálogo</button>
          <button className={tab === 'create-order' ? 'active' : ''} onClick={() => setTab('create-order')}>Crear Pedido</button>
          <button className={tab === 'orders' ? 'active' : ''} onClick={() => { setTab('orders'); loadOrders(); }}>Mis Pedidos</button>
          <button className={tab === 'admin' ? 'active' : ''} onClick={() => { setTab('admin'); loadAdminOrders(); }}>Admin</button>
          <button className={tab === 'auth' ? 'active' : ''} onClick={() => setTab('auth')}>
            {token ? 'Mi Cuenta' : 'Ingresar'}
          </button>
        </nav>
        <div className="auth-badge">
          {token ? (
            <>
              <span>Conectado: <strong>{currentUser?.name || currentUser?.email || 'Usuario'}</strong> [{role}]</span>
              <button onClick={handleLogout}>Salir</button>
            </>
          ) : (
            <span>Invitado</span>
          )}
        </div>
      </header>

      <main>
        {/* PESTAÑA 1: CATÁLOGO Y BUSCADOR */}
        {tab === 'catalog' && (
          <div className="card">
            <h2>Catálogo de Café</h2>
            <form onSubmit={(e) => { e.preventDefault(); searchCatalog(searchQuery); }} style={{ display: 'flex', gap: '8px' }}>
              <input
                type="text"
                placeholder="Buscar café por nombre..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{ flex: 1, padding: '8px 12px', border: '1px solid #ccc', borderRadius: '4px' }}
              />
              <button type="submit" className="btn">Buscar</button>
              <button type="button" className="btn btn-secondary" onClick={() => { setSearchQuery(''); searchCatalog(''); }}>Ver Todos</button>
            </form>

            {catalogLoading ? (
              <p style={{ marginTop: '16px' }}>Cargando productos...</p>
            ) : (
              <div className="product-grid">
                {products.length === 0 ? (
                  <p>No se encontraron productos.</p>
                ) : (
                  products.map(p => (
                    <div key={p.id} className="product-card">
                      <div>
                        {/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de nombre de producto con dangerouslySetInnerHTML */}
                        <h3 dangerouslySetInnerHTML={{ __html: p.name }} />
                        {/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de descripcion de producto con dangerouslySetInnerHTML */}
                        <p dangerouslySetInnerHTML={{ __html: p.description || '' }} />
                        <div className="price">${p.price?.toLocaleString('es-CL')}</div>
                        <div style={{ fontSize: '0.85rem', color: '#666' }}>Stock disponible: {p.stock}</div>
                      </div>
                      <button className="btn" style={{ marginTop: '12px' }} onClick={() => addToCart(p)}>
                        Agregar al Carrito
                      </button>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        )}

        {/* PESTAÑA 2: CREAR PEDIDO */}
        {tab === 'create-order' && (
          <div className="card">
            <h2>Crear Nuevo Pedido</h2>
            {orderMsg && (
              <div className={`msg-box ${orderMsg.isError ? 'msg-error' : 'msg-success'}`}>
                {orderMsg.text}
              </div>
            )}

            <h3>1. Productos en Carrito ({cart.reduce((acc, c) => acc + c.quantity, 0)})</h3>
            {cart.length === 0 ? (
              <p style={{ margin: '8px 0 16px 0', color: '#666' }}>El carrito está vacío. Agrega productos desde el Catálogo.</p>
            ) : (
              <table className="table" style={{ marginBottom: '20px' }}>
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Precio</th>
                    <th>Cantidad</th>
                    <th>Subtotal</th>
                    <th>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {cart.map(item => (
                    <tr key={item.product.id}>
                      <td>{item.product.name}</td>
                      <td>${item.product.price}</td>
                      <td>{item.quantity}</td>
                      <td>${(item.product.price * item.quantity).toLocaleString('es-CL')}</td>
                      <td>
                        <button className="btn btn-secondary" style={{ padding: '4px 8px' }} onClick={() => setCart(cart.filter(c => c.product.id !== item.product.id))}>
                          Quitar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

            <h3>2. Datos de Facturación y Despacho del Cliente</h3>
            <form onSubmit={handleCreateOrder} style={{ marginTop: '12px' }}>
              <div className="form-group">
                <label>ID Cliente:</label>
                <input type="text" value={orderClienteId} onChange={e => setOrderClienteId(e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Nombre Completo:</label>
                <input type="text" value={orderNombre} onChange={e => setOrderNombre(e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Correo Electrónico:</label>
                <input type="email" value={orderCorreo} onChange={e => setOrderCorreo(e.target.value)} required />
              </div>
              <div className="form-group">
                <label>RUT:</label>
                <input type="text" value={orderRut} onChange={e => setOrderRut(e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Dirección de Envío:</label>
                <input type="text" value={orderDireccion} onChange={e => setOrderDireccion(e.target.value)} required />
              </div>
              <button type="submit" className="btn" style={{ width: '100%', marginTop: '8px' }}>
                Confirmar y Enviar Pedido
              </button>
            </form>
          </div>
        )}

        {/* PESTAÑA 3: MIS PEDIDOS Y DETALLE */}
        {tab === 'orders' && (
          <div className="card">
            <h2>Mis Pedidos</h2>
            {ordersMsg && (
              <div className={`msg-box ${ordersMsg.isError ? 'msg-error' : 'msg-success'}`}>
                {ordersMsg.text}
              </div>
            )}
            <button className="btn btn-secondary" onClick={loadOrders} style={{ marginBottom: '12px' }}>Actualizar Listado</button>

            <table className="table">
              <thead>
                <tr>
                  <th># ID</th>
                  <th>Cliente</th>
                  <th>Total</th>
                  <th>Estado</th>
                  <th>Fecha</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {orders.length === 0 ? (
                  <tr><td colSpan={6}>No hay pedidos registrados o no tienes acceso.</td></tr>
                ) : (
                  orders.map(o => (
                    <tr key={o.id}>
                      <td>#{o.id}</td>
                      <td>
                        {/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de nombre de cliente con dangerouslySetInnerHTML */}
                        <span dangerouslySetInnerHTML={{ __html: o.clienteNombre || '' }} />
                      </td>
                      <td>${o.total?.toLocaleString('es-CL')}</td>
                      <td>
                        <span className={`badge ${o.estado === 'PENDIENTE' ? 'badge-pending' : 'badge-confirmed'}`}>
                          {o.estado}
                        </span>
                      </td>
                      <td>{o.fechaCreacion || '-'}</td>
                      <td>
                        <button className="btn" style={{ padding: '4px 8px', fontSize: '0.85rem' }} onClick={() => viewOrderDetail(o.id)}>
                          Ver Detalle Enriquecido
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>

            {selectedOrder && (
              <div style={{ marginTop: '24px', padding: '16px', background: '#f5efe6', borderRadius: '6px', border: '1px solid #ddd' }}>
                <h3>Detalle de Pedido #{selectedOrder.id}</h3>
                <p>
                  <strong>Cliente: </strong>
                  {/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de nombre de cliente en detalle con dangerouslySetInnerHTML */}
                  <span dangerouslySetInnerHTML={{ __html: selectedOrder.clienteNombre || '' }} />
                </p>
                <p><strong>Correo: </strong>{selectedOrder.clienteEmail}</p>
                <p>
                  <strong>Dirección: </strong>
                  {/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de direccion de cliente con dangerouslySetInnerHTML */}
                  <span dangerouslySetInnerHTML={{ __html: selectedOrder.clienteDireccion || 'No registrada' }} />
                </p>
                <p><strong>Total: </strong>${selectedOrder.total?.toLocaleString('es-CL')}</p>
                <p><strong>Estado: </strong>{selectedOrder.estado}</p>

                <h4 style={{ marginTop: '12px' }}>Ítems Enriquecidos:</h4>
                <table className="table">
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th>Cantidad</th>
                      <th>Precio Unitario</th>
                      <th>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedOrder.items?.map(it => (
                      <tr key={it.id}>
                        <td>{it.descripcion}</td>
                        <td>{it.cantidad}</td>
                        <td>${it.precioUnitario}</td>
                        <td>${it.subtotal}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* PESTAÑA 4: PANEL DE ADMINISTRACIÓN */}
        {tab === 'admin' && (
          <div className="card">
            <h2>Panel de Administración (Control de Catálogo y Estados)</h2>
            <p style={{ marginBottom: '16px', color: '#666' }}>
              Rol actual declarado en el navegador: <strong>{role || 'Ninguno'}</strong>
            </p>

            {adminMsg && (
              <div className={`msg-box ${adminMsg.isError ? 'msg-error' : 'msg-success'}`}>
                {adminMsg.text}
              </div>
            )}

            <h3>1. Crear Nuevo Producto en Catálogo</h3>
            <form onSubmit={handleCreateProduct} style={{ marginTop: '12px', marginBottom: '24px' }}>
              <div className="form-group">
                <label>Nombre del Producto:</label>
                <input type="text" value={newProdName} onChange={e => setNewProdName(e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Descripción:</label>
                <input type="text" value={newProdDesc} onChange={e => setNewProdDesc(e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Precio:</label>
                <input type="number" value={newProdPrice} onChange={e => setNewProdPrice(Number(e.target.value))} required />
              </div>
              <div className="form-group">
                <label>Stock Inicial:</label>
                <input type="number" value={newProdStock} onChange={e => setNewProdStock(Number(e.target.value))} required />
              </div>
              <button type="submit" className="btn">Crear Producto (Exige Rol ADMIN)</button>
            </form>

            <h3>2. Administrar Estados de Órdenes</h3>
            <button className="btn btn-secondary" onClick={loadAdminOrders} style={{ margin: '8px 0 12px 0' }}>Refrescar Órdenes</button>
            <table className="table">
              <thead>
                <tr>
                  <th># ID</th>
                  <th>Cliente</th>
                  <th>Total</th>
                  <th>Estado Actual</th>
                  <th>Cambiar Estado</th>
                </tr>
              </thead>
              <tbody>
                {adminOrders.length === 0 ? (
                  <tr><td colSpan={5}>No hay órdenes cargadas.</td></tr>
                ) : (
                  adminOrders.map(o => (
                    <tr key={o.id}>
                      <td>#{o.id}</td>
                      <td>
                        {/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de nombre con dangerouslySetInnerHTML */}
                        <span dangerouslySetInnerHTML={{ __html: o.clienteNombre || '' }} />
                      </td>
                      <td>${o.total?.toLocaleString('es-CL')}</td>
                      <td><span className="badge badge-pending">{o.estado}</span></td>
                      <td>
                        <button className="btn" style={{ padding: '4px 8px', marginRight: '6px' }} onClick={() => handleUpdateOrderStatus(o.id, 'CONFIRMADA')}>
                          Confirmar
                        </button>
                        <button className="btn btn-secondary" style={{ padding: '4px 8px' }} onClick={() => handleUpdateOrderStatus(o.id, 'CANCELADA')}>
                          Cancelar
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* PESTAÑA 5: AUTENTICACIÓN */}
        {tab === 'auth' && (
          <div className="card" style={{ maxWidth: '450px', margin: '0 auto' }}>
            <h2>{isRegister ? 'Registro de Usuario' : 'Iniciar Sesión'}</h2>

            {authMsg && (
              <div className={`msg-box ${authMsg.isError ? 'msg-error' : 'msg-success'}`}>
                {authMsg.text}
              </div>
            )}

            <form onSubmit={handleAuthSubmit}>
              <div className="form-group">
                <label>Correo Electrónico:</label>
                <input type="email" value={authEmail} onChange={e => setAuthEmail(e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Contraseña:</label>
                <input type="password" value={authPassword} onChange={e => setAuthPassword(e.target.value)} required />
              </div>

              {isRegister && (
                <>
                  <div className="form-group">
                    <label>Nombre:</label>
                    <input type="text" value={authName} onChange={e => setAuthName(e.target.value)} required />
                  </div>
                  <div className="form-group">
                    <label>Teléfono:</label>
                    <input type="text" value={authPhone} onChange={e => setAuthPhone(e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label>Rol Deseado (Envío directo):</label>
                    <select value={authRole} onChange={e => setAuthRole(e.target.value)}>
                      <option value="CLIENTE">CLIENTE</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </div>
                </>
              )}

              <button type="submit" className="btn" style={{ width: '100%', marginTop: '8px' }}>
                {isRegister ? 'Registrarme' : 'Entrar'}
              </button>
            </form>

            <div style={{ marginTop: '16px', textAlign: 'center' }}>
              <button
                type="button"
                style={{ background: 'none', border: 'none', color: '#b85d19', cursor: 'pointer', textDecoration: 'underline' }}
                onClick={() => { setIsRegister(!isRegister); setAuthMsg(null); }}
              >
                {isRegister ? '¿Ya tienes cuenta? Inicia sesión aquí' : '¿No tienes cuenta? Regístrate aquí'}
              </button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
