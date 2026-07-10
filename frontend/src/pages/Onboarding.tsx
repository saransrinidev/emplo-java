import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import {
  onboardingApi,
  type OnboardingTask,
  type OnboardingProgress,
  type OnboardingSummaryItem,
  type OnboardingTemplate,
} from "../api/onboarding";
import {
  Check,
  ExternalLink,
  Calendar,
  Trash2,
  Plus,
  Sparkles,
  Info,
  User,
  FileText,
  Laptop,
  ClipboardCheck,
  Users,
  GraduationCap,
  Settings
} from "lucide-react";

// ─── Category metadata ────────────────────────────────────────────────────────

const CATEGORY_META: Record<string, { label: string; icon: React.ComponentType<any>; color: string }> = {
  personal_info: { label: "Personal Information", icon: User, color: "#3b82f6" },
  documents: { label: "Documents", icon: FileText, color: "#8b5cf6" },
  it_setup: { label: "IT & System Setup", icon: Laptop, color: "#06b6d4" },
  compliance: { label: "Compliance & Policies", icon: ClipboardCheck, color: "#f59e0b" },
  team_intro: { label: "Team Introduction", icon: Users, color: "#10b981" },
  training: { label: "Training", icon: GraduationCap, color: "#ec4899" },
  custom: { label: "Other", icon: Settings, color: "#6b7280" },
};

// ─── Employee Onboarding View ─────────────────────────────────────────────────

