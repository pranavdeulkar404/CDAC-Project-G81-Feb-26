import { useEffect, useMemo, useState } from "react";
import { api } from "./api.js";

const resources = [
  "users",
  "profiles",
  "projects",
  "tasks",
  "bugs",
  "comments",
  "notifications",
];

const navigation = [
  { id: "dashboard", label: "Dashboard", icon: "▦" },
  { id: "users", label: "Users", icon: "U" },
  { id: "profiles", label: "Profiles", icon: "P" },
  { id: "projects", label: "Projects", icon: "◆" },
  { id: "tasks", label: "Tasks", icon: "✓" },
  { id: "bugs", label: "Bugs", icon: "!" },
  { id: "comments", label: "Comments", icon: "C" },
  { id: "notifications", label: "Notifications", icon: "N" },
];

const emptyData = Object.fromEntries(
  resources.map((resource) => [resource, []]),
);

function toLocalDateTime(value) {
  return value ? String(value).slice(0, 16) : "";
}

function option(value, label) {
  return {
    value: String(value),
    label,
  };
}

function getConfig(resource, data) {
  const userOptions = data.users.map((item) =>
    option(item.id, `${item.name} (${item.email})`),
  );

  const projectOptions = data.projects.map((item) =>
    option(item.id, item.title),
  );

  const taskOptions = data.tasks.map((item) => option(item.id, item.title));

  const bugOptions = data.bugs.map((item) => option(item.id, item.title));

  const configs = {
    users: {
      title: "Users",
      singular: "User",
      subtitle:
        "Manage team members who participate in projects, tasks and bug tracking.",

      defaults: {
        name: "",
        email: "",
        password: "",
        role: "DEVELOPER",
        otpCode: "",
        otpExpiry: "",
        otpVerified: false,
      },

      fields: [
        {
          name: "name",
          label: "Name",
          required: true,
        },
        {
          name: "email",
          label: "Email",
          type: "email",
          required: true,
        },
        {
          name: "password",
          label: "Password",
          type: "password",
          required: true,
        },
        {
          name: "role",
          label: "Role",
          type: "select",
          required: true,
          options: ["ADMIN", "MANAGER", "DEVELOPER", "TESTER"].map((value) =>
            option(value, value),
          ),
        },
        {
          name: "otpCode",
          label: "OTP Code",
        },
        {
          name: "otpExpiry",
          label: "OTP Expiry",
          type: "datetime-local",
        },
        {
          name: "otpVerified",
          label: "OTP Verified",
          type: "checkbox",
        },
      ],

      columns: [
        ["name", "Name"],
        ["email", "Email"],
        ["role", "Role"],
        [
          "otpVerified",
          "OTP Verified",
          (row) => (row.otpVerified ? "Yes" : "No"),
        ],
      ],

      prepare: (form) => ({
        ...form,
        otpExpiry: form.otpExpiry || null,
        otpVerified: Boolean(form.otpVerified),
      }),

      edit: (row) => ({
        ...row,
        otpExpiry: toLocalDateTime(row.otpExpiry),
      }),
    },

    profiles: {
      title: "Profiles",
      singular: "Profile",
      subtitle: "Store contact and professional details for each team member.",

      prerequisite:
        data.users.length === 0
          ? "Create at least one user before creating a profile."
          : "",

      defaults: {
        userId: "",
        phone: "",
        designation: "",
        bio: "",
      },

      fields: [
        {
          name: "userId",
          label: "User",
          type: "select",
          required: true,
          options: userOptions,
        },
        {
          name: "phone",
          label: "Phone",
        },
        {
          name: "designation",
          label: "Designation",
        },
        {
          name: "bio",
          label: "Bio",
          type: "textarea",
          wide: true,
        },
      ],

      columns: [
        ["userName", "User"],
        ["phone", "Phone"],
        ["designation", "Designation"],
        ["bio", "Bio"],
      ],

      prepare: (form) => ({
        ...form,
        userId: Number(form.userId),
      }),
    },

    projects: {
      title: "Projects",
      singular: "Project",
      subtitle:
        "Create projects and select the team member responsible for creating them.",

      prerequisite:
        data.users.length === 0
          ? "Create at least one user before creating a project."
          : "",

      defaults: {
        title: "",
        description: "",
        startDate: "",
        status: "PLANNED",
        createdById: "",
      },

      fields: [
        {
          name: "title",
          label: "Title",
          required: true,
        },
        {
          name: "startDate",
          label: "Start Date",
          type: "date",
        },
        {
          name: "status",
          label: "Status",
          type: "select",
          required: true,
          options: ["PLANNED", "ACTIVE", "ON_HOLD", "COMPLETED"].map((value) =>
            option(value, value),
          ),
        },
        {
          name: "createdById",
          label: "Created By",
          type: "select",
          required: true,
          options: userOptions,
        },
        {
          name: "description",
          label: "Description",
          type: "textarea",
          wide: true,
        },
      ],

      columns: [
        ["title", "Title"],
        ["status", "Status"],
        ["startDate", "Start Date"],
        ["createdByName", "Created By"],
      ],

      prepare: (form) => ({
        ...form,
        startDate: form.startDate || null,
        createdById: Number(form.createdById),
      }),
    },

    tasks: {
      title: "Tasks",
      singular: "Task",
      subtitle:
        "Create project tasks, set priorities and assign work to team members.",

      prerequisite:
        data.users.length === 0 || data.projects.length === 0
          ? "Create a user and a project before creating a task."
          : "",

      defaults: {
        title: "",
        description: "",
        priority: "MEDIUM",
        status: "TODO",
        dueDate: "",
        projectId: "",
        assignedToId: "",
      },

      fields: [
        {
          name: "title",
          label: "Title",
          required: true,
        },
        {
          name: "priority",
          label: "Priority",
          type: "select",
          required: true,
          options: ["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((value) =>
            option(value, value),
          ),
        },
        {
          name: "status",
          label: "Status",
          type: "select",
          required: true,
          options: ["TODO", "IN_PROGRESS", "BLOCKED", "DONE"].map((value) =>
            option(value, value),
          ),
        },
        {
          name: "dueDate",
          label: "Due Date",
          type: "date",
        },
        {
          name: "projectId",
          label: "Project",
          type: "select",
          required: true,
          options: projectOptions,
        },
        {
          name: "assignedToId",
          label: "Assigned To",
          type: "select",
          required: true,
          options: userOptions,
        },
        {
          name: "description",
          label: "Description",
          type: "textarea",
          wide: true,
        },
      ],

      columns: [
        ["title", "Title"],
        ["projectTitle", "Project"],
        ["assignedToName", "Assignee"],
        ["priority", "Priority"],
        ["status", "Status"],
        ["dueDate", "Due Date"],
      ],

      prepare: (form) => ({
        ...form,
        dueDate: form.dueDate || null,
        projectId: Number(form.projectId),
        assignedToId: Number(form.assignedToId),
      }),
    },

    bugs: {
      title: "Bugs",
      singular: "Bug",
      subtitle:
        "Report project issues, define severity and assign them for resolution.",

      prerequisite:
        data.users.length === 0 || data.projects.length === 0
          ? "Create a user and a project before creating a bug."
          : "",

      defaults: {
        title: "",
        description: "",
        severity: "MEDIUM",
        status: "OPEN",
        projectId: "",
        assignedToId: "",
      },

      fields: [
        {
          name: "title",
          label: "Title",
          required: true,
        },
        {
          name: "severity",
          label: "Severity",
          type: "select",
          required: true,
          options: ["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((value) =>
            option(value, value),
          ),
        },
        {
          name: "status",
          label: "Status",
          type: "select",
          required: true,
          options: ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"].map((value) =>
            option(value, value),
          ),
        },
        {
          name: "projectId",
          label: "Project",
          type: "select",
          required: true,
          options: projectOptions,
        },
        {
          name: "assignedToId",
          label: "Assigned To",
          type: "select",
          required: true,
          options: userOptions,
        },
        {
          name: "description",
          label: "Description",
          type: "textarea",
          wide: true,
        },
      ],

      columns: [
        ["title", "Title"],
        ["projectTitle", "Project"],
        ["assignedToName", "Assignee"],
        ["severity", "Severity"],
        ["status", "Status"],
      ],

      prepare: (form) => ({
        ...form,
        projectId: Number(form.projectId),
        assignedToId: Number(form.assignedToId),
      }),
    },

    comments: {
      title: "Comments",
      singular: "Comment",
      subtitle: "Add team discussions and updates to tasks or reported bugs.",

      prerequisite:
        data.users.length === 0 ||
        (data.tasks.length === 0 && data.bugs.length === 0)
          ? "Create a user and at least one task or bug before adding a comment."
          : "",

      defaults: {
        message: "",
        createdAt: "",
        userId: "",
        targetType: data.tasks.length ? "TASK" : "BUG",
        targetId: "",
      },

      fields: [
        {
          name: "userId",
          label: "Written By",
          type: "select",
          required: true,
          options: userOptions,
        },
        {
          name: "targetType",
          label: "Target Type",
          type: "select",
          required: true,
          options: [option("TASK", "Task"), option("BUG", "Bug")],
        },
        {
          name: "targetId",
          label: "Target",
          type: "dynamic-target",
          required: true,
          taskOptions,
          bugOptions,
        },
        {
          name: "createdAt",
          label: "Created At",
          type: "datetime-local",
        },
        {
          name: "message",
          label: "Message",
          type: "textarea",
          required: true,
          wide: true,
        },
      ],

      columns: [
        ["message", "Message"],
        ["userName", "Written By"],
        ["targetType", "Type"],
        ["targetTitle", "Target"],
        [
          "createdAt",
          "Created At",
          (row) => row.createdAt?.replace("T", " ").slice(0, 16),
        ],
      ],

      prepare: (form) => ({
        message: form.message,
        createdAt: form.createdAt || null,
        userId: Number(form.userId),
        taskId: form.targetType === "TASK" ? Number(form.targetId) : null,
        bugId: form.targetType === "BUG" ? Number(form.targetId) : null,
      }),

      edit: (row) => ({
        message: row.message,
        createdAt: toLocalDateTime(row.createdAt),
        userId: String(row.userId),
        targetType: row.targetType,
        targetId: String(row.targetId),
      }),
    },

    notifications: {
      title: "Notifications",
      singular: "Notification",
      subtitle: "Create and manage important notifications for team members.",

      prerequisite:
        data.users.length === 0
          ? "Create at least one user before creating a notification."
          : "",

      defaults: {
        message: "",
        type: "GENERAL",
        createdAt: "",
        userId: "",
      },

      fields: [
        {
          name: "userId",
          label: "User",
          type: "select",
          required: true,
          options: userOptions,
        },
        {
          name: "type",
          label: "Type",
          type: "select",
          required: true,
          options: [
            "GENERAL",
            "TASK_ASSIGNED",
            "TASK_UPDATED",
            "BUG_ASSIGNED",
            "BUG_UPDATED",
          ].map((value) => option(value, value)),
        },
        {
          name: "createdAt",
          label: "Created At",
          type: "datetime-local",
        },
        {
          name: "message",
          label: "Message",
          type: "textarea",
          required: true,
          wide: true,
        },
      ],

      columns: [
        ["message", "Message"],
        ["type", "Type"],
        ["userName", "User"],
        [
          "createdAt",
          "Created At",
          (row) => row.createdAt?.replace("T", " ").slice(0, 16),
        ],
      ],

      prepare: (form) => ({
        ...form,
        createdAt: form.createdAt || null,
        userId: Number(form.userId),
      }),

      edit: (row) => ({
        ...row,
        userId: String(row.userId),
        createdAt: toLocalDateTime(row.createdAt),
      }),
    },
  };

  return configs[resource];
}

