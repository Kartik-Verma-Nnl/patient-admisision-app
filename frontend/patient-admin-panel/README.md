# Patient Admin Panel (frontend only)

React + MUI. No login. The backend is the Java API, not part of this project.

## Run
```bash
cd frontend && npm install && npm run dev    # http://localhost:3000
```
Dev server proxies `/api` to `http://localhost:8080` (edit `frontend/vite.config.js`).

## Docker (frontend only)
```bash
cd frontend && docker build -t patient-admin . && docker run -p 3000:80 patient-admin
```
nginx forwards `/api/` to `http://backend:8080/api/` (edit `frontend/nginx.conf`).

## Pointing to the Java API
All endpoints live in `frontend/src/config/api.js`: change `API_BASE_URL` (or `VITE_API_BASE_URL`) or any path in `ENDPOINTS`.

Expected contract:
- `GET /patients` -> array of `{ id, name, age, gender, ward, department, doctor, admissionDate (YYYY-MM-DD), status: "Admitted" | "Discharged" }`
- `POST /patients`, `PUT /patients/{id}` (body without id/status), `DELETE /patients/{id}`, `PATCH /patients/{id}/discharge`
- Validation errors: `400 { "message": "...", "errors": { "<field>": "<message>" } }` (shown on the form fields)
- The Java side must repeat the validation rules (name letters only, age 0-120, required selects, admission date not in the future).

Dropdown lists (gender, ward, department, doctor) are in `frontend/src/config/options.js`.
