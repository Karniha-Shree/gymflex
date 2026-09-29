/* GymFlex frontend - plain JavaScript, no build tools. */
(function () {
  "use strict";

  // ======================= small helpers =======================
  const $ = (sel, root) => (root || document).querySelector(sel);
  const $$ = (sel, root) => Array.from((root || document).querySelectorAll(sel));

  /** Escape text before putting it into HTML (prevents HTML injection). */
  function esc(value) {
    return String(value === null || value === undefined ? "" : value).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }

  function todayStr() {
    const d = new Date();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    return d.getFullYear() + "-" + mm + "-" + dd;
  }

  /** "2026-09-28" -> "28 Sep 2026" */
  function fmtDate(iso) {
    if (!iso) return "-";
    const p = String(iso).split("-");
    if (p.length !== 3) return iso;
    const d = new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
    return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
  }

  function fmtTime(isoDateTime) {
    if (!isoDateTime) return "-";
    return new Date(isoDateTime).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });
  }

  function fmtPrice(n) {
    return "\u20B9" + Number(n).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function statusBadge(status) {
    return '<span class="badge ' + esc(status) + '">' + esc(status) + "</span>";
  }

  function daysLeftText(days, status) {
    if (status !== "ACTIVE") return "-";
    if (days === 0) return "expires today";
    if (days === 1) return "1 day";
    return days + " days";
  }

  function toast(message, type) {
    const el = document.createElement("div");
    el.className = "toast " + (type || "success");
    el.textContent = message;
    $("#toasts").appendChild(el);
    setTimeout(function () { el.remove(); }, 4500);
  }

  function rowMessage(cols, text, cls) {
    return '<tr><td colspan="' + cols + '" class="' + (cls || "muted") + '">' + esc(text) + "</td></tr>";
  }

  // ======================= form helpers =======================
  function clearErrors(form) {
    $$(".error", form).forEach(function (e) { if (e.tagName === "SMALL") e.textContent = ""; });
    const box = $(".form-error", form);
    if (box) { box.classList.add("hidden"); box.textContent = ""; }
  }

  function setFieldErrors(form, errors) {
    Object.keys(errors).forEach(function (name) {
      const input = form.elements[name];
      if (input && input.closest) {
        const small = input.closest(".field").querySelector("small.error");
        if (small) small.textContent = errors[name];
      }
    });
  }

  function setFormError(form, message) {
    const box = $(".form-error", form);
    if (box) { box.textContent = message; box.classList.remove("hidden"); }
  }

  /**
   * Generic submit handler: validate -> call API -> close dialog -> refresh.
   * validate(form) returns an object {fieldName: message}.
   * action() returns a promise that calls the API.
   */
  async function handleSubmit(form, dialog, validate, action, successMessage, onDone) {
    clearErrors(form);
    const errors = validate(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(form, errors);
      return;
    }
    const btn = $("button[type=submit]", form);
    btn.disabled = true;
    try {
      const result = await action();
      dialog.close();
      toast(typeof successMessage === "function" ? successMessage(result) : successMessage, "success");
      if (onDone) onDone();
    } catch (err) {
      if (err.fieldErrors) setFieldErrors(form, err.fieldErrors);
      setFormError(form, err.message);
    } finally {
      btn.disabled = false;
    }
  }

  // close buttons on every dialog
  $$("dialog").forEach(function (dlg) {
    $$("[data-close]", dlg).forEach(function (b) { b.addEventListener("click", function () { dlg.close(); }); });
  });

  // ======================= navigation =======================
  const loaders = {
    dashboard: loadDashboard,
    members: loadMembers,
    plans: loadPlans,
    memberships: loadMemberships,
    attendance: loadAttendancePage,
    expiring: loadExpiring
  };

  function showSection(name) {
    $$(".section").forEach(function (s) { s.classList.toggle("active", s.id === "section-" + name); });
    $$(".nav-btn").forEach(function (b) { b.classList.toggle("active", b.dataset.section === name); });
    loaders[name]();
  }

  $$(".nav-btn").forEach(function (b) {
    b.addEventListener("click", function () { showSection(b.dataset.section); });
  });

  // ======================= DASHBOARD =======================
  async function loadDashboard() {
    const box = $("#dashboardCards");
    box.innerHTML = '<p class="muted">Loading...</p>';
    try {
      const d = await Api.dashboard();
      const items = [
        ["Total Members", d.totalMembers, "blue"],
        ["Active Memberships", d.activeMemberships, "green"],
        ["Expired Memberships", d.expiredMemberships, "red"],
        ["Expiring Soon (7 days)", d.expiringSoon, "orange"],
        ["Today's Check-ins", d.todayCheckIns, "purple"],
        ["Current Month Attendance", d.currentMonthCheckIns, "teal"]
      ];
      box.innerHTML = items.map(function (i) {
        return '<div class="card ' + i[2] + '"><div class="card-value">' + esc(i[1]) +
               '</div><div class="card-label">' + esc(i[0]) + "</div></div>";
      }).join("");
    } catch (err) {
      box.innerHTML = '<div class="alert error">' + esc(err.message) + "</div>";
    }
  }
  $("#refreshDashboard").addEventListener("click", loadDashboard);

  // ======================= MEMBERS =======================
  let membersCache = [];

  async function loadMembers() {
    const body = $("#membersBody");
    body.innerHTML = rowMessage(7, "Loading...");
    try {
      membersCache = await Api.members($("#memberSearch").value.trim());
      if (membersCache.length === 0) {
        body.innerHTML = rowMessage(7, "No members found.");
        return;
      }
      body.innerHTML = membersCache.map(function (m) {
        return "<tr><td>" + m.id + "</td><td>" + esc(m.name) + "</td><td>" + esc(m.email) + "</td><td>" +
          esc(m.phone) + "</td><td>" + fmtDate(m.dateOfBirth) + "</td><td>" + esc(m.emergencyContact || "-") +
          '</td><td class="actions">' +
          '<button class="btn small" data-edit="' + m.id + '">Edit</button> ' +
          '<button class="btn small danger" data-delete="' + m.id + '">Delete</button></td></tr>';
      }).join("");
    } catch (err) {
      body.innerHTML = rowMessage(7, err.message, "error-text");
    }
  }

  let searchTimer = null;
  $("#memberSearch").addEventListener("input", function () {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(loadMembers, 300);
  });

  const memberDialog = $("#memberDialog");
  const memberForm = $("#memberForm");

  function openMemberDialog(member) {
    memberForm.reset();
    clearErrors(memberForm);
    memberForm.elements.id.value = member ? member.id : "";
    $("#memberDialogTitle").textContent = member ? "Edit Member" : "Add Member";
    if (member) {
      memberForm.elements.name.value = member.name || "";
      memberForm.elements.email.value = member.email || "";
      memberForm.elements.phone.value = member.phone || "";
      memberForm.elements.dateOfBirth.value = member.dateOfBirth || "";
      memberForm.elements.address.value = member.address || "";
      memberForm.elements.emergencyContact.value = member.emergencyContact || "";
    }
    memberDialog.showModal();
  }

  $("#addMemberBtn").addEventListener("click", function () { openMemberDialog(null); });

  $("#membersBody").addEventListener("click", async function (e) {
    const editId = e.target.dataset.edit;
    const delId = e.target.dataset.delete;
    if (editId) {
      openMemberDialog(membersCache.find(function (m) { return String(m.id) === editId; }));
    } else if (delId) {
      const m = membersCache.find(function (x) { return String(x.id) === delId; });
      if (!confirm("Delete " + m.name + "?\nTheir memberships and check-in history will also be deleted.")) return;
      try {
        await Api.deleteMember(delId);
        toast("Member deleted");
        loadMembers();
      } catch (err) {
        toast(err.message, "error");
      }
    }
  });

  function validateMember(f) {
    const err = {};
    const v = function (n) { return f.elements[n].value.trim(); };
    if (!v("name")) err.name = "Name is required";
    if (!v("email")) err.email = "Email is required";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v("email"))) err.email = "Enter a valid email address";
    if (!v("phone")) err.phone = "Phone is required";
    else if (!/^[0-9+\- ]{7,20}$/.test(v("phone"))) err.phone = "Phone must be 7-20 characters (digits, +, -, space)";
    if (v("dateOfBirth") && v("dateOfBirth") >= todayStr()) err.dateOfBirth = "Date of birth must be in the past";
    return err;
  }

  memberForm.addEventListener("submit", function (e) {
    e.preventDefault();
    const f = memberForm;
    const id = f.elements.id.value;
    const payload = {
      name: f.elements.name.value.trim(),
      email: f.elements.email.value.trim(),
      phone: f.elements.phone.value.trim(),
      dateOfBirth: f.elements.dateOfBirth.value || null,   // empty date must be null, not ""
      address: f.elements.address.value.trim(),
      emergencyContact: f.elements.emergencyContact.value.trim()
    };
    handleSubmit(f, memberDialog, validateMember,
      function () { return id ? Api.updateMember(id, payload) : Api.createMember(payload); },
      id ? "Member updated" : "Member added",
      loadMembers);
  });

  // ======================= PLANS =======================
  let plansCache = [];

  async function loadPlans() {
    const body = $("#plansBody");
    body.innerHTML = rowMessage(7, "Loading...");
    try {
      plansCache = await Api.plans();
      if (plansCache.length === 0) {
        body.innerHTML = rowMessage(7, "No plans yet. Add one!");
        return;
      }
      body.innerHTML = plansCache.map(function (p) {
        return "<tr><td>" + p.id + "</td><td>" + esc(p.name) + "</td><td>" + esc(p.description || "-") +
          "</td><td>" + p.durationMonths + " month" + (p.durationMonths > 1 ? "s" : "") + "</td><td>" +
          fmtPrice(p.price) + "</td><td>" +
          (p.active ? '<span class="badge ACTIVE">Yes</span>' : '<span class="badge CANCELLED">No</span>') +
          '</td><td class="actions"><button class="btn small" data-edit="' + p.id + '">Edit</button></td></tr>';
      }).join("");
    } catch (err) {
      body.innerHTML = rowMessage(7, err.message, "error-text");
    }
  }

  const planDialog = $("#planDialog");
  const planForm = $("#planForm");

  function openPlanDialog(plan) {
    planForm.reset();
    clearErrors(planForm);
    planForm.elements.id.value = plan ? plan.id : "";
    $("#planDialogTitle").textContent = plan ? "Edit Plan" : "Add Plan";
    planForm.elements.active.checked = plan ? plan.active : true;
    if (plan) {
      planForm.elements.name.value = plan.name;
      planForm.elements.description.value = plan.description || "";
      planForm.elements.durationMonths.value = plan.durationMonths;
      planForm.elements.price.value = plan.price;
    }
    planDialog.showModal();
  }

  $("#addPlanBtn").addEventListener("click", function () { openPlanDialog(null); });
  $("#plansBody").addEventListener("click", function (e) {
    if (e.target.dataset.edit) {
      openPlanDialog(plansCache.find(function (p) { return String(p.id) === e.target.dataset.edit; }));
    }
  });

  function validatePlan(f) {
    const err = {};
    const dur = Number(f.elements.durationMonths.value);
    const price = Number(f.elements.price.value);
    if (!f.elements.name.value.trim()) err.name = "Plan name is required";
    if (!f.elements.durationMonths.value) err.durationMonths = "Duration is required";
    else if (!Number.isInteger(dur) || dur < 1 || dur > 120) err.durationMonths = "Duration must be a whole number from 1 to 120";
    if (!f.elements.price.value) err.price = "Price is required";
    else if (!(price > 0)) err.price = "Price must be greater than 0";
    return err;
  }

  planForm.addEventListener("submit", function (e) {
    e.preventDefault();
    const f = planForm;
    const id = f.elements.id.value;
    const payload = {
      name: f.elements.name.value.trim(),
      description: f.elements.description.value.trim(),
      durationMonths: Number(f.elements.durationMonths.value),
      price: Number(f.elements.price.value),
      active: f.elements.active.checked
    };
    handleSubmit(f, planDialog, validatePlan,
      function () { return id ? Api.updatePlan(id, payload) : Api.createPlan(payload); },
      id ? "Plan updated" : "Plan added",
      loadPlans);
  });

  // ======================= MEMBERSHIPS =======================
  let membershipsCache = [];

  async function loadMemberships() {
    const body = $("#membershipsBody");
    body.innerHTML = rowMessage(8, "Loading...");
    try {
      membershipsCache = await Api.memberships();
      renderMemberships();
    } catch (err) {
      body.innerHTML = rowMessage(8, err.message, "error-text");
    }
  }

  function renderMemberships() {
    const body = $("#membershipsBody");
    const filter = $("#membershipFilter").value;
    const rows = membershipsCache.filter(function (m) { return !filter || m.status === filter; });
    if (rows.length === 0) {
      body.innerHTML = rowMessage(8, "No memberships found.");
      return;
    }
    body.innerHTML = rows.map(function (m) {
      const renewBtn = m.status === "CANCELLED"
        ? '<span class="muted">-</span>'
        : '<button class="btn small" data-renew="' + m.id + '">Renew</button>';
      return "<tr><td>" + m.id + "</td><td>" + esc(m.memberName) + "</td><td>" + esc(m.planName) + "</td><td>" +
        fmtDate(m.startDate) + "</td><td>" + fmtDate(m.expiryDate) + "</td><td>" + statusBadge(m.status) +
        "</td><td>" + daysLeftText(m.daysRemaining, m.status) + '</td><td class="actions">' + renewBtn + "</td></tr>";
    }).join("");
  }

  $("#membershipFilter").addEventListener("change", renderMemberships);

  function fillSelect(select, items, labelFn, placeholder) {
    select.innerHTML = '<option value="">' + esc(placeholder) + "</option>" +
      items.map(function (i) { return '<option value="' + i.id + '">' + esc(labelFn(i)) + "</option>"; }).join("");
  }

  const membershipDialog = $("#membershipDialog");
  const membershipForm = $("#membershipForm");

  $("#addMembershipBtn").addEventListener("click", async function () {
    membershipForm.reset();
    clearErrors(membershipForm);
    try {
      const results = await Promise.all([Api.members(""), Api.plans()]);
      fillSelect(membershipForm.elements.memberId, results[0],
        function (m) { return m.name + " (" + m.email + ")"; }, "-- Select member --");
      fillSelect(membershipForm.elements.planId,
        results[1].filter(function (p) { return p.active; }),
        function (p) { return p.name + " - " + p.durationMonths + " mo - " + fmtPrice(p.price); }, "-- Select plan --");
      membershipForm.elements.startDate.value = todayStr();
      membershipDialog.showModal();
    } catch (err) {
      toast(err.message, "error");
    }
  });

  function validateMembership(f) {
    const err = {};
    if (!f.elements.memberId.value) err.memberId = "Select a member";
    if (!f.elements.planId.value) err.planId = "Select a plan";
    if (!f.elements.startDate.value) err.startDate = "Start date is required";
    if (f.elements.expiryDate.value && f.elements.startDate.value &&
        f.elements.expiryDate.value < f.elements.startDate.value) {
      err.expiryDate = "Expiry date cannot be before the start date";
    }
    return err;
  }

  membershipForm.addEventListener("submit", function (e) {
    e.preventDefault();
    const f = membershipForm;
    const payload = {
      memberId: Number(f.elements.memberId.value),
      planId: Number(f.elements.planId.value),
      startDate: f.elements.startDate.value,
      expiryDate: f.elements.expiryDate.value || null
    };
    handleSubmit(f, membershipDialog, validateMembership,
      function () { return Api.createMembership(payload); },
      function (r) { return "Membership created - expires " + fmtDate(r.expiryDate); },
      loadMemberships);
  });

  // ---- renew ----
  const renewDialog = $("#renewDialog");
  const renewForm = $("#renewForm");
  let afterRenewReload = loadMemberships;

  async function openRenewDialog(membership, reloadFn) {
    afterRenewReload = reloadFn || loadMemberships;
    clearErrors(renewForm);
    try {
      const plans = await Api.plans();
      fillSelect(renewForm.elements.planId, plans.filter(function (p) { return p.active; }),
        function (p) { return p.name + " - " + p.durationMonths + " mo - " + fmtPrice(p.price); }, "-- Select plan --");
      renewForm.elements.planId.value = String(membership.planId);
      renewForm.elements.id.value = membership.id;
      $("#renewInfo").textContent = membership.memberName + " - current expiry " + fmtDate(membership.expiryDate) +
        " (" + membership.status + ")";
      renewDialog.showModal();
    } catch (err) {
      toast(err.message, "error");
    }
  }

  $("#membershipsBody").addEventListener("click", function (e) {
    if (e.target.dataset.renew) {
      openRenewDialog(membershipsCache.find(function (m) { return String(m.id) === e.target.dataset.renew; }), loadMemberships);
    }
  });

  renewForm.addEventListener("submit", function (e) {
    e.preventDefault();
    const f = renewForm;
    const id = f.elements.id.value;
    handleSubmit(f, renewDialog,
      function () { return f.elements.planId.value ? {} : { planId: "Select a plan" }; },
      function () { return Api.renewMembership(id, { planId: Number(f.elements.planId.value) }); },
      function (r) { return "Renewed - new expiry date is " + fmtDate(r.expiryDate); },
      function () { afterRenewReload(); });
  });

  // ======================= ATTENDANCE =======================
  async function loadAttendancePage() {
    const select = $("#attMember");
    const previous = select.value;
    try {
      const members = await Api.members("");
      fillSelect(select, members, function (m) { return m.name + " (" + m.phone + ")"; }, "-- Select member --");
      if (previous) select.value = previous;
      await loadMemberAttendance();
    } catch (err) {
      showAttMessage(err.message, "error");
    }
  }

  function showAttMessage(text, type) {
    const box = $("#attMessage");
    if (!text) { box.classList.add("hidden"); return; }
    box.textContent = text;
    box.className = "alert " + type;
  }

  async function loadMemberAttendance() {
    const id = $("#attMember").value;
    const body = $("#attBody");
    const countBox = $("#attCount");
    if (!id) {
      body.innerHTML = rowMessage(4, "Select a member to see attendance.");
      countBox.classList.add("hidden");
      return;
    }
    body.innerHTML = rowMessage(4, "Loading...");
    try {
      const results = await Promise.all([Api.memberCheckIns(id), Api.currentMonthCount(id)]);
      const list = results[0];
      const c = results[1];
      const monthName = new Date(c.year, c.month - 1, 1).toLocaleDateString("en-IN", { month: "long", year: "numeric" });
      countBox.innerHTML = "<strong>" + esc(c.checkIns) + "</strong> check-in" + (c.checkIns === 1 ? "" : "s") +
        " in " + esc(monthName) + " for " + esc(c.memberName);
      countBox.classList.remove("hidden");
      if (list.length === 0) {
        body.innerHTML = rowMessage(4, "No check-ins yet.");
        return;
      }
      body.innerHTML = list.map(function (ci, i) {
        return "<tr><td>" + (list.length - i) + "</td><td>" + esc(ci.memberName) + "</td><td>" +
          fmtDate(ci.checkInDate) + "</td><td>" + fmtTime(ci.checkInTime) + "</td></tr>";
      }).join("");
    } catch (err) {
      body.innerHTML = rowMessage(4, err.message, "error-text");
    }
  }

  $("#attMember").addEventListener("change", function () {
    showAttMessage("");
    loadMemberAttendance();
  });

  $("#checkInBtn").addEventListener("click", async function () {
    const id = $("#attMember").value;
    if (!id) {
      showAttMessage("Please select a member first.", "error");
      return;
    }
    const btn = $("#checkInBtn");
    btn.disabled = true;
    try {
      const r = await Api.checkIn(id);
      showAttMessage("Checked in: " + r.memberName + " at " + fmtTime(r.checkInTime) + " on " + fmtDate(r.checkInDate), "success");
      toast("Check-in recorded");
      await loadMemberAttendance();
    } catch (err) {
      // e.g. "Check-in rejected: membership expired on 2026-09-20. Please renew the membership."
      showAttMessage(err.message, "error");
    } finally {
      btn.disabled = false;
    }
  });

  // ======================= EXPIRING SOON =======================
  let expiringCache = [];

  async function loadExpiring() {
    const body = $("#expiringBody");
    body.innerHTML = rowMessage(7, "Loading...");
    try {
      expiringCache = await Api.expiringSoon();
      if (expiringCache.length === 0) {
        body.innerHTML = rowMessage(7, "No memberships expire in the next 7 days.");
        return;
      }
      body.innerHTML = expiringCache.map(function (m) {
        return "<tr><td>" + esc(m.memberName) + "</td><td>" + esc(m.planName) + "</td><td>" + fmtDate(m.startDate) +
          "</td><td>" + fmtDate(m.expiryDate) + "</td><td>" + statusBadge(m.status) + "</td><td>" +
          daysLeftText(m.daysRemaining, m.status) +
          '</td><td class="actions"><button class="btn small" data-renew="' + m.id + '">Renew</button></td></tr>';
      }).join("");
    } catch (err) {
      body.innerHTML = rowMessage(7, err.message, "error-text");
    }
  }

  $("#refreshExpiring").addEventListener("click", loadExpiring);
  $("#expiringBody").addEventListener("click", function (e) {
    if (e.target.dataset.renew) {
      openRenewDialog(expiringCache.find(function (m) { return String(m.id) === e.target.dataset.renew; }), loadExpiring);
    }
  });

  // ======================= start =======================
  $("#apiUrlLabel").textContent = API_BASE_URL;
  loadDashboard();
})();
