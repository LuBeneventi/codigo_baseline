const API_BASE_URL = (import.meta as any).env?.VITE_API_URL || 'http://localhost:8088';

export function getToken(): string | null {
  return localStorage.getItem('token');
}

export function getRole(): string | null {
  return localStorage.getItem('role');
}

export function setAuth(token: string, role: string) {
  localStorage.setItem('token', token);
  localStorage.setItem('role', role);
}

export function clearAuth() {
  localStorage.removeItem('token');
  localStorage.removeItem('role');
}

export async function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // BASELINE-VULN [SEC-REQ-005]: el rol administrativo se toma del cliente (localStorage) y se envia en cabecera
  const clientRole = getRole();
  if (clientRole) {
    headers['X-User-Role'] = clientRole;
  }

  const url = `${API_BASE_URL}${path}`;
  return fetch(url, {
    ...options,
    headers,
  });
}
