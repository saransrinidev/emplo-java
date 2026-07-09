import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import {
  onboardingApi,
  type OnboardingTask,
  type OnboardingProgress,
  type OnboardingSummaryItem,
  type OnboardingTemplate,
} from "../api/onboarding";
import {
  User, FileText, Monitor, ClipboardCheck, Users, GraduationCap, Settings,
  CheckCircle2, Circle, Plus, Trash2, Sparkles
} from "lucide-react";

// ─── Category metadata ────────────────────────────────────────────────────────

const CATEGORY_ICONS: Record<string, React.ReactNode> = {
  personal_info: <User size={16} />,
  documents: <FileText size={16} />,
  it_setup: <Monitor size={16} />,
  compliance: <ClipboardCheck size={16} />,
  team_intro: <Users size={16} />,
  training: <GraduationCap size={16} />,
  custom: <Settings size={16} />,
};

const CATEGORY_META: Record<string, { label: string; color: string }> = {
  personal_info: { label: "Personal Information", color: "#3b82f6" },
  documents: { label: "Documents", color: "#8b5cf6" },
  it_setup: { label: "IT & System Setup", color: "#06b6d4" },
  compliance: { label: "Compliance & Policies", color: "#f59e0b" },
  team_intro: { label: "Team Introduction", color: "#10b981" },
  training: { label: "Training", color: "#ec4899" },
  custom: { label: "Other", color: "#6b7280" },
};

// ─── Employee Onboarding View ─────────────────────────────────────────────────

