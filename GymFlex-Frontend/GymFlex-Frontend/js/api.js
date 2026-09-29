/*
 * All communication with the Spring Boot backend goes through this file.
 * Every request uses API_BASE_URL from config.js.
 */

class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message);
    this.name = "ApiError";
    this.status = status;            // HTTP status, or 0 for network errors
    this.fieldErrors = fieldErrors || null;
  }
}

async function apiRequest(path, method = "GET", body = undefined) {
  const options = { method: method, headers: {} };
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(API_BASE_URL + path, options);
  } catch (networkError) {
    // Backend not running, wrong URL, or blocked by CORS
    throw new ApiError(
      "Cannot reach the backend at " + API_BASE_URL +
      ". Make sure the Spring Boot app is running and the frontend is opened via Live Server (see README).",
      0
    );
  }

  if (response.status === 204) {
    return null;                     // e.g. DELETE - no content
  }

  let data = null;
  const text = await response.text();
  if (text) {
    try { data = JSON.parse(text); } catch (e) { data = null; }
  }

  if (!response.ok) {
    const message = (data && data.message) ||
      (response.status >= 500 ? "Server error. Please try again." : "Request failed (" + response.status + ")");
    throw new ApiError(message, response.status, data && data.fieldErrors);
  }
  return data;
}

const Api = {
  // Dashboard
  dashboard: () => apiRequest("/dashboard"),

  // Members
  members: (search) => apiRequest("/members" + (search ? "?search=" + encodeURIComponent(search) : "")),
  createMember: (m) => apiRequest("/members", "POST", m),
  updateMember: (id, m) => apiRequest("/members/" + id, "PUT", m),
  deleteMember: (id) => apiRequest("/members/" + id, "DELETE"),

  // Plans
  plans: () => apiRequest("/plans"),
  createPlan: (p) => apiRequest("/plans", "POST", p),
  updatePlan: (id, p) => apiRequest("/plans/" + id, "PUT", p),

  // Memberships
  memberships: () => apiRequest("/memberships"),
  createMembership: (m) => apiRequest("/memberships", "POST", m),
  renewMembership: (id, body) => apiRequest("/memberships/" + id + "/renew", "PUT", body),
  expiringSoon: () => apiRequest("/memberships/expiring-soon"),

  // Check-ins
  checkIn: (memberId) => apiRequest("/checkins", "POST", { memberId: memberId }),
  memberCheckIns: (memberId) => apiRequest("/checkins/member/" + memberId),
  currentMonthCount: (memberId) => apiRequest("/checkins/member/" + memberId + "/current-month")
};
