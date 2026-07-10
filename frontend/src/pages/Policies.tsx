import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  BookOpen, Plus, X, Check, Shield, Calendar, FileText,
  Users, Wallet, Lock, HeartPulse, AlertTriangle, Plane,
  Home, FileCheck, ChevronRight, Trash2, Edit3, Eye, Info, Upload,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { policiesApi, type Policy, type PolicyCategory } from "../api/policies";
import { uploadFile } from "../api/upload";

const CATEGORY_META: Record<PolicyCategory, { label: string; icon: React.ComponentType<any>; color: string }> = {
  code_of_conduct: { label: "Code of Conduct", icon: FileCheck, color: "#3b82f6" },
  leave_policy: { label: "Leave Policy", icon: Calendar, color: "#10b981" },
  attendance_policy: { label: "Attendance Policy", icon: Users, color: "#06b6d4" },
  compensation_benefits: { label: "Compensation & Benefits", icon: Wallet, color: "#f59e0b" },
  it_security: { label: "IT & Security", icon: Lock, color: "#6366f1" },
  health_safety: { label: "Health & Safety", icon: HeartPulse, color: "#ef4444" },
  anti_harassment: { label: "Anti-Harassment", icon: AlertTriangle, color: "#dc2626" },
  travel_expense: { label: "Travel & Expense", icon: Plane, color: "#8b5cf6" },
  remote_work: { label: "Remote Work", icon: Home, color: "#ec4899" },
  general: { label: "General", icon: FileText, color: "#6b7280" },
};

