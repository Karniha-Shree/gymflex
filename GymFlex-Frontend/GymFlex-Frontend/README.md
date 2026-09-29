# GymFlex — Frontend

Plain HTML + CSS + JavaScript dashboard for the GymFlex backend. **No Node.js, npm or build step needed.**

```
GymFlex-Frontend/
  index.html        page layout (all sections + forms)
  css/style.css     styling
  js/config.js      API_BASE_URL  <-- the ONLY place with the backend address
  js/api.js         all fetch() calls, error handling
  js/app.js         page logic (dashboard, members, plans, memberships, attendance, expiring soon)
```

## 1. Open in VS Code
`File -> Open Folder...` -> select the `GymFlex-Frontend` folder.

## 2. Start the backend first
In Spring Tools run `GymflexApplication` (Run As -> Spring Boot App).
Check `http://localhost:8080/api/dashboard` shows JSON. MySQL must be running (the backend needs the `gymflex` database).

## 3. Run the frontend (use Live Server on port 5500)
1. In VS Code install the extension **Live Server** (by Ritwick Dey).
2. Right-click `index.html` -> **Open with Live Server** (or click "Go Live" at the bottom right).
3. The browser opens `http://127.0.0.1:5500/index.html`.

No extension? From inside the folder run: `python -m http.server 5500` and open `http://localhost:5500`.

> Do **not** double-click `index.html` (a `file://` page). The browser then sends the origin `null`, which the backend's CORS rules reject.

## 4. Backend API URL
`http://localhost:8080/api` (shown at the bottom of the sidebar).

## 5. Change API_BASE_URL
Edit **`js/config.js`**:
```js
const API_BASE_URL = "http://localhost:8080/api";
```
Use another port or machine here, e.g. `http://localhost:8081/api`. Nothing else needs changing.

## 6. MySQL requirement
The backend stores everything in MySQL (database `gymflex`). If MySQL is stopped, the backend will not start and the frontend will show "Cannot reach the backend".

## 7. Troubleshooting CORS
Symptom: red message "Cannot reach the backend..." while the backend is running, or a CORS error in the browser console (F12).
- Open the frontend through Live Server on port **5500** (`http://localhost:5500` or `http://127.0.0.1:5500`).
- Using a different port or address? Add it in the backend `src/main/resources/application.properties`:
  ```properties
  gymflex.cors.allowed-origins=http://localhost:5500,http://127.0.0.1:5500,http://localhost:5501
  ```
  (comma separated, no spaces) and restart the backend.
- Make sure the URL in `js/config.js` matches the backend port.

## 8. Example test workflow
1. **Dashboard** – shows the seeded counts (members, active/expired, expiring soon, check-ins).
2. **Members** – click *+ Add Member*; try an invalid email to see validation; then add a valid one. Use the search box, then *Edit* and *Delete*.
3. **Plans** – view Monthly/Quarterly/Half Yearly/Yearly; add a plan or edit one.
4. **Memberships** – *+ New Membership* for your new member (start date defaults to today; expiry is calculated from the plan).
5. **Attendance** – choose your member -> *Check In*; the history and this month's count update. Check-in again the same day to see the "already checked in today" message.
6. **Expired check-in** – in Attendance choose **Suresh Babu** (seeded with an expired membership) -> *Check In*. The backend's rejection message is shown in red.
7. **Renewal** – *Memberships* -> *Renew* on an active membership. Example: expiry 15 Oct + Monthly -> 15 Nov (extended from the old expiry, not from today). Renewing Suresh Babu's expired membership restarts from today; afterwards his check-in works.
8. **Expiring Soon** – lists memberships expiring within 7 days (Karthik Raj and Divya Lakshmi in the seed data), each with a *Renew* button.
9. **Errors** – stop the backend and click Refresh: a clear "Cannot reach the backend" message appears.
