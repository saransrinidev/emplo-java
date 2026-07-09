import { useEffect, useState, type FormEvent } from "react";
import { Star, User as UserIcon, Eye, Plus, ThumbsUp, Target, ClipboardList, Users } from "lucide-react";
import { employeesApi, type Employee } from "../api/employees";
import { performanceApi } from "../api/performance";
import type { PerformanceReview } from "../api/performance";
import { useAuth } from "../context/AuthContext";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import { useApi } from "../hooks/useApi";
import { useToast } from "../components/Toast";

// --- Render star bar indicator helper ---
function StarRating({ rating }: { rating: number }) {
  const stars = [];
  for (let i = 1; i <= 5; i++) {
    stars.push(
      <Star
        key={i}
        size={14}
        className={i <= rating ? "star-filled" : "star-empty"}
        style={{ color: i <= rating ? "#f59e0b" : "var(--border)", marginRight: 2 }}
        fill={i <= rating ? "#f59e0b" : "none"}
      />
    );
  }
  return <div className="perf-star-bar" style={{ display: "flex", alignItems: "center" }}>{stars}</div>;
}

// --- Interactive Rating selector (1-5 stars) ---
function InteractiveStarRating({
  value,
  onChange,
}: {
  value: number;
  onChange: (val: number) => void;
}) {
  return (
    <div className="perf-star-select-container">
      {[1, 2, 3, 4, 5].map((i) => (
        <button
          key={i}
          type="button"
          onClick={() => onChange(i)}
          className="perf-star-btn"
          style={{ cursor: "pointer", border: "none", background: "transparent", padding: 4 }}
        >
          <Star
            size={24}
            style={{ color: i <= value ? "#f59e0b" : "var(--border)", transition: "all 0.15s ease" }}
            fill={i <= value ? "#f59e0b" : "none"}
          />
        </button>
      ))}
    </div>
  );
}

function getRatingDescription(rating: string | number) {
  const score = Number(rating);
  if (Number.isNaN(score) || score <= 0) return { label: "No Ratings Yet", className: "desc-meets" };
  if (score >= 4.5) return { label: "Outstanding Performance", className: "desc-outstanding" };
  if (score >= 3.8) return { label: "Exceeds Expectations", className: "desc-exceeds" };
  if (score >= 3.0) return { label: "Meets Expectations", className: "desc-meets" };
  return { label: "Needs Improvement", className: "desc-needs-improvement" };
}

export default function Performance() {
  const { user } = useAuth();
  const isManager = user?.role === "manager";

  return (
    <div className="doc-enter-anim">
      <PageHeader
        title="Performance Reviews"
        subtitle={isManager ? "Submit employee evaluations and monitor team ratings." : "Your review history, goals, and feedback summaries."}
      />
      <MyPerformance />
      {isManager && <TeamPerformance />}
    </div>
  );
}

// ------- My Performance -------

