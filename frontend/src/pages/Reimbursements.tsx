import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Plus, X, Receipt, Upload, Check, XCircle, Clock,
  ShieldCheck, Banknote, User as UserIcon, Calendar,
  ChevronRight, FileText, Plane, Utensils, Building2,
  Package, Wifi, HeartPulse, GraduationCap, Users2, MoreHorizontal,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import {
  reimbursementsApi,
  type Reimbursement,
  type ReimbursementCategory,
  type ReimbursementStatus,
} from "../api/reimbursements";
import { uploadFile } from "../api/upload";

// ─── Category metadata ────────────────────────────────────────────────────────

const CATEGORY_META: Record<ReimbursementCategory, { label: string; icon: React.ComponentType<any>; color: string }> = {
  travel: { label: "Travel", icon: Plane, color: "#3b82f6" },
  food: { label: "Food", icon: Utensils, color: "#f59e0b" },
  accommodation: { label: "Accommodation", icon: Building2, color: "#8b5cf6" },
  office_supplies: { label: "Office Supplies", icon: Package, color: "#06b6d4" },
  internet_phone: { label: "Internet / Phone", icon: Wifi, color: "#10b981" },
  medical: { label: "Medical", icon: HeartPulse, color: "#ef4444" },
  training: { label: "Training", icon: GraduationCap, color: "#ec4899" },
  client_entertainment: { label: "Client Entertainment", icon: Users2, color: "#6366f1" },
  other: { label: "Other", icon: MoreHorizontal, color: "#6b7280" },
};

const STATUS_META: Record<ReimbursementStatus, { label: string; color: string; bg: string }> = {
  pending: { label: "Awaiting Manager", color: "#d97706", bg: "#fef3c7" },
  manager_approved: { label: "Awaiting HR", color: "#2563eb", bg: "#dbeafe" },
  manager_rejected: { label: "Rejected by Manager", color: "#dc2626", bg: "#fee2e2" },
  hr_approved: { label: "Approved", color: "#16a34a", bg: "#dcfce7" },
  hr_rejected: { label: "Rejected by HR", color: "#dc2626", bg: "#fee2e2" },
  paid: { label: "Paid", color: "#0891b2", bg: "#cffafe" },
};

function money(v: string | number): string {
  const n = Number(v);
  return `₹${n.toLocaleString("en-IN")}`;
}

