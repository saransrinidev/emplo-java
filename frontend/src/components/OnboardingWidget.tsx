import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import {
  Check,
  ArrowRight,
  Sparkles,
  User,
  FileText,
  Laptop,
  ClipboardCheck,
  Users,
  GraduationCap,
  Settings,
  ExternalLink,
} from "lucide-react";
import { onboardingApi, type OnboardingTask, type OnboardingProgress } from "../api/onboarding";

const CATEGORY_META: Record<string, { label: string; icon: React.ComponentType<any>; color: string }> = {
  personal_info: { label: "Personal Information", icon: User, color: "#3b82f6" },
  documents: { label: "Documents", icon: FileText, color: "#8b5cf6" },
  it_setup: { label: "IT & System Setup", icon: Laptop, color: "#06b6d4" },
  compliance: { label: "Compliance & Policies", icon: ClipboardCheck, color: "#f59e0b" },
  team_intro: { label: "Team Introduction", icon: Users, color: "#10b981" },
  training: { label: "Training", icon: GraduationCap, color: "#ec4899" },
  custom: { label: "Other", icon: Settings, color: "#6b7280" },
};

/**
 * Compact onboarding checklist widget for the employee dashboard.
 * Shows overall progress + the next few pending tasks.
 * Hides itself automatically once onboarding is 100% complete.
 */
export default function OnboardingWidget() {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState<OnboardingTask[]>([]);
  const [progress, setProgress] = useState<OnboardingProgress | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    try {
      const [t, p] = await Promise.all([onboardingApi.myTasks(), onboardingApi.myProgress()]);
      setTasks(t);
      setProgress(p);
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

  const handleComplete = async (taskId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    // Optimistic update — instantly remove/mark the task, then sync with server
    const prevTasks = tasks;
    const prevProgress = progress;
    const updatedTasks = tasks.map((t) => (t.id === taskId ? { ...t, status: "completed" as const } : t));
    setTasks(updatedTasks);
    if (progress) {
      const newCompleted = progress.completed + 1;
      setProgress({ ...progress, completed: newCompleted, percentage: Math.round((newCompleted / progress.total) * 100) });
    }
    try {
      await onboardingApi.completeTask(taskId);
    } catch {
      // Revert on failure
      setTasks(prevTasks);
      setProgress(prevProgress);
    }
  };

  if (loading) return null;
  // No tasks assigned at all — don't show the widget
  if (!progress || tasks.length === 0) return null;
  // Fully onboarded — don't clutter the dashboard
  if (progress.percentage >= 100) return null;

  const pendingTasks = tasks
    .filter((t) => t.status === "pending" || t.status === "in_progress")
    .slice(0, 4);

  return (
    <motion.div
      className="onboarding-widget"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
    >
      <div className="onboarding-widget-header">
        <div className="onboarding-widget-title-row">
          <span className="onboarding-widget-icon">
            <Sparkles size={16} />
          </span>
          <div>
            <h3>Complete Your Onboarding</h3>
            <p>Finish these steps to get fully set up</p>
          </div>
        </div>

        <div className="onboarding-widget-progress">
          <div className="onboarding-widget-ring">
            <svg viewBox="0 0 36 36" className="onboarding-widget-ring-svg">
              <circle cx="18" cy="18" r="15" className="onboarding-widget-ring-bg" />
              <circle
                cx="18"
                cy="18"
                r="15"
                className="onboarding-widget-ring-fill"
                style={{
                  strokeDasharray: `${2 * Math.PI * 15}`,
                  strokeDashoffset: `${2 * Math.PI * 15 * (1 - progress.percentage / 100)}`,
                }}
              />
            </svg>
            <span className="onboarding-widget-ring-text">{progress.percentage}%</span>
          </div>
        </div>
      </div>

      <div className="onboarding-widget-tasks">
        {pendingTasks.map((task) => {
          const meta = CATEGORY_META[task.category] || CATEGORY_META.custom;
          const Icon = meta.icon;
          return (
            <div
              key={task.id}
              className="onboarding-widget-task"
              onClick={() => navigate("/onboarding")}
            >
              <span
                className="onboarding-widget-task-checkbox"
                onClick={(e) => handleComplete(task.id, e)}
                title="Mark as done"
              >
                <span className="onboarding-widget-checkbox-circle" />
              </span>
              <span className="onboarding-widget-task-icon" style={{ color: meta.color, backgroundColor: `${meta.color}15` }}>
                <Icon size={13} />
              </span>
              <span className="onboarding-widget-task-title">{task.title}</span>
              {task.is_required && <span className="onboarding-widget-required-dot" title="Required" />}
            </div>
          );
        })}
      </div>

      <Link to="/onboarding" className="onboarding-widget-footer">
        View full checklist
        <ArrowRight size={14} />
      </Link>
    </motion.div>
  );
}