function EmployeeOnboarding() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [tasks, setTasks] = useState<OnboardingTask[]>([]);
  const [progress, setProgress] = useState<OnboardingProgress | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState<string>("");

  const load = async () => {
    try {
      const [t, p] = await Promise.all([onboardingApi.myTasks(), onboardingApi.myProgress()]);
      setTasks(t);
      setProgress(p);

      // Auto-select the first incomplete category, or fallback to the first category
      const groupedTasks = groupByCategory(t);
      const categories = Object.keys(groupedTasks);
      if (categories.length > 0) {
        const incomplete = categories.find((cat) => {
          const catTasks = t.filter((task) => task.category === cat);
          return catTasks.some((task) => task.status === "pending" || task.status === "in_progress");
        });
        setActiveCategory(incomplete || categories[0]);
      }
    } catch {
      setTasks([]);
      setProgress(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleComplete = async (taskId: string) => {
    await onboardingApi.completeTask(taskId);
    load();
  };

  const handleSkip = async (taskId: string) => {
    await onboardingApi.skipTask(taskId);
    load();
  };

  const groupByCategory = (tasksList: OnboardingTask[]) => {
    const grouped: Record<string, OnboardingTask[]> = {};
    tasksList.forEach((t) => {
      if (!grouped[t.category]) grouped[t.category] = [];
      grouped[t.category].push(t);
    });
    return grouped;
  };

  if (loading) {
    return (
      <div className="loading-spinner-wrapper">
        <div className="spinner" />
        <p>Loading onboarding checklist...</p>
      </div>
    );
  }

  if (!progress || tasks.length === 0) {
    return (
      <div className="onboarding-empty">
        <div className="onboarding-empty-icon" style={{ color: "#10b981" }}>
          <Sparkles size={48} />
        </div>
        <h2>All Set!</h2>
        <p>You have no pending onboarding tasks. Welcome aboard!</p>
      </div>
    );
  }

  const grouped = groupByCategory(tasks);
  const activeTasks = grouped[activeCategory] || [];
  const completedCount = tasks.filter((t) => t.status === "completed" || t.status === "skipped").length;

  return (
    <div className="onboarding-page">
      {/* Welcome & Progress Banner */}
      <div className="onboarding-header">
        <div className="onboarding-header-text">
          <h1>
            Welcome Aboard, {user?.name.split(" ")[0]}!{" "}
            <Sparkles style={{ display: "inline", color: "#fef08a" }} size={24} />
          </h1>
          <p>Let's get you set up and settled into your new role. Follow the checklist below.</p>
        </div>
        <div className="onboarding-progress-card">
          <div className="progress-circle">
            <svg viewBox="0 0 100 100" className="progress-svg">
              <circle cx="50" cy="50" r="40" className="progress-bg" />
              <circle
                cx="50"
                cy="50"
                r="40"
                className="progress-fill"
                style={{
                  strokeDasharray: `${2 * Math.PI * 40}`,
                  strokeDashoffset: `${2 * Math.PI * 40 * (1 - progress.percentage / 100)}`,
                }}
              />
            </svg>
            <span className="progress-text">{progress.percentage}%</span>
          </div>
          <div className="progress-stats">
            <span className="progress-stat-number">
              {completedCount}/{tasks.length}
            </span>
            <span className="progress-stat-label">Tasks Done</span>
          </div>
        </div>
      </div>

      {/* Main Layout Grid */}
      <div className="onboarding-layout">
        {/* Sidebar Nav */}
        <div className="onboarding-sidebar">
          {Object.entries(grouped).map(([category, categoryTasks]) => {
            const meta = CATEGORY_META[category] || CATEGORY_META.custom;
            const catCompleted = categoryTasks.filter(
              (t) => t.status === "completed" || t.status === "skipped"
            ).length;
            const allDone = catCompleted === categoryTasks.length;
            const Icon = meta.icon;
            const active = activeCategory === category;

            return (
              <button
                key={category}
                className={`category-nav-item ${active ? "active" : ""}`}
                onClick={() => setActiveCategory(category)}
              >
                <span
                  className="nav-icon"
                  style={{
                    backgroundColor: active ? meta.color : `${meta.color}15`,
                    color: active ? "#ffffff" : meta.color,
                  }}
                >
                  <Icon size={16} />
                </span>
                <span className="nav-label">{meta.label}</span>
                <span className={`nav-status ${allDone ? "nav-status-done" : ""}`}>
                  {allDone ? <Check size={12} strokeWidth={3} /> : `${catCompleted}/${categoryTasks.length}`}
                </span>
              </button>
            );
          })}
        </div>

        {/* Content Pane */}
        <div className="onboarding-content-panel">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeCategory}
              initial={{ opacity: 0, x: 15 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -15 }}
              transition={{ duration: 0.2 }}
            >
              <div className="active-category-header">
                <div className="active-category-info">
                  <h2>
                    {(() => {
                      const ActiveIcon = CATEGORY_META[activeCategory]?.icon || Settings;
                      return (
                        <ActiveIcon
                          size={22}
                          style={{
                            color: CATEGORY_META[activeCategory]?.color,
                            marginRight: "10px",
                            verticalAlign: "middle",
                            display: "inline-block",
                          }}
                        />
                      );
                    })()}
                    {CATEGORY_META[activeCategory]?.label || activeCategory}
                  </h2>
                  <p>Please complete these steps to finish this section.</p>
                </div>
                <div className="active-category-progress">
                  {activeTasks.filter((t) => t.status === "completed" || t.status === "skipped").length}/{activeTasks.length} Completed
                </div>
              </div>

              <div className="tasks-list">
                {activeTasks.map((task) => (
                  <TaskCard
                    key={task.id}
                    task={task}
                    onComplete={handleComplete}
                    onSkip={handleSkip}
                    navigate={navigate}
                  />
                ))}
              </div>
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}

// ─── Task Card Component ──────────────────────────────────────────────────────

function TaskCard({
  task,
  onComplete,
  onSkip,
  navigate,
}: {
  task: OnboardingTask;
  onComplete: (id: string) => void;
  onSkip: (id: string) => void;
  navigate: (path: string) => void;
}) {
  const isDone = task.status === "completed" || task.status === "skipped";
  const isOverdue = task.due_date && new Date(task.due_date) < new Date() && !isDone;

  const handleActionClick = () => {
    if (!task.action_url) return;
    if (task.action_url.startsWith("http")) {
      window.open(task.action_url, "_blank");
    } else {
      navigate(task.action_url);
    }
  };

  return (
    <div className={`task-card ${isDone ? "task-done" : ""}`}>
      <div className="task-checkbox-wrapper">
        <div
          className={`custom-checkbox ${isDone ? "checked" : ""}`}
          onClick={() => !isDone && onComplete(task.id)}
        >
          {isDone && <Check size={12} className="checkmark-icon" strokeWidth={3} />}
        </div>
      </div>
      <div className="task-details">
        <div className="task-title-row">
          <span className="task-title-text">{task.title}</span>
          {task.is_required ? (
            <span className="tag-badge tag-required">Required</span>
          ) : (
            <span className="tag-badge tag-optional">Optional</span>
          )}
          {task.status === "skipped" && <span className="tag-badge tag-skipped">Skipped</span>}
        </div>
        {task.description && <p className="task-desc">{task.description}</p>}
        <div className="task-meta-row">
          {task.due_date && !isDone && (
            <span className={`due-tag ${isOverdue ? "overdue" : ""}`}>
              <Calendar size={12} style={{ marginRight: "4px" }} /> Due:{" "}
              {new Date(task.due_date).toLocaleDateString()} {isOverdue && "(Overdue)"}
            </span>
          )}
        </div>

        {!isDone && (
          <div className="task-actions-row">
            {task.action_url && (
              <button className="btn-action-primary" onClick={handleActionClick}>
                Start Task <ExternalLink size={14} style={{ marginLeft: "4px" }} />
              </button>
            )}
            <button className="btn-action-secondary" onClick={() => onComplete(task.id)}>
              Mark Done
            </button>
            {!task.is_required && (
              <button className="btn-action-skip" onClick={() => onSkip(task.id)}>
                Skip
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ─── HR Onboarding Management View ───────────────────────────────────────────

function HROnboarding() {
  const [tab, setTab] = useState<"summary" | "templates">("summary");
  const [summary, setSummary] = useState<OnboardingSummaryItem[]>([]);
  const [templates, setTemplates] = useState<OnboardingTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);

  useEffect(() => {
    loadData();
  }, [tab]);

  const loadData = async () => {
    setLoading(true);
    try {
      if (tab === "summary") {
        setSummary(await onboardingApi.getSummary());
      } else {
        setTemplates(await onboardingApi.listTemplates());
      }
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="onboarding-page">
      <div className="onboarding-hr-header">
        <h1>Onboarding Hub</h1>
        <div className="onboarding-tabs">
          <button className={`tab ${tab === "summary" ? "active" : ""}`} onClick={() => setTab("summary")}>
            Employee Progress
          </button>
          <button className={`tab ${tab === "templates" ? "active" : ""}`} onClick={() => setTab("templates")}>
            Checklist Templates
          </button>
        </div>
      </div>

      {loading ? (
        <div className="loading-spinner-wrapper">
          <div className="spinner" />
          <p>Fetching onboarding data...</p>
        </div>
      ) : tab === "summary" ? (
        <SummaryView items={summary} />
      ) : (
        <TemplatesView
          templates={templates}
          onRefresh={loadData}
          showCreate={showCreate}
          setShowCreate={setShowCreate}
        />
      )}
    </div>
  );
}

// ─── Summary View (HR sees all employee progress) ─────────────────────────────

function SummaryView({ items }: { items: OnboardingSummaryItem[] }) {
  if (items.length === 0) {
    return (
      <div className="onboarding-empty">
        <div className="onboarding-empty-icon">
          <Info size={48} />
        </div>
        <h3>No Employees in Onboarding</h3>
        <p>Go to the Employees directory to initialize onboarding checklists.</p>
      </div>
    );
  }

  // Helper to generate name initials
  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .substring(0, 2);
  };

  return (
    <div className="summary-grid">
      {items.map((item) => {
        const initials = getInitials(item.employee_name);
        return (
          <div key={item.employee_id} className={`summary-card ${item.is_complete ? "card-complete" : ""}`}>
            <div className="summary-card-header">
              <div className={`employee-avatar-circle ${item.is_complete ? "avatar-green" : ""}`}>
                {initials}
              </div>
              <div className="employee-info-text">
                <h4>{item.employee_name}</h4>
                <span className="summary-meta">
                  {item.employee_code || "N/A"} • {item.department || "General"}
                </span>
              </div>
            </div>

            <div className="summary-progress-section">
              <div className="progress-header-row">
                <span>
                  {item.completed_tasks}/{item.total_tasks} Tasks
                </span>
                <span className={`progress-label ${item.is_complete ? "complete" : ""}`}>{item.percentage}%</span>
              </div>
              <div className="progress-bar-mini">
                <div className="progress-bar-fill" style={{ width: `${item.percentage}%` }} />
              </div>
            </div>

            <div className="summary-card-footer">
              {item.date_of_joining ? (
                <span>Joined: {new Date(item.date_of_joining).toLocaleDateString()}</span>
              ) : (
                <span>—</span>
              )}
              {item.is_complete ? (
                <span className="badge-complete">✓ Onboarded</span>
              ) : (
                <span style={{ color: "#d97706", fontWeight: 600 }}>In Progress</span>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ─── Templates View (HR manages checklist templates) ──────────────────────────

function TemplatesView({
  templates,
  onRefresh,
  showCreate,
  setShowCreate,
}: {
  templates: OnboardingTemplate[];
  onRefresh: () => void;
  showCreate: boolean;
  setShowCreate: (v: boolean) => void;
}) {
  const [form, setForm] = useState({
    title: "",
    description: "",
    category: "personal_info",
    sort_order: 0,
    is_required: true,
    due_days: 7,
    action_type: "manual",
    action_url: "",
  });

  const handleCreate = async () => {
    if (!form.title.trim()) return;
    await onboardingApi.createTemplate({
      title: form.title,
      description: form.description || undefined,
      category: form.category as any,
      sort_order: form.sort_order,
      is_required: form.is_required,
      due_days: form.due_days,
      action_type: form.action_type,
      action_url: form.action_url || undefined,
    });
    setShowCreate(false);
    setForm({
      title: "",
      description: "",
      category: "personal_info",
      sort_order: 0,
      is_required: true,
      due_days: 7,
      action_type: "manual",
      action_url: "",
    });
    onRefresh();
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Are you sure you want to delete this onboarding checklist item template?")) return;
    await onboardingApi.deleteTemplate(id);
    onRefresh();
  };

  return (
    <div className="templates-section">
      <div className="templates-header">
        <p className="templates-subtitle">Configure the checklist templates that are assigned to new employees.</p>
        {!showCreate && (
          <button className="btn-primary" onClick={() => setShowCreate(true)}>
            <Plus size={16} style={{ marginRight: "4px" }} /> Add Template
          </button>
        )}
      </div>

      {showCreate && (
        <div className="template-form">
          <h4>Add Onboarding Checklist Item</h4>
          <div className="form-grid">
            <div className="form-field full-width">
              <label>Task Title *</label>
              <input
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="e.g., Upload Identity & Address Proof"
              />
            </div>

            <div className="form-field">
              <label>Category</label>
              <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                {Object.entries(CATEGORY_META).map(([key, meta]) => (
                  <option key={key} value={key}>
                    {meta.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-field">
              <label>Due Within (Days from Joining)</label>
              <input
                type="number"
                value={form.due_days}
                onChange={(e) => setForm({ ...form, due_days: parseInt(e.target.value) || 0 })}
              />
            </div>

            <div className="form-field">
              <label>Sort Order Position</label>
              <input
                type="number"
                value={form.sort_order}
                onChange={(e) => setForm({ ...form, sort_order: parseInt(e.target.value) || 0 })}
              />
            </div>

            <div className="form-field">
              <label>Action Link / Route URL (Optional)</label>
              <input
                value={form.action_url}
                onChange={(e) => setForm({ ...form, action_url: e.target.value })}
                placeholder="e.g., /documents or https://example.com"
              />
            </div>

            <div className="form-field full-width">
              <label>Detailed Description</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Describe what the employee needs to do..."
                rows={3}
              />
            </div>

            <div className="form-field checkbox-field">
              <input
                type="checkbox"
                id="isRequiredCheck"
                checked={form.is_required}
                onChange={(e) => setForm({ ...form, is_required: e.target.checked })}
              />
              <label htmlFor="isRequiredCheck" style={{ cursor: "pointer", textTransform: "none" }}>
                This task is mandatory for completion
              </label>
            </div>
          </div>
          <div className="form-actions">
            <button className="btn-action-primary" onClick={handleCreate}>
              Save Template
            </button>
            <button className="btn-action-secondary" onClick={() => setShowCreate(false)}>
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="templates-list">
        {templates.length === 0 ? (
          <div className="onboarding-empty">
            <p>No template items defined yet. Click "Add Template" to start setting up the onboarding flow.</p>
          </div>
        ) : (
          templates
            .sort((a, b) => a.sort_order - b.sort_order)
            .map((t) => {
              const meta = CATEGORY_META[t.category] || CATEGORY_META.custom;
              const Icon = meta.icon;
              return (
                <div key={t.id} className="template-card">
                  <div className="template-card-left">
                    <span
                      className="template-icon"
                      style={{
                        backgroundColor: `${meta.color}15`,
                        color: meta.color,
                        padding: "10px",
                        borderRadius: "10px",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        width: "44px",
                        height: "44px",
                      }}
                    >
                      <Icon size={20} />
                    </span>
                    <div>
                      <div className="template-title">
                        {t.title} {t.is_required && <span style={{ color: "#ef4444" }}>*</span>}
                      </div>
                      {t.description && <p className="template-desc">{t.description}</p>}
                      <span className="template-meta">
                        {meta.label} • Sort Order: {t.sort_order} • Due in {t.due_days || "N/A"} days
                        {t.action_url && ` • Action URL: ${t.action_url}`}
                      </span>
                    </div>
                  </div>
                  <button className="btn-danger-sm" onClick={() => handleDelete(t.id)}>
                    <Trash2 size={14} style={{ display: "inline", marginRight: "4px" }} /> Delete
                  </button>
                </div>
              );
            })
        )}
      </div>
    </div>
  );
}

// ─── Main Export ──────────────────────────────────────────────────────────────

export default function Onboarding() {
  const { user } = useAuth();
  if (!user) return null;

  return user.role === "hr_admin" ? <HROnboarding /> : <EmployeeOnboarding />;
}
