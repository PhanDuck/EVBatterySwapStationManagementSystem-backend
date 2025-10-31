# EVBS Simple Frontend (for testing)

This is a small React + Vite frontend to test the EV Battery Swap Station backend APIs.

Features included:
- Login (stores JWT in localStorage)
- Stations list and detail
- Create booking (uses your vehicles and stations)
- My bookings (list + cancel)
- Swap by confirmation code (public endpoints)

Quick start
1. Install dependencies from the `frontend` folder:

```bash
cd frontend
npm install
```

2. Run dev server (default Vite port 5173):

```bash
npm run dev
```

3. Configure backend URL with environment variable `VITE_API_URL`. By default it points to `http://localhost:8080`.

Example (PowerShell):

```powershell
$env:VITE_API_URL = 'http://localhost:8080'
npm run dev
```

Notes
- This is a minimal test UI. It assumes your backend is running and CORS allows the frontend origin. If you run both on the same machine, update `VITE_API_URL` accordingly.
- The app stores JWT in localStorage under `token`. It attaches the token in `Authorization: Bearer <token>` for secured endpoints.

Next steps (suggested):
- Add form validation and better styling
- Add subscription pages (renew/upgrade/downgrade)
- Add staff/admin flows