function MyPerformance() {
  const { data, loading, error } = useApi(() => performanceApi.list(), [], "performance:my");
  const reviews = data ?? [];

  // Compute average rating
  const ratings = reviews.map((r) => Number(r.rating)).filter((n) => !Number.isNaN(n) && n > 0);
  const avgRating = ratings.length > 0 ? (ratings.reduce((acc, curr) => acc + curr, 0) / ratings.length).toFixed(1) : null;
  const ratingDesc = avgRating ? getRatingDescription(avgRating) : null;

  return (
    <div style={{ marginBottom: 40 }}>
      <h2 style={{ marginBottom: 16, fontSize: 16, fontWeight: 700 }}>My Overview</h2>
      <AsyncState loading={loading} error={error}>
        {reviews.length === 0 ? (
          <div className="onboarding-empty" style={{ padding: "3rem 1.5rem" }}>
            <div className="onboarding-empty-icon" style={{ color: "var(--text-muted)", opacity: 0.4, marginBottom: 8 }}>
              <Star size={36} />
            </div>
            <h3>No Reviews Logged</h3>
            <p>Your performance reviews will appear here once submitted by your manager.</p>
          </div>
        ) : (
          <div>
            {/* Rating Banner Dashboard stats */}
            <div className="perf-rating-banner">
              <div className="perf-rating-hero-card">
                <div className="perf-rating-large-num">
                  {avgRating} <span className="perf-rating-large-scale">/ 5.0</span>
                </div>
                {avgRating && <div className="perf-rating-stars-wrapper"><StarRating rating={Math.round(Number(avgRating))} /></div>}
                {ratingDesc && <span className={`perf-rating-desc ${ratingDesc.className}`}>{ratingDesc.label}</span>}
              </div>

              <div className="perf-stats-card">
                <div className="perf-stat-item">
                  <span className="perf-stat-label">Total Reviews</span>
                  <span className="perf-stat-value">{reviews.length} Completed</span>
                  <span className="perf-stat-sub">Across all periods</span>
                </div>

                <div className="perf-stat-item">
                  <span className="perf-stat-label">Latest review</span>
                  <span className="perf-stat-value">{reviews[0]?.review_period ?? "—"}</span>
                  <span className="perf-stat-sub">Rated: {reviews[0]?.rating ? `${reviews[0].rating}/5` : "—"}</span>
                </div>
              </div>
            </div>

            <h3 style={{ marginBottom: 16, fontSize: 15, fontWeight: 700 }}>Feedback History</h3>
            <div className="stack">
              {reviews.map((review) => (
                <ReviewCard key={review.id} review={review} />
              ))}
            </div>
          </div>
        )}
      </AsyncState>
    </div>
  );
}

// ------- Review Card -------

function ReviewCard({ review }: { review: PerformanceReview }) {
  const ratingNum = Number(review.rating) || 0;

  return (
    <div className="perf-feedback-card">
      <div className="perf-feedback-header">
        <h4 className="perf-period-title">{review.review_period ?? "General Review"}</h4>
        {review.rating && <StarRating rating={ratingNum} />}
      </div>

      <div className="perf-reviewer-meta">
        <UserIcon size={13} /> Evaluated by <strong>{review.reviewer_name ?? "Manager"}</strong> on {review.review_date ?? "—"}
      </div>

      <div className="perf-journal-grid">
        {/* Strengths card block */}
        <div className="perf-journal-block block-strengths">
          <div className="perf-journal-block-title title-strengths">
            <ThumbsUp size={13} /> Key Strengths
          </div>
          <p className="perf-journal-block-text">{review.strengths || "No strengths logged for this period."}</p>
        </div>

        {/* Improvements card block */}
        <div className="perf-journal-block block-improvements">
          <div className="perf-journal-block-title title-improvements">
            <Target size={13} /> Areas for Improvement
          </div>
          <p className="perf-journal-block-text">{review.areas_for_improvement || "No areas for improvement logged."}</p>
        </div>
      </div>

      {/* General Remarks */}
      {review.comments && (
        <div className="perf-comments-section">
          <strong>Reviewer Remarks:</strong> {review.comments}
        </div>
      )}
    </div>
  );
}

// ------- Team Performance (Manager View) -------

function TeamPerformance() {
  const [team, setTeam] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewTarget, setViewTarget] = useState<Employee | null>(null);

  useEffect(() => {
    employeesApi
      .list()
      .then(setTeam)
      .catch(() => { })
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h2 style={{ marginBottom: 16, fontSize: 16, fontWeight: 700 }}>Team Performance Dashboard</h2>
      {loading && (
        <div style={{ textAlign: "center", padding: "2rem 0" }}>
          <div className="spinner" style={{ width: 24, height: 24, margin: "0 auto 8px" }} />
          <p className="muted">Loading direct reports...</p>
        </div>
      )}

      {!loading && team.length === 0 && (
        <div className="onboarding-empty" style={{ padding: "2.5rem 1.5rem" }}>
          <div className="onboarding-empty-icon" style={{ color: "var(--text-muted)", opacity: 0.4, marginBottom: 8 }}>
            <Users size={36} />
          </div>
          <h3>No Direct Reports</h3>
          <p>No team members are currently assigned under your supervision.</p>
        </div>
      )}

      {!loading && team.length > 0 && (
        <div className="grid" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: 14 }}>
          {team.map((emp) => (
            <div
              key={emp.id}
              className="perf-team-card"
              onClick={() => setViewTarget(emp)}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 12, minWidth: 0, flex: 1 }}>
                <div className="perf-team-avatar-box">
                  <UserIcon size={18} />
                </div>
                <div className="perf-team-details">
                  <div className="perf-team-name">{emp.full_name}</div>
                  <div className="perf-team-meta">
                    {emp.employee_code} • {emp.designation ?? "Team Member"}
                  </div>
                </div>
              </div>
              <Eye size={15} style={{ color: "var(--text-muted)", flexShrink: 0 }} />
            </div>
          ))}
        </div>
      )}

      {/* View Reviews Modal */}
      {viewTarget && (
        <ViewReviewsModal
          employee={viewTarget}
          onClose={() => setViewTarget(null)}
        />
      )}
    </div>
  );
}

