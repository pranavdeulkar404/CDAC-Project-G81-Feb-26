const API_BASE = 'http://localhost:8080/api';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}/${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });

  if (response.status === 204) return null;

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const validation = payload?.details
      ? Object.entries(payload.details).map(([field, message]) => `${field}: ${message}`).join(', ')
      : '';
    throw new Error(validation || payload?.message || payload?.detail || `Request failed (${response.status})`);
  }

  return payload;
}

export const api = {
  list: (resource) => request(resource),
  create: (resource, body) => request(resource, { method: 'POST', body: JSON.stringify(body) }),
  update: (resource, id, body) => request(`${resource}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  remove: (resource, id) => request(`${resource}/${id}`, { method: 'DELETE' }),
};