function formatDate(d: string): string {
  return new Date(d).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

// ─── Policy Card ───────────────────────────────────────────────────────────────

function PolicyCard({ policy, onClick, pending }: { policy: Policy; onClick: () => void; pending?: boolean }) {
  const meta = CATEGORY_META[policy.category];
  const Icon = meta.icon;
  return (
    <motion.div
      className="policy-card"
      onClick={onClick}
      whileHover={{ y: -2 }}
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <div className="policy-card-icon" style={{ backgroundColor: `${meta.color}15`, color: meta.color }}>
        <Icon size={20} />
      </div>
      <div className="policy-card-body">
        <div className="policy-card-title-row">
          <span className="policy-card-title">{policy.title}</span>
          {pending && <span className="policy-badge-pending">Action Needed</span>}
        </div>
        <div className="policy-card-meta">
          <span>{meta.label}</span>
          <span className="policy-dot">•</span>
          <span>v{policy.version}</span>
          <span className="policy-dot">•</span>
          <span>Updated {formatDate(policy.updated_at)}</span>
        </div>
      </div>
      <ChevronRight size={16} className="policy-card-chevron" />
    </motion.div>
  );
}

// ─── Policy Detail Modal ───────────────────────────────────────────────────────

function PolicyDetailModal({
  policy,
  isHr,
  onClose,
  onChanged,
}: {
  policy: Policy;
  isHr: boolean;
  onClose: () => void;
  onChanged: () => void;
}) {
  const [acknowledged, setAcknowledged] = useState(false);
  const [acking, setAcking] = useState(false);
  const [stats, setStats] = useState<{ total_employees: number; acknowledged_count: number; pending_count: number; percentage: number } | null>(null);
  const meta = CATEGORY_META[policy.category];
  const Icon = meta.icon;

  useEffect(() => {
    if (policy.requires_acknowledgement) {
      policiesApi.hasAcknowledged(policy.id).then((r) => setAcknowledged(r.acknowledged)).catch(() => {});
    }
    if (isHr) {
      policiesApi.stats(policy.id).then(setStats).catch(() => {});
    }
  }, [policy.id, isHr, policy.requires_acknowledgement]);

  const handleAcknowledge = async () => {
    setAcking(true);
    try {
      await policiesApi.acknowledge(policy.id);
      setAcknowledged(true);
      onChanged();
    } finally {
      setAcking(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <motion.div
        className="policy-modal"
        onClick={(e) => e.stopPropagation()}
        initial={{ opacity: 0, scale: 0.96, y: 10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
      >
        <div className="policy-modal-header">
          <div className="policy-detail-title-row">
            <div className="policy-card-icon" style={{ backgroundColor: `${meta.color}15`, color: meta.color }}>
              <Icon size={22} />
            </div>
            <div>
              <h2>{policy.title}</h2>
              <p>{meta.label} • Version {policy.version} • Updated {formatDate(policy.updated_at)}</p>
            </div>
          </div>
          <button className="policy-icon-btn" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="policy-modal-body">
          <div className="policy-content-text">
            {policy.content.split("\n").map((line, i) => (
              <p key={i}>{line || "\u00A0"}</p>
            ))}
          </div>

          {policy.attachment_url && (
            <div className="policy-pdf-viewer">
              <div className="policy-pdf-header">
                <FileText size={14} />
                <span>Attached Document</span>
                <a href={policy.attachment_url} target="_blank" rel="noreferrer" className="policy-pdf-open-btn">
                  Open in new tab
                </a>
              </div>
              {policy.attachment_url.includes(".pdf") || policy.attachment_url.includes("application/pdf") ? (
                <iframe
                  src={policy.attachment_url}
                  className="policy-pdf-embed"
                  title="Policy Document"
                />
              ) : (
                <img
                  src={policy.attachment_url}
                  alt="Policy attachment"
                  className="policy-img-embed"
                />
              )}
            </div>
          )}

          {isHr && stats && (
            <div className="policy-stats-box">
              <div className="policy-stats-header">
                <Shield size={14} /> Acknowledgement Progress
              </div>
              <div className="policy-stats-bar">
                <div className="policy-stats-fill" style={{ width: `${stats.percentage}%` }} />
              </div>
              <div className="policy-stats-numbers">
                {stats.acknowledged_count} of {stats.total_employees} employees acknowledged ({stats.percentage}%)
              </div>
            </div>
          )}
        </div>

        <div className="policy-modal-footer">
          {policy.requires_acknowledgement && !isHr && (
            acknowledged ? (
              <span className="policy-acknowledged-label"><Check size={15} /> You've acknowledged this policy</span>
            ) : (
              <button className="policy-btn-primary" onClick={handleAcknowledge} disabled={acking}>
                <Check size={15} /> {acking ? "Saving..." : "I Acknowledge This Policy"}
              </button>
            )
          )}
          {(!policy.requires_acknowledgement || isHr) && (
            <button className="policy-btn-secondary" onClick={onClose}>Close</button>
          )}
        </div>
      </motion.div>
    </div>
  );
}

// ─── Create/Edit Policy Modal (HR) ────────────────────────────────────────────

function PolicyFormModal({
  policy,
  onClose,
  onSaved,
}: {
  policy?: Policy;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [title, setTitle] = useState(policy?.title || "");
  const [category, setCategory] = useState<PolicyCategory>(policy?.category || "general");
  const [content, setContent] = useState(policy?.content || "");
  const [attachmentUrl, setAttachmentUrl] = useState(policy?.attachment_url || "");
  const [requiresAck, setRequiresAck] = useState(policy?.requires_acknowledgement ?? false);
  const [isPublished, setIsPublished] = useState(policy?.is_published ?? true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.includes("pdf") && !file.type.includes("image")) {
      setError("Only PDF and image files are supported.");
      return;
    }
    setUploading(true);
    setError("");
    try {
      const res = await uploadFile(file);
      setAttachmentUrl(res.url);
    } catch {
      setError("Failed to upload file.");
    } finally {
      setUploading(false);
    }
  };

  const handleSave = async () => {
    setError("");
    if (!title.trim() || !content.trim()) {
      setError("Title and content are required.");
      return;
    }
    setSaving(true);
    try {
      if (policy) {
        await policiesApi.update(policy.id, {
          title, category, content, attachment_url: attachmentUrl || undefined,
          requires_acknowledgement: requiresAck, is_published: isPublished,
        });
      } else {
        await policiesApi.create({
          title, category, content, attachment_url: attachmentUrl || undefined,
          requires_acknowledgement: requiresAck, is_published: isPublished,
        });
      }
      onSaved();
      onClose();
    } catch (err: any) {
      setError(err.message || "Failed to save policy.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <motion.div
        className="policy-modal"
        onClick={(e) => e.stopPropagation()}
        initial={{ opacity: 0, scale: 0.96, y: 10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
      >
        <div className="policy-modal-header">
          <div>
            <h2>{policy ? "Edit Policy" : "New Policy"}</h2>
            <p>{policy ? "Editing will bump the version and notify employees" : "Publish a new HR policy"}</p>
          </div>
          <button className="policy-icon-btn" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="policy-modal-body">
          <div className="policy-form-grid">
            <div className="policy-field full-width">
              <label>Policy Title *</label>
              <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Remote Work Guidelines" />
            </div>
            <div className="policy-field">
              <label>Category</label>
              <select value={category} onChange={(e) => setCategory(e.target.value as PolicyCategory)}>
                {Object.entries(CATEGORY_META).map(([key, meta]) => (
                  <option key={key} value={key}>{meta.label}</option>
                ))}
              </select>
            </div>
            <div className="policy-field checkbox-field">
              <input type="checkbox" id="req-ack" checked={requiresAck} onChange={(e) => setRequiresAck(e.target.checked)} />
              <label htmlFor="req-ack">Requires employee acknowledgement</label>
            </div>
            <div className="policy-field checkbox-field">
              <input type="checkbox" id="is-pub" checked={isPublished} onChange={(e) => setIsPublished(e.target.checked)} />
              <label htmlFor="is-pub">Publish immediately</label>
            </div>
            <div className="policy-field full-width">
              <label>Policy Content *</label>
              <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={10} placeholder="Write the full policy text here..." />
            </div>
            <div className="policy-field full-width">
              <label>Attach Document (PDF)</label>
              <div className="policy-upload-area">
                {attachmentUrl ? (
                  <div className="policy-upload-done">
                    <FileText size={16} />
                    <span>Document attached</span>
                    <button type="button" onClick={() => setAttachmentUrl("")} className="policy-upload-remove"><X size={14} /></button>
                  </div>
                ) : (
                  <label className="policy-upload-label">
                    <Upload size={16} />
                    <span>{uploading ? "Uploading..." : "Choose PDF file"}</span>
                    <input type="file" accept=".pdf,application/pdf,image/*" onChange={handleFileUpload} disabled={uploading} hidden />
                  </label>
                )}
              </div>
            </div>
          </div>
          {error && <p className="policy-error">{error}</p>}
        </div>

        <div className="policy-modal-footer">
          <button className="policy-btn-secondary" onClick={onClose}>Cancel</button>
          <button className="policy-btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? "Saving..." : policy ? "Save Changes" : "Publish Policy"}
          </button>
        </div>
      </motion.div>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function Policies() {
  const { user } = useAuth();
  const isHr = user?.role === "hr_admin";
  const [policies, setPolicies] = useState<Policy[]>([]);
  const [pendingAck, setPendingAck] = useState<Policy[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Policy | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState<Policy | undefined>(undefined);

  const load = async () => {
    setLoading(true);
    try {
      const [all, pending] = await Promise.all([
        isHr ? policiesApi.listAll() : policiesApi.list(),
        !isHr ? policiesApi.pendingAcknowledgement() : Promise.resolve([]),
      ]);
      setPolicies(all);
      setPendingAck(pending);
    } catch {
      setPolicies([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isHr]);

  const handleDelete = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm("Delete this policy? This cannot be undone.")) return;
    await policiesApi.delete(id);
    load();
  };

  const pendingIds = new Set(pendingAck.map((p) => p.id));

  return (
    <div className="policy-page">
      <div className="policy-header">
        <div>
          <h1><BookOpen size={22} style={{ verticalAlign: "middle", marginRight: 8 }} />HR Policies</h1>
          <p>Company policies, guidelines, and compliance documents</p>
        </div>
        {isHr && (
          <button className="policy-btn-primary" onClick={() => { setEditingPolicy(undefined); setShowForm(true); }}>
            <Plus size={16} /> New Policy
          </button>
        )}
      </div>

      {!isHr && pendingAck.length > 0 && (
        <div className="policy-alert-banner">
          <Info size={16} />
          <span>You have {pendingAck.length} polic{pendingAck.length > 1 ? "ies" : "y"} awaiting your acknowledgement.</span>
        </div>
      )}

      <div className="policy-list">
        {loading && <p className="muted" style={{ padding: 24, textAlign: "center" }}>Loading...</p>}
        {!loading && policies.length === 0 && (
          <div className="policy-empty">
            <BookOpen size={40} />
            <h3>No policies published yet</h3>
            <p>{isHr ? "Create your first company policy." : "Check back later for updates."}</p>
          </div>
        )}
        <AnimatePresence>
          {policies.map((policy) => (
            <div key={policy.id} className="policy-card-wrapper">
              <PolicyCard policy={policy} onClick={() => setSelected(policy)} pending={pendingIds.has(policy.id)} />
              {isHr && (
                <div className="policy-card-actions">
                  {!policy.is_published && <span className="policy-draft-tag">Draft</span>}
                  <button className="policy-action-btn" onClick={(e) => { e.stopPropagation(); setEditingPolicy(policy); setShowForm(true); }}>
                    <Edit3 size={14} />
                  </button>
                  <button className="policy-action-btn policy-action-danger" onClick={(e) => handleDelete(policy.id, e)}>
                    <Trash2 size={14} />
                  </button>
                </div>
              )}
            </div>
          ))}
        </AnimatePresence>
      </div>

      {selected && (
        <PolicyDetailModal policy={selected} isHr={isHr} onClose={() => setSelected(null)} onChanged={load} />
      )}
      {showForm && (
        <PolicyFormModal policy={editingPolicy} onClose={() => setShowForm(false)} onSaved={load} />
      )}
    </div>
  );
}