// ------- View Reviews Modal (Managers can view and write new reviews) -------

function ViewReviewsModal({
  employee,
  onClose,
}: {
  employee: Employee;
  onClose: () => void;
}) {
  const toast = useToast();
  const [reviews, setReviews] = useState<PerformanceReview[]>([]);
  const [loading, setLoading] = useState(true);

  // Tab state inside modal
  const [modalTab, setModalTab] = useState<"history" | "new">("history");

  // Form states for creating a review
  const [period, setPeriod] = useState("Q1 2026");
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [rating, setRating] = useState(4);
  const [strengths, setStrengths] = useState("");
  const [improvements, setImprovements] = useState("");
  const [comments, setComments] = useState("");
  const [saving, setSaving] = useState(false);

  const fetchReviews = () => {
    setLoading(true);
    performanceApi
      .list(employee.id)
      .then(setReviews)
      .catch(() => { })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchReviews();
  }, [employee.id]);

  const handleCreateReview = async (e: FormEvent) => {
    e.preventDefault();
    if (!period || !date) {
      alert("Please fill in the review period and date.");
      return;
    }

    setSaving(true);
    try {
      await performanceApi.add({
        employee_id: employee.id,
        review_period: period,
        review_date: date,
        rating: String(rating),
        strengths,
        areas_for_improvement: improvements,
        comments,
      });
      toast.success("Review logged successfully!");

      // Reset form fields
      setStrengths("");
      setImprovements("");
      setComments("");
      setRating(4);

      // Re-fetch review list and toggle back to history
      fetchReviews();
      setModalTab("history");
    } catch {
      alert("Failed to submit review.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" style={{ maxWidth: 580, maxHeight: "90vh", display: "flex", flexDirection: "column" }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h2 style={{ margin: 0 }}>Team Feedback Manager</h2>
            <p className="muted" style={{ margin: "2px 0 0 0", fontSize: 12 }}>Evaluating: {employee.full_name}</p>
          </div>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>

        {/* Tab Selection */}
        <div style={{ padding: "10px 20px 0" }}>
          <div className="perf-modal-tab-bar">
            <button
              className={`perf-modal-tab ${modalTab === "history" ? "active" : ""}`}
              onClick={() => setModalTab("history")}
            >
              <ClipboardList size={13} style={{ marginRight: 4, verticalAlign: "middle" }} /> History
            </button>
            <button
              className={`perf-modal-tab ${modalTab === "new" ? "active" : ""}`}
              onClick={() => setModalTab("new")}
            >
              <Plus size={13} style={{ marginRight: 4, verticalAlign: "middle" }} /> Propose Evaluation
            </button>
          </div>
        </div>

        {/* Scrollable Modal Content Body */}
        <div style={{ flex: 1, overflowY: "auto", padding: "10px 20px 20px" }}>

          {/* TAB 1: HISTORY */}
          {modalTab === "history" && (
            <div>
              {loading && (
                <div style={{ textAlign: "center", padding: "2rem 0" }}>
                  <div className="spinner" style={{ width: 24, height: 24, margin: "0 auto 8px" }} />
                  <p className="muted">Loading previous reviews...</p>
                </div>
              )}
              {!loading && reviews.length === 0 && (
                <div style={{ textAlign: "center", padding: "32px 16px" }}>
                  <Star size={32} style={{ color: "var(--text-muted)", opacity: 0.3, marginBottom: 12 }} />
                  <p style={{ color: "var(--text-secondary)", fontSize: 14 }}>
                    No evaluations logged for {employee.full_name} yet.
                  </p>
                </div>
              )}
              {!loading && reviews.length > 0 && (
                <div className="stack">
                  {reviews.map((review) => (
                    <div key={review.id} style={{ paddingBottom: 16, borderBottom: "1px solid hsl(var(--border) / 0.5)", marginBottom: 16 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                        <strong style={{ fontSize: 14 }}>{review.review_period}</strong>
                        {review.rating && <StarRating rating={Number(review.rating)} />}
                      </div>
                      <div className="detail-grid" style={{ gap: 8, gridTemplateColumns: "1fr" }}>
                        <div className="detail-item">
                          <div className="detail-label">Review Date & Reviewer</div>
                          <div className="detail-value">{review.review_date} · By {review.reviewer_name}</div>
                        </div>
                        {review.strengths && (
                          <div className="detail-item">
                            <div className="detail-label">Strengths</div>
                            <div className="detail-value" style={{ fontSize: 13, color: "var(--text-secondary)" }}>{review.strengths}</div>
                          </div>
                        )}
                        {review.areas_for_improvement && (
                          <div className="detail-item">
                            <div className="detail-label">Areas for Improvement</div>
                            <div className="detail-value" style={{ fontSize: 13, color: "var(--text-secondary)" }}>{review.areas_for_improvement}</div>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* TAB 2: PROPOSE EVALUATION FORM */}
          {modalTab === "new" && (
            <form onSubmit={handleCreateReview} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                <div className="field">
                  <label>Review Period *</label>
                  <select className="input" value={period} onChange={(e) => setPeriod(e.target.value)}>
                    <option>Q1 2026</option>
                    <option>Q2 2026</option>
                    <option>Q3 2026</option>
                    <option>Q4 2026</option>
                    <option>H1 2026</option>
                    <option>H2 2026</option>
                    <option>Annual 2026</option>
                  </select>
                </div>
                <div className="field">
                  <label>Date of Evaluation *</label>
                  <input className="input" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
                </div>
              </div>

              <div className="field">
                <label>Overall Score Rating *</label>
                <InteractiveStarRating value={rating} onChange={setRating} />
              </div>

              <div className="field">
                <label>Key Strengths & Achievements</label>
                <textarea
                  className="input"
                  rows={2}
                  value={strengths}
                  onChange={(e) => setStrengths(e.target.value)}
                  placeholder="e.g. Promotes collaboration, delivered API project on schedule..."
                />
              </div>

              <div className="field">
                <label>Areas for Development & Objectives</label>
                <textarea
                  className="input"
                  rows={2}
                  value={improvements}
                  onChange={(e) => setImprovements(e.target.value)}
                  placeholder="e.g. Enhance unit test coverage, improve public documentation..."
                />
              </div>

              <div className="field">
                <label>General Remarks & Comments</label>
                <textarea
                  className="input"
                  rows={2}
                  value={comments}
                  onChange={(e) => setComments(e.target.value)}
                  placeholder="General notes on overall performance..."
                />
              </div>

              <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 8 }}>
                <button type="button" className="btn btn-outline btn-sm" onClick={() => setModalTab("history")}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-sm" disabled={saving}>
                  {saving ? "Saving Evaluation..." : "Submit Review"}
                </button>
              </div>
            </form>
          )}

        </div>

        {/* Modal Footer */}
        <div style={{ padding: "12px 20px", borderTop: "1px solid hsl(var(--border))", display: "flex", justifyContent: "flex-end", gap: 8 }}>
          <button className="btn btn-outline btn-sm" onClick={onClose}>Close Portal</button>
        </div>
      </div>
    </div>
  );
}