function Dashboard({ data, onOpen }) {
  const cards = [
    ["projects", "Projects", "◆"],
    ["tasks", "Tasks", "✓"],
    ["bugs", "Bugs", "!"],
    ["users", "Users", "U"],
    ["comments", "Comments", "C"],
    ["notifications", "Notifications", "N"],
  ];

  const openTasks = data.tasks.filter((item) => item.status !== "DONE").length;

  const openBugs = data.bugs.filter(
    (item) => !["RESOLVED", "CLOSED"].includes(item.status),
  ).length;

  const setupSteps = [
    "Add Team Members",
    "Create Projects",
    "Assign Tasks & Bugs",
    "Collaborate & Track",
  ];

  return (
    <div className="page-stack">
      <section className="hero-panel">
        <div>
          <span className="eyebrow">PROJECT MANAGEMENT</span>

          <h1>Welcome to SprintFlow</h1>

          <p>
            Manage projects, assign tasks, report bugs and coordinate your team
            from one workspace.
          </p>
        </div>

        <div className="hero-metrics">
          <div>
            <strong>{openTasks}</strong>
            <span>Open tasks</span>
          </div>

          <div>
            <strong>{openBugs}</strong>
            <span>Open bugs</span>
          </div>
        </div>
      </section>

      <section className="metric-grid">
        {cards.map(([id, label, icon]) => (
          <button className="metric-card" key={id} onClick={() => onOpen(id)}>
            <span className="metric-icon">{icon}</span>

            <span>
              <strong>{data[id].length}</strong>
              <small>{label}</small>
            </span>
          </button>
        ))}
      </section>

      <section className="panel getting-started">
        <div>
          <span className="eyebrow">GETTING STARTED</span>

          <h2>Set up your project workspace</h2>
        </div>

        <div className="steps">
          {setupSteps.map((step, index) => (
            <div className="step" key={step}>
              <span>{index + 1}</span>
              <strong>{step}</strong>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function Field({ field, value, form, onChange }) {
  const common = {
    id: field.name,
    name: field.name,
    value: value ?? "",
    required: field.required,
    onChange: (event) => onChange(field.name, event.target.value),
  };

  if (field.type === "checkbox") {
    return (
      <>
        <input
          type="checkbox"
          checked={Boolean(value)}
          onChange={(event) => onChange(field.name, event.target.checked)}
        />

        <span>{field.label}</span>
      </>
    );
  }

  if (field.type === "textarea") {
    return (
      <textarea
        {...common}
        rows="4"
        placeholder={`Enter ${field.label.toLowerCase()}`}
      />
    );
  }

  if (field.type === "select") {
    return (
      <select {...common}>
        <option value="">Select {field.label}</option>

        {field.options.map((item) => (
          <option key={item.value} value={item.value}>
            {item.label}
          </option>
        ))}
      </select>
    );
  }

  if (field.type === "dynamic-target") {
    const choices =
      form.targetType === "BUG" ? field.bugOptions : field.taskOptions;

    return (
      <select {...common}>
        <option value="">
          Select {form.targetType === "BUG" ? "Bug" : "Task"}
        </option>

        {choices.map((item) => (
          <option key={item.value} value={item.value}>
            {item.label}
          </option>
        ))}
      </select>
    );
  }

  return (
    <input
      {...common}
      type={field.type || "text"}
      placeholder={`Enter ${field.label.toLowerCase()}`}
    />
  );
}

function CrudPage({ resource, rows, config, onSave, onDelete, busy }) {
  const [form, setForm] = useState(config.defaults);

  const [editingId, setEditingId] = useState(null);

  const [showForm, setShowForm] = useState(false);

  useEffect(() => {
    setForm(config.defaults);
    setEditingId(null);
    setShowForm(false);
  }, [resource]);

  function change(name, value) {
    setForm((current) => {
      const next = {
        ...current,
        [name]: value,
      };

      if (name === "targetType") {
        next.targetId = "";
      }

      return next;
    });
  }

  function startCreate() {
    setEditingId(null);
    setForm(config.defaults);
    setShowForm(true);
  }

  function startEdit(row) {
    setEditingId(row.id);

    const prepared = config.edit ? config.edit(row) : { ...row };

    const picked = Object.fromEntries(
      Object.keys(config.defaults).map((key) => [
        key,
        prepared[key] ?? config.defaults[key],
      ]),
    );

    setForm(picked);
    setShowForm(true);
  }

  function cancel() {
    setEditingId(null);
    setForm(config.defaults);
    setShowForm(false);
  }

  async function submit(event) {
    event.preventDefault();

    const payload = config.prepare ? config.prepare(form) : form;

    try {
      await onSave(resource, editingId, payload);

      cancel();
    } catch (error) {
      console.error(error);
    }
  }

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div>
          <span className="eyebrow">SPRINTFLOW WORKSPACE</span>

          <h1>{config.title}</h1>

          <p>{config.subtitle}</p>
        </div>

        <button
          className="primary-button"
          onClick={startCreate}
          disabled={Boolean(config.prerequisite)}
        >
          + Add {config.singular}
        </button>
      </section>

      {config.prerequisite && (
        <div className="notice">{config.prerequisite}</div>
      )}

      {showForm && (
        <section className="panel form-panel">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">
                {editingId ? "UPDATE DETAILS" : "ADD DETAILS"}
              </span>

              <h2>
                {editingId
                  ? `Edit ${config.singular}`
                  : `Add ${config.singular}`}
              </h2>
            </div>

            <button className="icon-button" onClick={cancel} type="button">
              ×
            </button>
          </div>

          <form onSubmit={submit} className="entity-form">
            {config.fields.map((field) => (
              <label
                className={`${field.wide ? "wide-field " : ""}${
                  field.type === "checkbox" ? "checkbox-field" : ""
                }`}
                key={field.name}
              >
                {field.type !== "checkbox" && (
                  <span>
                    {field.label}
                    {field.required ? " *" : ""}
                  </span>
                )}

                <Field
                  field={field}
                  value={form[field.name]}
                  form={form}
                  onChange={change}
                />
              </label>
            ))}

            <div className="form-actions wide-field">
              <button
                type="button"
                className="secondary-button"
                onClick={cancel}
              >
                Cancel
              </button>

              <button type="submit" className="primary-button" disabled={busy}>
                {busy ? "Saving…" : editingId ? "Update" : "Create"}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="panel table-panel">
        <div className="panel-heading">
          <div>
            <h2>All {config.title}</h2>

            <p>
              {rows.length} {rows.length === 1 ? "record" : "records"}
            </p>
          </div>
        </div>

        {rows.length === 0 ? (
          <div className="empty-state">
            <strong>No records yet</strong>

            <span>
              Use the Add button to create the first{" "}
              {config.singular.toLowerCase()}.
            </span>
          </div>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>ID</th>

                  {config.columns.map((column) => (
                    <th key={column[0]}>{column[1]}</th>
                  ))}

                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td>#{row.id}</td>

                    {config.columns.map(([key, , render]) => (
                      <td key={key}>
                        {render ? render(row) : row[key] || "—"}
                      </td>
                    ))}

                    <td className="action-cell">
                      <button
                        className="text-button"
                        onClick={() => startEdit(row)}
                      >
                        Edit
                      </button>

                      <button
                        className="text-button danger"
                        onClick={() =>
                          onDelete(resource, row.id, config.singular)
                        }
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

export default function App() {
  const [active, setActive] = useState("dashboard");

  const [data, setData] = useState(emptyData);

  const [loading, setLoading] = useState(true);

  const [busy, setBusy] = useState(false);

  const [message, setMessage] = useState(null);

  const config = useMemo(
    () => (active === "dashboard" ? null : getConfig(active, data)),
    [active, data],
  );

  async function loadAll(showLoader = true) {
    if (showLoader) {
      setLoading(true);
    }

    try {
      const values = await Promise.all(
        resources.map((resource) => api.list(resource)),
      );

      setData(
        Object.fromEntries(
          resources.map((resource, index) => [resource, values[index]]),
        ),
      );
    } catch (error) {
      setMessage({
        type: "error",
        text: `${error.message}. Please ensure that the application server is running.`,
      });
    } finally {
      if (showLoader) {
        setLoading(false);
      }
    }
  }

  useEffect(() => {
    loadAll();
  }, []);

  useEffect(() => {
    if (!message) {
      return undefined;
    }

    const timeout = setTimeout(() => setMessage(null), 5000);

    return () => clearTimeout(timeout);
  }, [message]);

  async function save(resource, id, payload) {
    setBusy(true);

    try {
      if (id) {
        await api.update(resource, id, payload);
      } else {
        await api.create(resource, payload);
      }

      await loadAll(false);

      setMessage({
        type: "success",
        text: `${id ? "Updated" : "Created"} successfully.`,
      });
    } catch (error) {
      setMessage({
        type: "error",
        text: error.message,
      });

      throw error;
    } finally {
      setBusy(false);
    }
  }

  async function remove(resource, id, singular) {
    const confirmed = window.confirm(`Delete this ${singular.toLowerCase()}?`);

    if (!confirmed) {
      return;
    }

    setBusy(true);

    try {
      await api.remove(resource, id);
      await loadAll(false);

      setMessage({
        type: "success",
        text: `${singular} deleted successfully.`,
      });
    } catch (error) {
      setMessage({
        type: "error",
        text: error.message,
      });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span>SF</span>

          <div>
            <strong>SprintFlow</strong>
            <small>Agile Project Tracker</small>
          </div>
        </div>

        <nav>
          {navigation.map((item) => (
            <button
              key={item.id}
              className={active === item.id ? "active" : ""}
              onClick={() => setActive(item.id)}
            >
              <span className="nav-icon">{item.icon}</span>

              {item.label}

              {item.id !== "dashboard" && <small>{data[item.id].length}</small>}
            </button>
          ))}
        </nav>

        <div className="sidebar-note">
          <strong>Workspace</strong>
          <span>Manage your projects efficiently</span>
        </div>
      </aside>

      <main>
        <header className="topbar">
          <div>
            <strong>
              {active === "dashboard" ? "Project Overview" : config?.title}
            </strong>

            <span>Plan, assign and track team progress</span>
          </div>

          <button
            className="secondary-button"
            onClick={() => loadAll()}
            disabled={loading}
          >
            Refresh Data
          </button>
        </header>

        <div className="content">
          {message && (
            <div className={`toast ${message.type}`}>{message.text}</div>
          )}

          {loading ? (
            <div className="loading-screen">
              <span className="spinner" />

              <strong>Loading SprintFlow…</strong>
            </div>
          ) : active === "dashboard" ? (
            <Dashboard data={data} onOpen={setActive} />
          ) : (
            <CrudPage
              key={active}
              resource={active}
              rows={data[active]}
              config={config}
              onSave={save}
              onDelete={remove}
              busy={busy}
            />
          )}
        </div>
      </main>
    </div>
  );
}

// import { useEffect, useMemo, useState } from 'react';
// import { api } from './api.js';

// const resources = ['users', 'profiles', 'projects', 'tasks', 'bugs', 'comments', 'notifications'];

// const navigation = [
//   { id: 'dashboard', label: 'Dashboard', icon: '▦' },
//   { id: 'users', label: 'Users', icon: 'U' },
//   { id: 'profiles', label: 'Profiles', icon: 'P' },
//   { id: 'projects', label: 'Projects', icon: '◆' },
//   { id: 'tasks', label: 'Tasks', icon: '✓' },
//   { id: 'bugs', label: 'Bugs', icon: '!' },
//   { id: 'comments', label: 'Comments', icon: 'C' },
//   { id: 'notifications', label: 'Notifications', icon: 'N' },
// ];

// const emptyData = Object.fromEntries(resources.map((resource) => [resource, []]));

// function toLocalDateTime(value) {
//   return value ? String(value).slice(0, 16) : '';
// }

// function option(value, label) {
//   return { value: String(value), label };
// }

// function getConfig(resource, data) {
//   const userOptions = data.users.map((item) => option(item.id, `${item.name} (${item.email})`));
//   const projectOptions = data.projects.map((item) => option(item.id, item.title));
//   const taskOptions = data.tasks.map((item) => option(item.id, item.title));
//   const bugOptions = data.bugs.map((item) => option(item.id, item.title));

//   const configs = {
//     users: {
//       title: 'Users',
//       singular: 'User',
//       subtitle: 'Create the team members used by projects, tasks, bugs, comments and notifications.',
//       defaults: { name: '', email: '', password: '', role: 'DEVELOPER', otpCode: '', otpExpiry: '', otpVerified: false },
//       fields: [
//         { name: 'name', label: 'Name', required: true },
//         { name: 'email', label: 'Email', type: 'email', required: true },
//         { name: 'password', label: 'Password', type: 'password', required: true },
//         { name: 'role', label: 'Role', type: 'select', required: true, options: ['ADMIN', 'MANAGER', 'DEVELOPER', 'TESTER'].map((v) => option(v, v)) },
//         { name: 'otpCode', label: 'OTP Code' },
//         { name: 'otpExpiry', label: 'OTP Expiry', type: 'datetime-local' },
//         { name: 'otpVerified', label: 'OTP Verified', type: 'checkbox' },
//       ],
//       columns: [
//         ['name', 'Name'], ['email', 'Email'], ['role', 'Role'],
//         ['otpVerified', 'OTP Verified', (row) => row.otpVerified ? 'Yes' : 'No'],
//       ],
//       prepare: (form) => ({ ...form, otpExpiry: form.otpExpiry || null, otpVerified: Boolean(form.otpVerified) }),
//       edit: (row) => ({ ...row, otpExpiry: toLocalDateTime(row.otpExpiry) }),
//     },
//     profiles: {
//       title: 'Profiles',
//       singular: 'Profile',
//       subtitle: 'Each user can have one profile.',
//       prerequisite: data.users.length === 0 ? 'Create at least one user before creating a profile.' : '',
//       defaults: { userId: '', phone: '', designation: '', bio: '' },
//       fields: [
//         { name: 'userId', label: 'User', type: 'select', required: true, options: userOptions },
//         { name: 'phone', label: 'Phone' },
//         { name: 'designation', label: 'Designation' },
//         { name: 'bio', label: 'Bio', type: 'textarea', wide: true },
//       ],
//       columns: [['userName', 'User'], ['phone', 'Phone'], ['designation', 'Designation'], ['bio', 'Bio']],
//       prepare: (form) => ({ ...form, userId: Number(form.userId) }),
//     },
//     projects: {
//       title: 'Projects',
//       singular: 'Project',
//       subtitle: 'Create projects and select the user who created each project.',
//       prerequisite: data.users.length === 0 ? 'Create at least one user before creating a project.' : '',
//       defaults: { title: '', description: '', startDate: '', status: 'PLANNED', createdById: '' },
//       fields: [
//         { name: 'title', label: 'Title', required: true },
//         { name: 'startDate', label: 'Start Date', type: 'date' },
//         { name: 'status', label: 'Status', type: 'select', required: true, options: ['PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED'].map((v) => option(v, v)) },
//         { name: 'createdById', label: 'Created By', type: 'select', required: true, options: userOptions },
//         { name: 'description', label: 'Description', type: 'textarea', wide: true },
//       ],
//       columns: [['title', 'Title'], ['status', 'Status'], ['startDate', 'Start Date'], ['createdByName', 'Created By']],
//       prepare: (form) => ({ ...form, startDate: form.startDate || null, createdById: Number(form.createdById) }),
//     },
//     tasks: {
//       title: 'Tasks',
//       singular: 'Task',
//       subtitle: 'Create tasks inside a project and assign them to a user.',
//       prerequisite: data.users.length === 0 || data.projects.length === 0 ? 'Create a user and a project before creating a task.' : '',
//       defaults: { title: '', description: '', priority: 'MEDIUM', status: 'TODO', dueDate: '', projectId: '', assignedToId: '' },
//       fields: [
//         { name: 'title', label: 'Title', required: true },
//         { name: 'priority', label: 'Priority', type: 'select', required: true, options: ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((v) => option(v, v)) },
//         { name: 'status', label: 'Status', type: 'select', required: true, options: ['TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE'].map((v) => option(v, v)) },
//         { name: 'dueDate', label: 'Due Date', type: 'date' },
//         { name: 'projectId', label: 'Project', type: 'select', required: true, options: projectOptions },
//         { name: 'assignedToId', label: 'Assigned To', type: 'select', required: true, options: userOptions },
//         { name: 'description', label: 'Description', type: 'textarea', wide: true },
//       ],
//       columns: [['title', 'Title'], ['projectTitle', 'Project'], ['assignedToName', 'Assignee'], ['priority', 'Priority'], ['status', 'Status'], ['dueDate', 'Due Date']],
//       prepare: (form) => ({ ...form, dueDate: form.dueDate || null, projectId: Number(form.projectId), assignedToId: Number(form.assignedToId) }),
//     },
//     bugs: {
//       title: 'Bugs',
//       singular: 'Bug',
//       subtitle: 'Report bugs for a project and assign them to a user.',
//       prerequisite: data.users.length === 0 || data.projects.length === 0 ? 'Create a user and a project before creating a bug.' : '',
//       defaults: { title: '', description: '', severity: 'MEDIUM', status: 'OPEN', projectId: '', assignedToId: '' },
//       fields: [
//         { name: 'title', label: 'Title', required: true },
//         { name: 'severity', label: 'Severity', type: 'select', required: true, options: ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((v) => option(v, v)) },
//         { name: 'status', label: 'Status', type: 'select', required: true, options: ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((v) => option(v, v)) },
//         { name: 'projectId', label: 'Project', type: 'select', required: true, options: projectOptions },
//         { name: 'assignedToId', label: 'Assigned To', type: 'select', required: true, options: userOptions },
//         { name: 'description', label: 'Description', type: 'textarea', wide: true },
//       ],
//       columns: [['title', 'Title'], ['projectTitle', 'Project'], ['assignedToName', 'Assignee'], ['severity', 'Severity'], ['status', 'Status']],
//       prepare: (form) => ({ ...form, projectId: Number(form.projectId), assignedToId: Number(form.assignedToId) }),
//     },
//     comments: {
//       title: 'Comments',
//       singular: 'Comment',
//       subtitle: 'Write a comment for exactly one task or one bug.',
//       prerequisite: data.users.length === 0 || (data.tasks.length === 0 && data.bugs.length === 0) ? 'Create a user and at least one task or bug before adding a comment.' : '',
//       defaults: { message: '', createdAt: '', userId: '', targetType: data.tasks.length ? 'TASK' : 'BUG', targetId: '' },
//       fields: [
//         { name: 'userId', label: 'Written By', type: 'select', required: true, options: userOptions },
//         { name: 'targetType', label: 'Target Type', type: 'select', required: true, options: [option('TASK', 'Task'), option('BUG', 'Bug')] },
//         { name: 'targetId', label: 'Target', type: 'dynamic-target', required: true, taskOptions, bugOptions },
//         { name: 'createdAt', label: 'Created At', type: 'datetime-local' },
//         { name: 'message', label: 'Message', type: 'textarea', required: true, wide: true },
//       ],
//       columns: [['message', 'Message'], ['userName', 'Written By'], ['targetType', 'Type'], ['targetTitle', 'Target'], ['createdAt', 'Created At', (row) => row.createdAt?.replace('T', ' ').slice(0, 16)]],
//       prepare: (form) => ({
//         message: form.message,
//         createdAt: form.createdAt || null,
//         userId: Number(form.userId),
//         taskId: form.targetType === 'TASK' ? Number(form.targetId) : null,
//         bugId: form.targetType === 'BUG' ? Number(form.targetId) : null,
//       }),
//       edit: (row) => ({
//         message: row.message,
//         createdAt: toLocalDateTime(row.createdAt),
//         userId: String(row.userId),
//         targetType: row.targetType,
//         targetId: String(row.targetId),
//       }),
//     },
//     notifications: {
//       title: 'Notifications',
//       singular: 'Notification',
//       subtitle: 'CRUD-only notification records. No email microservice is included.',
//       prerequisite: data.users.length === 0 ? 'Create at least one user before creating a notification.' : '',
//       defaults: { message: '', type: 'GENERAL', createdAt: '', userId: '' },
//       fields: [
//         { name: 'userId', label: 'User', type: 'select', required: true, options: userOptions },
//         { name: 'type', label: 'Type', type: 'select', required: true, options: ['GENERAL', 'TASK_ASSIGNED', 'TASK_UPDATED', 'BUG_ASSIGNED', 'BUG_UPDATED'].map((v) => option(v, v)) },
//         { name: 'createdAt', label: 'Created At', type: 'datetime-local' },
//         { name: 'message', label: 'Message', type: 'textarea', required: true, wide: true },
//       ],
//       columns: [['message', 'Message'], ['type', 'Type'], ['userName', 'User'], ['createdAt', 'Created At', (row) => row.createdAt?.replace('T', ' ').slice(0, 16)]],
//       prepare: (form) => ({ ...form, createdAt: form.createdAt || null, userId: Number(form.userId) }),
//       edit: (row) => ({ ...row, userId: String(row.userId), createdAt: toLocalDateTime(row.createdAt) }),
//     },
//   };

//   return configs[resource];
// }

// function Dashboard({ data, onOpen }) {
//   const cards = [
//     ['projects', 'Projects', '◆'], ['tasks', 'Tasks', '✓'], ['bugs', 'Bugs', '!'],
//     ['users', 'Users', 'U'], ['comments', 'Comments', 'C'], ['notifications', 'Notifications', 'N'],
//   ];

//   const openTasks = data.tasks.filter((item) => item.status !== 'DONE').length;
//   const openBugs = data.bugs.filter((item) => !['RESOLVED', 'CLOSED'].includes(item.status)).length;

//   return (
//     <div className="page-stack">
//       <section className="hero-panel">
//         <div>
//           <span className="eyebrow">CRUD DEVELOPMENT BUILD</span>
//           <h1>Welcome to SprintFlow</h1>
//           <p>Manage your seven ER-diagram entities without authentication, JWT, OTP verification, or microservices.</p>
//         </div>
//         <div className="hero-metrics">
//           <div><strong>{openTasks}</strong><span>Open tasks</span></div>
//           <div><strong>{openBugs}</strong><span>Open bugs</span></div>
//         </div>
//       </section>

//       <section className="metric-grid">
//         {cards.map(([id, label, icon]) => (
//           <button className="metric-card" key={id} onClick={() => onOpen(id)}>
//             <span className="metric-icon">{icon}</span>
//             <span><strong>{data[id].length}</strong><small>{label}</small></span>
//           </button>
//         ))}
//       </section>

//       <section className="panel getting-started">
//         <div>
//           <span className="eyebrow">RECOMMENDED CREATION ORDER</span>
//           <h2>Build linked data without foreign-key errors</h2>
//         </div>
//         <div className="steps">
//           {['Users', 'Profiles & Projects', 'Tasks & Bugs', 'Comments & Notifications'].map((step, index) => (
//             <div className="step" key={step}><span>{index + 1}</span><strong>{step}</strong></div>
//           ))}
//         </div>
//       </section>
//     </div>
//   );
// }

// function Field({ field, value, form, onChange }) {
//   const common = {
//     id: field.name,
//     name: field.name,
//     value: value ?? '',
//     required: field.required,
//     onChange: (event) => onChange(field.name, event.target.value),
//   };

//   if (field.type === 'checkbox') {
//     return (
//       <>
//         <input
//           type="checkbox"
//           checked={Boolean(value)}
//           onChange={(event) => onChange(field.name, event.target.checked)}
//         />
//         <span>{field.label}</span>
//       </>
//     );
//   }

//   if (field.type === 'textarea') {
//     return <textarea {...common} rows="4" placeholder={`Enter ${field.label.toLowerCase()}`} />;
//   }

//   if (field.type === 'select') {
//     return (
//       <select {...common}>
//         <option value="">Select {field.label}</option>
//         {field.options.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
//       </select>
//     );
//   }

//   if (field.type === 'dynamic-target') {
//     const choices = form.targetType === 'BUG' ? field.bugOptions : field.taskOptions;
//     return (
//       <select {...common}>
//         <option value="">Select {form.targetType === 'BUG' ? 'Bug' : 'Task'}</option>
//         {choices.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
//       </select>
//     );
//   }

//   return <input {...common} type={field.type || 'text'} placeholder={`Enter ${field.label.toLowerCase()}`} />;
// }

// function CrudPage({ resource, rows, config, onSave, onDelete, busy }) {
//   const [form, setForm] = useState(config.defaults);
//   const [editingId, setEditingId] = useState(null);
//   const [showForm, setShowForm] = useState(false);

//   useEffect(() => {
//     setForm(config.defaults);
//     setEditingId(null);
//     setShowForm(false);
//   }, [resource]);

//   function change(name, value) {
//     setForm((current) => {
//       const next = { ...current, [name]: value };
//       if (name === 'targetType') next.targetId = '';
//       return next;
//     });
//   }

//   function startCreate() {
//     setEditingId(null);
//     setForm(config.defaults);
//     setShowForm(true);
//   }

//   function startEdit(row) {
//     setEditingId(row.id);
//     const prepared = config.edit ? config.edit(row) : { ...row };
//     const picked = Object.fromEntries(Object.keys(config.defaults).map((key) => [key, prepared[key] ?? config.defaults[key]]));
//     setForm(picked);
//     setShowForm(true);
//   }

//   function cancel() {
//     setEditingId(null);
//     setForm(config.defaults);
//     setShowForm(false);
//   }

//   async function submit(event) {
//     event.preventDefault();
//     const payload = config.prepare ? config.prepare(form) : form;
//     try {
//       await onSave(resource, editingId, payload);
//       cancel();
//     } catch {
//       // The parent already displays the API error. Keep the form open for correction.
//     }
//   }

//   return (
//     <div className="page-stack">
//       <section className="page-heading">
//         <div>
//           <span className="eyebrow">ENTITY CRUD</span>
//           <h1>{config.title}</h1>
//           <p>{config.subtitle}</p>
//         </div>
//         <button className="primary-button" onClick={startCreate} disabled={Boolean(config.prerequisite)}>+ Add {config.singular}</button>
//       </section>

//       {config.prerequisite && <div className="notice">{config.prerequisite}</div>}

//       {showForm && (
//         <section className="panel form-panel">
//           <div className="panel-heading">
//             <div><span className="eyebrow">{editingId ? 'UPDATE RECORD' : 'NEW RECORD'}</span><h2>{editingId ? `Edit ${config.singular}` : `Add ${config.singular}`}</h2></div>
//             <button className="icon-button" onClick={cancel} type="button">×</button>
//           </div>
//           <form onSubmit={submit} className="entity-form">
//             {config.fields.map((field) => (
//               <label className={`${field.wide ? 'wide-field ' : ''}${field.type === 'checkbox' ? 'checkbox-field' : ''}`} key={field.name}>
//                 {field.type !== 'checkbox' && <span>{field.label}{field.required ? ' *' : ''}</span>}
//                 <Field field={field} value={form[field.name]} form={form} onChange={change} />
//               </label>
//             ))}
//             <div className="form-actions wide-field">
//               <button type="button" className="secondary-button" onClick={cancel}>Cancel</button>
//               <button type="submit" className="primary-button" disabled={busy}>{busy ? 'Saving…' : editingId ? 'Update' : 'Create'}</button>
//             </div>
//           </form>
//         </section>
//       )}

//       <section className="panel table-panel">
//         <div className="panel-heading">
//           <div><h2>All {config.title}</h2><p>{rows.length} record{rows.length === 1 ? '' : 's'}</p></div>
//         </div>
//         {rows.length === 0 ? (
//           <div className="empty-state"><strong>No records yet</strong><span>Use the Add button to create the first {config.singular.toLowerCase()}.</span></div>
//         ) : (
//           <div className="table-scroll">
//             <table>
//               <thead><tr><th>ID</th>{config.columns.map((column) => <th key={column[0]}>{column[1]}</th>)}<th>Actions</th></tr></thead>
//               <tbody>
//                 {rows.map((row) => (
//                   <tr key={row.id}>
//                     <td>#{row.id}</td>
//                     {config.columns.map(([key, , render]) => <td key={key}>{render ? render(row) : row[key] || '—'}</td>)}
//                     <td className="action-cell">
//                       <button className="text-button" onClick={() => startEdit(row)}>Edit</button>
//                       <button className="text-button danger" onClick={() => onDelete(resource, row.id, config.singular)}>Delete</button>
//                     </td>
//                   </tr>
//                 ))}
//               </tbody>
//             </table>
//           </div>
//         )}
//       </section>
//     </div>
//   );
// }

// export default function App() {
//   const [active, setActive] = useState('dashboard');
//   const [data, setData] = useState(emptyData);
//   const [loading, setLoading] = useState(true);
//   const [busy, setBusy] = useState(false);
//   const [message, setMessage] = useState(null);

//   const config = useMemo(() => active === 'dashboard' ? null : getConfig(active, data), [active, data]);

//   async function loadAll(showLoader = true) {
//     if (showLoader) setLoading(true);
//     try {
//       const values = await Promise.all(resources.map((resource) => api.list(resource)));
//       setData(Object.fromEntries(resources.map((resource, index) => [resource, values[index]])));
//     } catch (error) {
//       setMessage({ type: 'error', text: `${error.message}. Make sure the Spring Boot backend is running on port 8080.` });
//     } finally {
//       if (showLoader) setLoading(false);
//     }
//   }

//   useEffect(() => { loadAll(); }, []);

//   useEffect(() => {
//     if (!message) return undefined;
//     const timeout = setTimeout(() => setMessage(null), 5000);
//     return () => clearTimeout(timeout);
//   }, [message]);

//   async function save(resource, id, payload) {
//     setBusy(true);
//     try {
//       if (id) await api.update(resource, id, payload);
//       else await api.create(resource, payload);
//       await loadAll(false);
//       setMessage({ type: 'success', text: `${id ? 'Updated' : 'Created'} successfully.` });
//     } catch (error) {
//       setMessage({ type: 'error', text: error.message });
//       throw error;
//     } finally {
//       setBusy(false);
//     }
//   }

//   async function remove(resource, id, singular) {
//     if (!window.confirm(`Delete this ${singular.toLowerCase()}?`)) return;
//     setBusy(true);
//     try {
//       await api.remove(resource, id);
//       await loadAll(false);
//       setMessage({ type: 'success', text: `${singular} deleted.` });
//     } catch (error) {
//       setMessage({ type: 'error', text: error.message });
//     } finally {
//       setBusy(false);
//     }
//   }

//   return (
//     <div className="app-shell">
//       <aside className="sidebar">
//         <div className="brand"><span>SF</span><div><strong>SprintFlow</strong><small>CRUD edition</small></div></div>
//         <nav>
//           {navigation.map((item) => (
//             <button key={item.id} className={active === item.id ? 'active' : ''} onClick={() => setActive(item.id)}>
//               <span className="nav-icon">{item.icon}</span>{item.label}
//               {item.id !== 'dashboard' && <small>{data[item.id].length}</small>}
//             </button>
//           ))}
//         </nav>
//         <div className="sidebar-note"><strong>Backend</strong><span>http://localhost:8080</span><strong>Database</strong><span>sprintflow</span></div>
//       </aside>

//       <main>
//         <header className="topbar">
//           <div><strong>{active === 'dashboard' ? 'Project Overview' : config?.title}</strong><span>Entity formation + basic CRUD only</span></div>
//           <button className="secondary-button" onClick={() => loadAll()} disabled={loading}>Refresh Data</button>
//         </header>

//         <div className="content">
//           {message && <div className={`toast ${message.type}`}>{message.text}</div>}
//           {loading ? (
//             <div className="loading-screen"><span className="spinner" /><strong>Connecting to SprintFlow API…</strong></div>
//           ) : active === 'dashboard' ? (
//             <Dashboard data={data} onOpen={setActive} />
//           ) : (
//             <CrudPage key={active} resource={active} rows={data[active]} config={config} onSave={save} onDelete={remove} busy={busy} />
//           )}
//         </div>
//       </main>
//     </div>
//   );
// }