function EmployeeOnboarding() {
  const [tasks, setTasks] = useState<OnboardingTask[]>([]);
  const [progress, setProgress] = useState<OnboardingProgress | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    try {
      const [t, p] = await Promise.all([onboardingApi.myTasks(), onboardingApi.myProgress()]);
      setTasks(t);
      setProgress(p);
    } catch {
      // No onboarding tasks
      setTasks([]);
      setProgress(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleComplete = async (taskId: string) => {
    await onboardingApi.completeTask(taskId);
    load();
  };

  const handleSkip = async (taskId: string) => {
    await onboardingApi.skipTask(taskId);
    load();
  };

  if (loading) return <div className="loading-spinner">Loading...</div>;
  if (!progress || tasks.length === 0) {
    return (
      <div className="onboarding-empty">
        <div className="onboarding-empty-icon"><CheckCircle2 size={48} /></div>
        <h2>No Onboarding Tasks</h2>
        <p>You're all set! No pending onboarding items.</p>
      </div>
    );
  }

  // Group tasks by category
  const grouped: Record<string, OnboardingTask[]> = {};
  tasks.forEach((t) => {
    if (!grouped[t.category]) grouped[t.category] = [];
    grouped[t.category].push(t);
  });

  const completedCount = tasks.filter((t) => t.status === "completed" || t.status === "skipped").length;

  return (
    <div className="onboarding-page">
      {/* Progress Header */}
      <div className="onboarding-header">
        <div className="onboarding-header-text">
          <h1>Welcome Aboard! <Sparkles size={24} style={{ display: "inline", verticalAlign: "middle" }} /></h1>
          <p>Complete your onboarding checklist to get started with the team.</p>
        </div>
        <div className="onboarding-progress-card">
          <div className="progress-circle">
            <svg viewBox="0 0 100 100" className="progress-svg">
              <circle cx="50" cy="50" r="40" className="progress-bg" />
              <circle
                cx="50" cy="50" r="40"
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
            <span className="progress-stat-number">{completedCount}/{tasks.length}</span>
            <span className="progress-stat-label">tasks completed</span>
          </div>
        </div>
      </div>

      {/* Checklist by Category */}
      <div className="onboarding-categories">
        {Object.entries(grouped).map(([category, categoryTasks]) => {
          const meta = CATEGORY_META[category] || CATEGORY_META.custom;
          const catCompleted = categoryTasks.filter(
            (t) => t.status === "completed" || t.status === "skipped"
          ).length;
          const allDone = catCompleted === categoryTasks.length;

          return (
            <div key={category} className={`onboarding-category ${allDone ? "category-done" : ""}`}>
              <div className="category-header">
                <span className="category-icon" style={{ color: meta.color }}>{CATEGORY_ICONS[category] || CATEGORY_ICONS.custom}</span>
                <h3 className="category-title">{meta.label}</h3>
                <span className="category-count">
                  {catCompleted}/{categoryTasks.length}
                </span>
                {allDone && <span className="category-badge-done"><CheckCircle2 size={12} style={{ display: "inline", verticalAlign: "middle" }} /> Complete</span>}
              </div>

              <div className="category-tasks">
                {categoryTasks.map((task) => (
                  <TaskItem
                    key={task.id}
                    task={task}
                    onComplete={handleComplete}
                    onSkip={handleSkip}
                  />
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─── Task Item Component ──────────────────────────────────────────────────────

function TaskItem({
  task,
  onComplete,
  onSkip,
}: {
  task: OnboardingTask;
  onComplete: (id: string) => void;
  onSkip: (id: string) => void;
}) {
  const isDone = task.status === "completed" || task.status === "skipped";

  return (
    <div className={`task-item ${isDone ? "task-done" : ""}`}>
      <div className="task-checkbox" onClick={() => !isDone && onComplete(task.id)}>
        {isDone ? (
          <span className="checkbox-checked"><CheckCircle2 size={14} /></span>
        ) : (
          <span className="checkbox-empty"><Circle size={20} /></span>
        )}
      </div>
      <div className="task-content">
        <div className="task-title">
          {task.title}
          {task.is_required && <span className="task-required">*</span>}
        </div>
        {task.description && <p className="task-description">{task.description}</p>}
        {task.due_date && !isDone && (
          <span className="task-due">Due: {new Date(task.due_date).toLocaleDateString()}</span>
        )}
        {task.status === "skipped" && <span className="task-skipped-label">Skipped</span>}
      </div>
      <div className="task-actions">
        {!isDone && (
          <>
            <button className="btn-task-complete" onClick={() => onComplete(task.id)}>
              Mark Done
            </button>
            {!task.is_required && (
              <button className="btn-task-skip" onClick={() => onSkip(task.id)}>
                Skip
              </button>
            )}
          </>
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
        <h1 style={{ color: "white" }}>Onboarding Management</h1>
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
        <div className="loading-spinner">Loading...</div>
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
        <h3>No employees in onboarding</h3>
        <p>Initialize onboarding from the Employee detail page.</p>
      </div>
    );
  }

  return (
    <div className="summary-grid">
      {items.map((item) => (
        <div key={item.employee_id} className={`summary-card ${item.is_complete ? "card-complete" : ""}`}>
          <div className="summary-card-header">
            <div>
              <h4>{item.employee_name}</h4>
              <span className="summary-meta">{item.employee_code} • {item.department || "—"}</span>
            </div>
            <div className="summary-progress-mini">
              <div className="progress-bar-mini">
                <div
                  className="progress-bar-fill"
                  style={{ width: `${item.percentage}%` }}
                />
              </div>
              <span className="progress-label">{item.percentage}%</span>
            </div>
          </div>
          <div className="summary-card-footer">
            <span>{item.completed_tasks}/{item.total_tasks} tasks</span>
            {item.date_of_joining && <span>Joined: {item.date_of_joining}</span>}
            {item.is_complete && <span className="badge-complete"><CheckCircle2 size={12} style={{ display: "inline", verticalAlign: "middle" }} /> Completed</span>}
          </div>
        </div>
      ))}
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
    setForm({ title: "", description: "", category: "personal_info", sort_order: 0, is_required: true, due_days: 7, action_type: "manual", action_url: "" });
    onRefresh();
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this template?")) return;
    await onboardingApi.deleteTemplate(id);
    onRefresh();
  };

  return (
    <div className="templates-section">
      <div className="templates-header">
        <p className="templates-subtitle">Define checklist items that new employees must complete.</p>
        <button className="btn-primary" onClick={() => setShowCreate(true)}><Plus size={15} style={{ marginRight: 4 }} /> Add Template</button>
      </div>

      {showCreate && (
        <div className="template-form card">
          <h4>New Onboarding Item</h4>
          <div className="form-grid">
            <div className="form-field">
              <label>Title *</label>
              <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="e.g. Upload ID proof" />
            </div>
            <div className="form-field">
              <label>Category</label>
              <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                {Object.entries(CATEGORY_META).map(([key, meta]) => (
                  <option key={key} value={key}>{meta.label}</option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label>Description</label>
              <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={2} />
            </div>
            <div className="form-field">
              <label>Due in (days from joining)</label>
              <input type="number" value={form.due_days} onChange={(e) => setForm({ ...form, due_days: parseInt(e.target.value) || 0 })} />
            </div>
            <div className="form-field">
              <label>Order</label>
              <input type="number" value={form.sort_order} onChange={(e) => setForm({ ...form, sort_order: parseInt(e.target.value) || 0 })} />
            </div>
            <div className="form-field checkbox-field">
              <label><input type="checkbox" checked={form.is_required} onChange={(e) => setForm({ ...form, is_required: e.target.checked })} /> Required</label>
            </div>
          </div>
          <div className="form-actions">
            <button className="btn-primary" onClick={handleCreate}>Save</button>
            <button className="btn-secondary" onClick={() => setShowCreate(false)}>Cancel</button>
          </div>
        </div>
      )}

      <div className="templates-list">
        {templates.length === 0 ? (
          <div className="onboarding-empty"><p>No templates yet. Create your first onboarding checklist item.</p></div>
        ) : (
          templates.map((t) => {
            const meta = CATEGORY_META[t.category] || CATEGORY_META.custom;
            return (
              <div key={t.id} className="template-card">
                <div className="template-card-left">
                  <span className="template-icon" style={{ color: meta.color }}>{CATEGORY_ICONS[t.category] || CATEGORY_ICONS.custom}</span>
                  <div>
                    <div className="template-title">{t.title} {t.is_required && <span className="task-required">*</span>}</div>
                    {t.description && <p className="template-desc">{t.description}</p>}
                    <span className="template-meta">{meta.label} • Due in {t.due_days || "—"} days</span>
                  </div>
                </div>
                <button className="btn-danger-sm" onClick={() => handleDelete(t.id)}><Trash2 size={13} /></button>
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
