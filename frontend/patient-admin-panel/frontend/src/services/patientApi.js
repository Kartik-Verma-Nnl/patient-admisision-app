import { API_BASE_URL, ENDPOINTS } from '../config/api';

// Error carrying the backend's message and per-field errors
export class ApiError extends Error {
  constructor(message, fieldErrors = {}) {
    super(message);
    this.fieldErrors = fieldErrors;
  }
}

async function request(path, options = {}) {
  let res;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
  } catch {
    throw new ApiError('Cannot reach the server. Please try again.');
  }
  if (res.status === 204) return null;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    // Expected error shape: { message: string, errors: { field: message } }
    throw new ApiError(body.message || `Request failed (${res.status})`, body.errors || {});
  }
  return body;
}

export const patientApi = {
  list: () => request(ENDPOINTS.PATIENTS),
  create: (data) => request(ENDPOINTS.PATIENTS, { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) =>
    request(ENDPOINTS.PATIENT_BY_ID(id), { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id) => request(ENDPOINTS.PATIENT_BY_ID(id), { method: 'DELETE' }),
  discharge: (id) => request(ENDPOINTS.PATIENT_DISCHARGE(id), { method: 'PATCH' }),
};