function formatDate(d: string | null): string {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

// ─── Status Badge ─────────────────────────────────────────────────────────────

function StatusBadge({ status }: { status: ReimbursementStatus }) {
  const meta = STATUS_META[status];
  return (
    <span className="reimb-status-badge" style={{ color: meta.color, backgroundColor: meta.bg }}>
      {meta.label}
    </span>
  );
}

// ─── Claim Card ────────────────────────────────────────────────────────────────

function ClaimCard({
  claim,
  onClick,
}: {
  claim: Reimbursement;
  onClick: () => void;
}) {
  const catMeta = CATEGORY_META[claim.category];
  const Icon = catMeta.icon;

  return (
    <motion.div
      className="reimb-card"
      onClick={onClick}
      whileHover={{ y: -2 }}
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <div className="reimb-card-icon" style={{ backgroundColor: `${catMeta.color}15`, color: catMeta.color }}>
        <Icon size={18} />
      </div>
      <div className="reimb-card-body">
        <div className="reimb-card-top">
          <span className="reimb-card-title">{claim.title}</span>
          <span className="reimb-card-amount">{money(claim.amount)}</span>
        </div>
        <div className="reimb-card-meta">
          <span className="reimb-claim-number">{claim.claim_number}</span>
          <span className="reimb-dot">•</span>
          <span>{claim.claimant_name}</span>
          <span className="reimb-dot">•</span>
          <span>{formatDate(claim.expense_date)}</span>
        </div>
      </div>
      <StatusBadge status={claim.status} />
      <ChevronRight size={16} className="reimb-card-chevron" />
    </motion.div>
  );
}

// ─── Submit Claim Modal ───────────────────────────────────────────────────────

function SubmitClaimModal({ onClose, onSubmitted }: { onClose: () => void; onSubmitted: () => void }) {
  const { user } = useAuth();
  const [claimantName, setClaimantName] = useState(user?.name || "");
  const [category, setCategory] = useState<ReimbursementCategory>("travel");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [expenseDate, setExpenseDate] = useState("");
  const [billUrl, setBillUrl] = useState("");
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const res = await uploadFile(file);
      setBillUrl(res.url);
    } catch {
      setError("Failed to upload bill. Please try again.");
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async () => {
    setError("");
    if (!title.trim()) {
      setError("Please enter a title for this expense.");
      return;
    }
    const amt = parseFloat(amount);
    if (!amt || amt <= 0) {
      setError("Please enter a valid amount.");
      return;
    }
    setSubmitting(true);
    try {
      await reimbursementsApi.submit({
        claimant_name: claimantName || undefined,
        category,
        title: title.trim(),
        description: description || undefined,
        amount: amt,
        expense_date: expenseDate || undefined,
        bill_url: billUrl || undefined,
      });
      onSubmitted();
      onClose();
    } catch (err: any) {
      setError(err.message || "Failed to submit claim.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <motion.div
        className="reimb-modal"
        onClick={(e) => e.stopPropagation()}
        initial={{ opacity: 0, scale: 0.96, y: 10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
      >
        <div className="reimb-modal-header">
          <div>
            <h2>New Expense Claim</h2>
            <p>Submit a bill for reimbursement</p>
          </div>
          <button className="reimb-icon-btn" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="reimb-modal-body">
          <div className="reimb-form-grid">
            <div className="reimb-field">
              <label>Your Name</label>
              <input value={claimantName} onChange={(e) => setClaimantName(e.target.value)} placeholder="Full name" />
            </div>
            <div className="reimb-field">
              <label>Category</label>
              <select value={category} onChange={(e) => setCategory(e.target.value as ReimbursementCategory)}>
                {Object.entries(CATEGORY_META).map(([key, meta]) => (
                  <option key={key} value={key}>{meta.label}</option>
                ))}
              </select>
            </div>
            <div className="reimb-field full-width">
              <label>Expense Title *</label>
              <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Taxi fare to client site" />
            </div>
            <div className="reimb-field">
              <label>Amount (₹) *</label>
              <input type="number" min="0" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" />
            </div>
            <div className="reimb-field">
              <label>Expense Date</label>
              <input type="date" value={expenseDate} onChange={(e) => setExpenseDate(e.target.value)} />
            </div>
            <div className="reimb-field full-width">
              <label>Description</label>
              <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} placeholder="Add any extra context..." />
            </div>
            <div className="reimb-field full-width">
              <label>Upload Bill / Receipt</label>
              <div className="reimb-upload-box">
                {billUrl ? (
                  <div className="reimb-upload-success">
                    <FileText size={16} />
                    <span>Bill uploaded</span>
                    <button onClick={() => setBillUrl("")}><X size={14} /></button>
                  </div>
                ) : (
                  <label className="reimb-upload-label">
                    <Upload size={16} />
                    {uploading ? "Uploading..." : "Choose file"}
                    <input type="file" accept="image/*,.pdf" onChange={handleFileUpload} disabled={uploading} hidden />
                  </label>
                )}
              </div>
            </div>
          </div>

          {error && <p className="reimb-error">{error}</p>}
        </div>

        <div className="reimb-modal-footer">
          <button className="reimb-btn-secondary" onClick={onClose}>Cancel</button>
          <button className="reimb-btn-primary" onClick={handleSubmit} disabled={submitting}>
            {submitting ? "Submitting..." : "Submit Claim"}
          </button>
        </div>
      </motion.div>
    </div>
  );
}

// ─── Claim Detail Modal ───────────────────────────────────────────────────────

function ClaimDetailModal({
  claim,
  role,
  onClose,
  onUpdated,
}: {
  claim: Reimbursement;
  role: string;
  onClose: () => void;
  onUpdated: () => void;
}) {
  const [remarks, setRemarks] = useState("");
  const [acting, setActing] = useState(false);
  const catMeta = CATEGORY_META[claim.category];
  const Icon = catMeta.icon;

  const canManagerAct = role === "manager" && claim.status === "pending";
  const canHrAct = role === "hr_admin" && claim.status === "manager_approved";
  const canMarkPaid = role === "hr_admin" && claim.status === "hr_approved";

  const act = async (fn: () => Promise<any>) => {
    setActing(true);
    try {
      await fn();
      onUpdated();
      onClose();
    } catch {
      // swallow — could add toast
    } finally {
      setActing(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <motion.div
        className="reimb-modal reimb-detail-modal"
        onClick={(e) => e.stopPropagation()}
        initial={{ opacity: 0, scale: 0.96, y: 10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
      >
        <div className="reimb-modal-header">
          <div className="reimb-detail-title-row">
            <div className="reimb-card-icon" style={{ backgroundColor: `${catMeta.color}15`, color: catMeta.color }}>
              <Icon size={20} />
            </div>
            <div>
              <h2>{claim.title}</h2>
              <p>{claim.claim_number} • {catMeta.label}</p>
            </div>
          </div>
          <button className="reimb-icon-btn" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="reimb-modal-body">
          <div className="reimb-detail-stats">
            <div className="reimb-detail-stat">
              <span className="reimb-detail-stat-label">Amount</span>
              <span className="reimb-detail-stat-value">{money(claim.amount)}</span>
            </div>
            <div className="reimb-detail-stat">
              <span className="reimb-detail-stat-label">Status</span>
              <StatusBadge status={claim.status} />
            </div>
            <div className="reimb-detail-stat">
              <span className="reimb-detail-stat-label">Claimant</span>
              <span className="reimb-detail-stat-value-sm"><UserIcon size={12} /> {claim.claimant_name}</span>
            </div>
            <div className="reimb-detail-stat">
              <span className="reimb-detail-stat-label">Expense Date</span>
              <span className="reimb-detail-stat-value-sm"><Calendar size={12} /> {formatDate(claim.expense_date)}</span>
            </div>
          </div>

          {claim.description && (
            <div className="reimb-detail-section">
              <h4>Description</h4>
              <p>{claim.description}</p>
            </div>
          )}

          {claim.bill_url && (
            <div className="reimb-detail-section">
              <h4>Attached Bill</h4>
              <a href={claim.bill_url} target="_blank" rel="noreferrer" className="reimb-bill-link">
                <FileText size={14} /> View receipt
              </a>
            </div>
          )}

          {/* Approval Timeline */}
          <div className="reimb-timeline">
            <div className={`reimb-timeline-step ${claim.status !== "pending" ? "done" : "active"}`}>
              <span className="reimb-timeline-dot"><UserIcon size={12} /></span>
              <div>
                <span className="reimb-timeline-label">Submitted by Employee</span>
                <span className="reimb-timeline-date">{formatDate(claim.created_at)}</span>
              </div>
            </div>
            <div className={`reimb-timeline-step ${
              claim.status === "pending" ? "" :
              claim.status === "manager_rejected" ? "rejected" : "done"
            }`}>
              <span className="reimb-timeline-dot"><ShieldCheck size={12} /></span>
              <div>
                <span className="reimb-timeline-label">Manager Assurance</span>
                {claim.manager_acted_at ? (
                  <>
                    <span className="reimb-timeline-date">{formatDate(claim.manager_acted_at)}</span>
                    {claim.manager_remarks && <p className="reimb-timeline-remarks">"{claim.manager_remarks}"</p>}
                  </>
                ) : (
                  <span className="reimb-timeline-date">Pending</span>
                )}
              </div>
            </div>
            <div className={`reimb-timeline-step ${
              ["pending", "manager_approved", "manager_rejected"].includes(claim.status) ? "" :
              claim.status === "hr_rejected" ? "rejected" : "done"
            }`}>
              <span className="reimb-timeline-dot"><Check size={12} /></span>
              <div>
                <span className="reimb-timeline-label">HR Final Decision</span>
                {claim.hr_acted_at ? (
                  <>
                    <span className="reimb-timeline-date">{formatDate(claim.hr_acted_at)}</span>
                    {claim.hr_remarks && <p className="reimb-timeline-remarks">"{claim.hr_remarks}"</p>}
                  </>
                ) : (
                  <span className="reimb-timeline-date">Pending</span>
                )}
              </div>
            </div>
            {claim.status === "hr_approved" || claim.status === "paid" ? (
              <div className={`reimb-timeline-step ${claim.status === "paid" ? "done" : ""}`}>
                <span className="reimb-timeline-dot"><Banknote size={12} /></span>
                <div>
                  <span className="reimb-timeline-label">Payment Processed</span>
                  <span className="reimb-timeline-date">{claim.paid_at ? formatDate(claim.paid_at) : "Pending"}</span>
                </div>
              </div>
            ) : null}
          </div>

          {(canManagerAct || canHrAct) && (
            <div className="reimb-field full-width" style={{ marginTop: 16 }}>
              <label>Remarks (optional)</label>
              <textarea value={remarks} onChange={(e) => setRemarks(e.target.value)} rows={2} placeholder="Add a note..." />
            </div>
          )}
        </div>

        <div className="reimb-modal-footer">
          {canManagerAct && (
            <>
              <button className="reimb-btn-reject" disabled={acting} onClick={() => act(() => reimbursementsApi.managerReject(claim.id, remarks))}>
                <XCircle size={15} /> Reject
              </button>
              <button className="reimb-btn-approve" disabled={acting} onClick={() => act(() => reimbursementsApi.managerApprove(claim.id, remarks))}>
                <ShieldCheck size={15} /> Give Assurance
              </button>
            </>
          )}
          {canHrAct && (
            <>
              <button className="reimb-btn-reject" disabled={acting} onClick={() => act(() => reimbursementsApi.hrReject(claim.id, remarks))}>
                <XCircle size={15} /> Reject
              </button>
              <button className="reimb-btn-approve" disabled={acting} onClick={() => act(() => reimbursementsApi.hrApprove(claim.id, remarks))}>
                <Check size={15} /> Final Approve
              </button>
            </>
          )}
          {canMarkPaid && (
            <button className="reimb-btn-approve" disabled={acting} onClick={() => act(() => reimbursementsApi.markPaid(claim.id))}>
              <Banknote size={15} /> Mark as Paid
            </button>
          )}
          {!canManagerAct && !canHrAct && !canMarkPaid && (
            <button className="reimb-btn-secondary" onClick={onClose}>Close</button>
          )}
        </div>
      </motion.div>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function Reimbursements() {
  const { user } = useAuth();
  const [tab, setTab] = useState<"my" | "team" | "hr">("my");
  const [claims, setClaims] = useState<Reimbursement[]>([]);
  const [loading, setLoading] = useState(true);
  const [showSubmit, setShowSubmit] = useState(false);
  const [selected, setSelected] = useState<Reimbursement | null>(null);

  const isManager = user?.role === "manager";
  const isHr = user?.role === "hr_admin";

  useEffect(() => {
    if (isHr && tab === "my") setTab("hr");
    if (isManager && tab === "my") setTab("my");
  }, [isHr, isManager]);

  const load = async () => {
    setLoading(true);
    try {
      let data: Reimbursement[] = [];
      if (tab === "my") data = await reimbursementsApi.my();
      else if (tab === "team") data = await reimbursementsApi.team();
      else if (tab === "hr") data = await reimbursementsApi.pendingHr();
      setClaims(data);
    } catch {
      setClaims([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab]);

  const totalPending = claims.filter((c) => !["hr_approved", "hr_rejected", "manager_rejected", "paid"].includes(c.status)).length;
  const totalAmount = claims.reduce((sum, c) => sum + Number(c.amount), 0);

  return (
    <div className="reimb-page">
      <div className="reimb-header">
        <div>
          <h1><Receipt size={22} style={{ verticalAlign: "middle", marginRight: 8 }} />Reimbursements</h1>
          <p>Submit and track expense claims</p>
        </div>
        {!isHr && (
          <button className="reimb-btn-primary" onClick={() => setShowSubmit(true)}>
            <Plus size={16} /> New Claim
          </button>
        )}
      </div>

      <div className="reimb-summary-row">
        <div className="reimb-summary-card">
          <span className="reimb-summary-value">{claims.length}</span>
          <span className="reimb-summary-label">Total Claims</span>
        </div>
        <div className="reimb-summary-card">
          <span className="reimb-summary-value" style={{ color: "#d97706" }}>{totalPending}</span>
          <span className="reimb-summary-label">In Progress</span>
        </div>
        <div className="reimb-summary-card">
          <span className="reimb-summary-value">{money(totalAmount)}</span>
          <span className="reimb-summary-label">Total Value</span>
        </div>
      </div>

      <div className="reimb-tabs">
        {!isHr && <button className={`reimb-tab ${tab === "my" ? "active" : ""}`} onClick={() => setTab("my")}>My Claims</button>}
        {(isManager || isHr) && <button className={`reimb-tab ${tab === "team" ? "active" : ""}`} onClick={() => setTab("team")}>{isHr ? "All Claims" : "Team Claims"}</button>}
        {isHr && <button className={`reimb-tab ${tab === "hr" ? "active" : ""}`} onClick={() => setTab("hr")}>Awaiting HR Review</button>}
      </div>

      <div className="reimb-list">
        {loading && <p className="muted" style={{ padding: 24, textAlign: "center" }}>Loading...</p>}
        {!loading && claims.length === 0 && (
          <div className="reimb-empty">
            <Receipt size={40} />
            <h3>No claims here</h3>
            <p>{tab === "my" ? "Submit your first expense claim to get started." : "Nothing to review right now."}</p>
          </div>
        )}
        <AnimatePresence>
          {claims.map((claim) => (
            <ClaimCard key={claim.id} claim={claim} onClick={() => setSelected(claim)} />
          ))}
        </AnimatePresence>
      </div>

      {showSubmit && <SubmitClaimModal onClose={() => setShowSubmit(false)} onSubmitted={load} />}
      {selected && (
        <ClaimDetailModal
          claim={selected}
          role={user?.role || ""}
          onClose={() => setSelected(null)}
          onUpdated={load}
        />
      )}
    </div>
  );
}
