// ONE place for every API endpoint. When the Java backend is ready,
// change API_BASE_URL (or an individual path) here and nothing else.
// Default '/api' works with the Vite dev proxy and the nginx proxy.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';

export const ENDPOINTS = {
  PATIENTS: '/patients',                          // GET list, POST create
  PATIENT_BY_ID: (id) => `/patients/${id}`,       // GET, PUT, DELETE
  PATIENT_DISCHARGE: (id) => `/patients/${id}/discharge`, // PATCH
};
