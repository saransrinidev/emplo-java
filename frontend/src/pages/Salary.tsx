import { useState, useEffect, type FormEvent } from "react";
import {
  TrendingUp,
  Plus,
  Calendar,
  ArrowUpRight,
  IndianRupee,
  Briefcase,
  ArrowRight,
  TrendingDown,
  ChevronDown,
  ChevronRight,
  Wallet,
  Download,
  Info,
  Sparkles
} from "lucide-react";
import { salaryApi, type SalaryRevision } from "../api/salary";
import { salaryStructureApi, type SalaryStructure } from "../api/salaryStructure";
import { payslipApi } from "../api/payslip";
import { ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import AsyncState from "../components/AsyncState";
import EmptyState from "../components/EmptyState";
import PageHeader from "../components/PageHeader";
import Skeleton from "../components/Skeleton";
import { useToast } from "../components/Toast";
import { useApi } from "../hooks/useApi";

function formatCurrency(value: string | null | undefined): string {
  if (!value) return "—";
  const n = Number(value);
  if (Number.isNaN(n)) return value;
  if (n >= 100000) return `₹${(n / 100000).toFixed(2)}L`;
  return `₹${n.toLocaleString("en-IN")}`;
}

function formatCurrencyFull(value: string | null | undefined): string {
  if (!value) return "—";
  const n = Number(value);
  if (Number.isNaN(n)) return value;
  return `₹${n.toLocaleString("en-IN")}`;
}

function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

function getYear(dateStr: string): string {
  return new Date(dateStr).getFullYear().toString();
}

const EARNING_LABELS: Record<string, string> = {
  basic_pay: "Basic Pay", hra: "House Rent Allowance (HRA)", dearness_allowance: "Dearness Allowance (DA)",
  special_allowance: "Special Allowance", conveyance_allowance: "Conveyance Allowance",
  medical_allowance: "Medical Allowance", internet_allowance: "Internet Allowance",
  telephone_allowance: "Telephone Allowance", food_allowance: "Food Allowance",
  shift_allowance: "Shift Allowance", performance_bonus: "Performance Bonus",
  incentives: "Incentives", overtime: "Overtime", other_allowances: "Other Allowances",
};

const DEDUCTION_LABELS: Record<string, string> = {
  employee_pf: "Employee Provident Fund", employee_esi: "Employee State Insurance (ESI)",
  professional_tax: "Professional Tax", income_tax_tds: "Income Tax (TDS)",
  labour_welfare_fund: "Labour Welfare Fund", nps_employee: "National Pension Scheme",
  insurance_deduction: "Insurance Deduction", loan_recovery: "Loan Recovery",
  advance_recovery: "Advance Recovery", other_deductions: "Other Deductions",
};

const EMPLOYER_LABELS: Record<string, string> = {
  employer_pf: "Employer Provident Fund", employer_esi: "Employer ESI",
  gratuity: "Gratuity", employer_insurance: "Employer Insurance", employer_nps: "Employer NPS",
};

export default function Salary() {
  const { user } = useAuth();
  const isHr = user?.role === "hr_admin";
  const toast = useToast();

  const current = useApi(() => salaryApi.current(), [], "salary:current");
  const history = useApi(() => salaryApi.history(), [], "salary:history");

  const [activeTab, setActiveTab] = useState<"timeline" | "structure">("timeline");
  const [showModal, setShowModal] = useState(false);

  // Salary structure loading states
  const [structure, setStructure] = useState<SalaryStructure | null>(null);
  const [structureLoading, setStructureLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set(["earnings", "deductions", "employer"]));

  const loading = current.loading || history.loading;
  const error = current.error || history.error;
  const rows = history.data ?? [];
  const approvedRows = rows.filter((r) => r.approval_status === "approved");
  const pendingRows = rows.filter((r) => r.approval_status === "pending");

  // Fetch structure only when switching to structure tab
  useEffect(() => {
    if (activeTab === "structure" && !structure) {
      setStructureLoading(true);
      salaryStructureApi.getMy()
        .then(setStructure)
        .catch(() => { })
        .finally(() => setStructureLoading(false));
    }
  }, [activeTab, structure]);

  const toggleSection = (s: string) => {
    setExpandedSections((p) => {
      const n = new Set(p);
      n.has(s) ? n.delete(s) : n.add(s);
      return n;
    });
  };

  const handleDownloadPayslip = async () => {
    setDownloading(true);
    try {
      await payslipApi.download();
    } catch {
      alert("Failed to download. Ensure salary revision exists.");
    } finally {
      setDownloading(false);
    }
  };

  // Calculate annual CTC
  const currentCtc = current.data?.current_salary;
  const latestDate = current.data?.latest_revision_date;

  // Last increment percentage
  const lastIncrement = approvedRows.length > 0 ? approvedRows[0].revision_percentage : null;

  // --- SVG Chart Calculations ---
  const chartData = [...approvedRows].reverse();
  const hasMultiplePoints = chartData.length >= 2;

  // State for active hovered point in the chart
  const [hoveredPoint, setHoveredPoint] = useState<{
    index: number;
    x: number;
    y: number;
    value: string;
    date: string;
  } | null>(null);

  const handleMouseEnterDot = (index: number, x: number, y: number, value: number, date: string) => {
    setHoveredPoint({
      index,
      x,
      y,
      value: formatCurrencyFull(String(value)),
      date: formatDate(date),
    });
  };

  const handleMouseLeaveDot = () => {
    setHoveredPoint(null);
  };

  const svgWidth = 600;
  const svgHeight = 220;
  const paddingLeft = 70;
  const paddingRight = 20;
  const paddingTop = 20;
  const paddingBottom = 40;

  const chartWidth = svgWidth - paddingLeft - paddingRight;
  const chartHeight = svgHeight - paddingTop - paddingBottom;

  const salaries = chartData.map((d) => Number(d.revised_salary));
  const minSalary = salaries.length > 0 ? Math.min(...salaries) : 0;
  const maxSalary = salaries.length > 0 ? Math.max(...salaries) : 0;

  const salaryRange = maxSalary - minSalary;
  // If no range (single point or flat), add padding
  const rangeMultiplier = salaryRange === 0 ? minSalary || 100000 : salaryRange;
  const yMin = Math.max(0, minSalary - rangeMultiplier * 0.15);
  const yMax = maxSalary + rangeMultiplier * 0.15;
  const yRange = yMax - yMin;

  const getCoordinates = () => {
    return chartData.map((d, index) => {
      const x = paddingLeft + (chartData.length > 1 ? (index / (chartData.length - 1)) * chartWidth : chartWidth / 2);
      const val = Number(d.revised_salary);
      const y = paddingTop + chartHeight - ((val - yMin) / yRange) * chartHeight;
      return { x, y, value: val, date: d.effective_date };
    });
  };

  const coords = getCoordinates();

  const linePath = coords.map((c, i) => `${i === 0 ? "M" : "L"} ${c.x} ${c.y}`).join(" ");
  const fillPath = coords.length > 0
    ? `${linePath} L ${coords[coords.length - 1].x} ${paddingTop + chartHeight} L ${coords[0].x} ${paddingTop + chartHeight} Z`
    : "";

  const gridLines = yRange > 0 ? [
    { label: formatCurrency(String(yMax)), y: paddingTop },
    { label: formatCurrency(String(yMin + yRange / 2)), y: paddingTop + chartHeight / 2 },
    { label: formatCurrency(String(yMin)), y: paddingTop + chartHeight },
  ] : [];

  const handleCreated = () => {
    setShowModal(false);
    toast.success("Salary revision proposed successfully!");
    current.refetch();
    history.refetch();
    setStructure(null); // Force reload salary structure tab
  };

  // --- Growth Insights Calculations ---
  const initialSalary = approvedRows.length > 0 ? Number(approvedRows[approvedRows.length - 1].revised_salary) : 0;
  const currentCtcNum = currentCtc ? Number(currentCtc) : 0;
  const totalGain = currentCtcNum - initialSalary;
  const gainPercentage = initialSalary > 0 ? ((totalGain / initialSalary) * 100).toFixed(1) : "0.0";

  const incrementRevisions = approvedRows.filter((r) => r.revision_percentage);
  const avgIncrement = incrementRevisions.length > 0
    ? (incrementRevisions.reduce((acc, curr) => acc + Number(curr.revision_percentage), 0) / incrementRevisions.length).toFixed(1)
    : null;

  return (
    <div>
      <PageHeader
        title="Compensation"
        subtitle="Salary history and growth timeline."
        actions={
          isHr && (
            <button className="btn btn-sm" onClick={() => setShowModal(true)}>
              <Plus size={14} /> New Revision
            </button>
          )
        }
      />

      {loading && (
        <div className="stack">
          <div className="grid grid-cards">
            <Skeleton.Stat />
            <Skeleton.Stat />
            <Skeleton.Stat />
          </div>
          <Skeleton.Card />
        </div>
      )}

      <AsyncState loading={loading} error={error}>
        {/* ─── Premium Key Stat Cards ─── */}
        <div className="compensation-header">
          {/* CTC Card */}
          <div className="compensation-ctc-card">
            <div className="compensation-card-header">
              <div className="compensation-card-label">Current CTC</div>
              <div className="compensation-card-icon-box icon-box-ctc">
                <IndianRupee size={16} />
              </div>
            </div>
            <div className="compensation-ctc-amount">
              {formatCurrencyFull(currentCtc)} <span className="compensation-ctc-period">/ year</span>
            </div>
            <div className="compensation-ctc-effective">
              Effective: {formatDate(latestDate)}
            </div>
          </div>

          {/* Salary Growth Card */}
          <div className="compensation-growth-card">
            <div className="compensation-card-header">
              <div className="compensation-card-label">Last Revision</div>
              <div className="compensation-card-icon-box icon-box-growth">
                <TrendingUp size={16} />
              </div>
            </div>
            <div>
              {lastIncrement ? (
                <div className="compensation-growth-badge">
                  <ArrowUpRight size={20} style={{ marginRight: "2px" }} />
                  +{lastIncrement}%
                </div>
              ) : (
                <div className="compensation-growth-badge compensation-growth-neutral">—</div>
              )}
            </div>
            <div className="compensation-growth-date">
              {lastIncrement ? formatDate(latestDate) : "No revisions recorded"}
            </div>
          </div>

          {/* Monthly Gross Card */}
          <div className="compensation-monthly-card">
            <div className="compensation-card-header">
              <div className="compensation-card-label">Monthly Gross</div>
              <div className="compensation-card-icon-box icon-box-monthly">
                <Briefcase size={16} />
              </div>
            </div>
            <div className="compensation-monthly-amount">
              {currentCtc ? formatCurrencyFull(String(Math.round(Number(currentCtc) / 12))) : "—"}
            </div>
            <div className="compensation-monthly-sub">
              Estimated per month
            </div>
          </div>
        </div>

        {/* ─── Pending Revisions (HR only) ─── */}
        {isHr && pendingRows.length > 0 && (
          <div className="compensation-pending">
            <h3 className="compensation-section-title">
              <Calendar size={16} /> Pending Approval ({pendingRows.length})
            </h3>
            {pendingRows.map((rev) => (
              <PendingRevisionCard
                key={rev.id}
                revision={rev}
                onAction={() => {
                  history.refetch();
                  current.refetch();
                }}
              />
            ))}
          </div>
        )}

        {/* --- Custom Tab Selectors --- */}
        <div className="comp-tabs-container">
          <div className="comp-tabs">
            <button
              className={`comp-tab-btn ${activeTab === "timeline" ? "active" : ""}`}
              onClick={() => setActiveTab("timeline")}
            >
              <TrendingUp size={15} /> Growth & Timeline
            </button>
            <button
              className={`comp-tab-btn ${activeTab === "structure" ? "active" : ""}`}
              onClick={() => setActiveTab("structure")}
            >
              <Briefcase size={15} /> Salary Breakdown
            </button>
          </div>
        </div>

        {/* ─── TAB 1: GROWTH TIMELINE & CHART ─── */}
        {activeTab === "timeline" && (
          <div className="doc-enter-anim">
            {/* SVG Interactive Chart */}
            {hasMultiplePoints && (
              <div className="comp-chart-card">
                <div className="comp-chart-header">
                  <div className="comp-chart-title">CTC Growth Trajectory</div>
                  <span style={{ fontSize: 11, color: "var(--text-muted)", display: "flex", alignItems: "center", gap: 4 }}>
                    <Info size={12} /> Hover nodes to preview values
                  </span>
                </div>

                <div className="comp-chart-svg-wrapper">
                  {/* Chart Tooltip */}
                  {hoveredPoint && (
                    <div
                      className="comp-chart-tooltip"
                      style={{
                        left: hoveredPoint.x,
                        top: hoveredPoint.y - 65,
                        transform: "translateX(-50%)",
                        opacity: 1,
                      }}
                    >
                      <span className="comp-chart-tooltip-amount">{hoveredPoint.value}</span>
                      <span className="comp-chart-tooltip-date">{hoveredPoint.date}</span>
                    </div>
                  )}

                  <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} width="100%" height="100%" style={{ overflow: "visible" }}>
                    <defs>
                      <linearGradient id="salary-chart-gradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="var(--primary-color)" stopOpacity="0.25" />
                        <stop offset="100%" stopColor="var(--primary-color)" stopOpacity="0.00" />
                      </linearGradient>
                    </defs>

                    {/* Horizontal Grid lines */}
                    {gridLines.map((gl, i) => (
                      <g key={i}>
                        <text
                          x={paddingLeft - 10}
                          y={gl.y + 4}
                          textAnchor="end"
                          fill="var(--text-muted)"
                          style={{ fontSize: "10px", fontWeight: "600", fontVariantNumeric: "tabular-nums" }}
                        >
                          {gl.label}
                        </text>
                        <line
                          x1={paddingLeft}
                          y1={gl.y}
                          x2={svgWidth - paddingRight}
                          y2={gl.y}
                          className="comp-chart-grid-line"
                        />
                      </g>
                    ))}

                    {/* Gradient Fill under path */}
                    <path d={fillPath} fill="url(#salary-chart-gradient)" />

                    {/* Main Line path */}
                    <path
                      d={linePath}
                      fill="none"
                      stroke="var(--primary-color)"
                      strokeWidth="3.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      className="comp-chart-line"
                    />

                    {/* Node interactive dots */}
                    {coords.map((c, i) => (
                      <circle
                        key={i}
                        cx={c.x}
                        cy={c.y}
                        r={hoveredPoint?.index === i ? "7" : "5"}
                        fill="var(--surface)"
                        stroke="var(--primary-color)"
                        strokeWidth={hoveredPoint?.index === i ? "4.5" : "3.5"}
                        className={`comp-chart-interactive-dot ${hoveredPoint?.index === i ? "active" : ""}`}
                        onMouseEnter={() => handleMouseEnterDot(i, c.x, c.y, c.value, c.date)}
                        onMouseLeave={handleMouseLeaveDot}
                      />
                    ))}

                    {/* X axis year labels */}
                    {coords.map((c, i) => {
                      const showXLabel = i === 0 || i === coords.length - 1 || coords.length < 5;
                      if (!showXLabel) return null;
                      return (
                        <text
                          key={i}
                          x={c.x}
                          y={svgHeight - 10}
                          textAnchor="middle"
                          fill="var(--text-muted)"
                          style={{ fontSize: "10px", fontWeight: "700" }}
                        >
                          {getYear(c.date)}
                        </text>
                      );
                    })}
                  </svg>
                </div>
              </div>
            )}

            {/* Growth Insights Widgets */}
            {hasMultiplePoints && (
              <div className="comp-insights-grid">
                <div className="comp-insight-card">
                  <div className="comp-insight-icon-box comp-insight-icon-blue">
                    <Sparkles size={18} />
                  </div>
                  <div className="comp-insight-info">
                    <div className="comp-insight-label">Overall CTC Growth</div>
                    <div className="comp-insight-value">+{gainPercentage}% ({formatCurrency(String(totalGain))})</div>
                  </div>
                </div>

                <div className="comp-insight-card">
                  <div className="comp-insight-icon-box comp-insight-icon-green">
                    <TrendingUp size={18} />
                  </div>
                  <div className="comp-insight-info">
                    <div className="comp-insight-label">Avg. Revision Increment</div>
                    <div className="comp-insight-value">{avgIncrement ? `+${avgIncrement}%` : "—"}</div>
                  </div>
                </div>

                <div className="comp-insight-card">
                  <div className="comp-insight-icon-box comp-insight-icon-orange">
                    <Calendar size={18} />
                  </div>
                  <div className="comp-insight-info">
                    <div className="comp-insight-label">Total Adjustments</div>
                    <div className="comp-insight-value">{approvedRows.length} Revisions</div>
                  </div>
                </div>
              </div>
            )}

            {/* Salary Timeline list */}
            <div className="compensation-timeline-section">
              <h3 className="compensation-section-title">
                <TrendingUp size={16} /> Salary Timeline
              </h3>

              {approvedRows.length === 0 ? (
                <EmptyState
                  icon={<IndianRupee size={40} />}
                  title="No salary history"
                  description="Salary revisions will appear here as a growth timeline."
                  variant="compact"
                />
              ) : (
                <div className="compensation-timeline-wrapper">
                  <div className="compensation-timeline-line-connector" />

                  <div className="compensation-timeline">
                    {approvedRows.map((rev, idx) => {
                      const isFirst = idx === 0;
                      const yearLabel = getYear(rev.effective_date);
                      const showYear = idx === 0 || getYear(approvedRows[idx - 1].effective_date) !== yearLabel;

                      return (
                        <div key={rev.id} className="compensation-timeline-item">
                          <div className="compensation-timeline-left">
                            {showYear ? yearLabel : ""}
                          </div>
                          <div className="compensation-timeline-center">
                            <div className={`compensation-timeline-dot ${isFirst ? "active" : ""}`} />
                          </div>
                          <div className="compensation-timeline-right">
                            <div className="compensation-timeline-right-header">
                              {rev.revision_percentage && (
                                <span className="compensation-increment-badge">
                                  <ArrowUpRight size={12} style={{ marginRight: "2px" }} /> +{rev.revision_percentage}% Revision
                                </span>
                              )}
                              {!rev.revision_percentage && idx === approvedRows.length - 1 && (
                                <span className="compensation-initial-badge">Initial Placement</span>
                              )}
                            </div>
                            <div className="compensation-timeline-amounts">
                              {rev.previous_salary && (
                                <span className="compensation-prev">{formatCurrency(rev.previous_salary)}</span>
                              )}
                              {rev.previous_salary && (
                                <span className="compensation-arrow">
                                  <ArrowRight size={14} style={{ display: "inline-block", verticalAlign: "middle", margin: "0 4px" }} />
                                </span>
                              )}
                              <span className="compensation-new">{formatCurrency(rev.revised_salary)}</span>
                            </div>
                            <div className="compensation-timeline-meta">
                              {rev.comments ? (
                                <span className="compensation-reason">{rev.comments}</span>
                              ) : (
                                <span />
                              )}
                              <span className="compensation-effective">Effective: {formatDate(rev.effective_date)}</span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ─── TAB 2: DETAILED BREAKDOWN NATIVE VIEW ─── */}
        {activeTab === "structure" && (
          <div className="doc-enter-anim">
            {structureLoading && (
              <div style={{ textAlign: "center", padding: "4rem 0" }}>
                <div className="spinner" style={{ width: 32, height: 32, margin: "0 auto 12px" }} />
                <p className="muted">Loading salary components...</p>
              </div>
            )}

            {!structureLoading && !structure && (
              <div className="onboarding-empty" style={{ padding: "4rem 2rem" }}>
                <div className="onboarding-empty-icon" style={{ color: "var(--text-muted)", opacity: 0.4, marginBottom: 8 }}>
                  <Wallet size={36} />
                </div>
                <h3>No Salary Structure Configured</h3>
                <p>HR has not yet set up the itemized breakdown (allowances, PF deductions) for your profile.</p>
              </div>
            )}

            {!structureLoading && structure && (
              <div className="comp-structure-grid">
                {/* Left: Collapsible Components Accordion */}
                <div className="comp-struct-accordion-group">
                  {/* Earnings Accordion */}
                  <AccordionSection
                    title="Earnings & Allowances"
                    typeKey="earnings"
                    icon={<TrendingUp size={15} />}
                    accentColor="#059669"
                    components={structure.earnings}
                    labels={EARNING_LABELS}
                    total={structure.gross_salary}
                    isOpen={expandedSections.has("earnings")}
                    onToggle={toggleSection}
                  />

                  {/* Deductions Accordion */}
                  <AccordionSection
                    title="Deductions"
                    typeKey="deductions"
                    icon={<TrendingDown size={15} />}
                    accentColor="#dc2626"
                    components={structure.deductions}
                    labels={DEDUCTION_LABELS}
                    total={structure.total_deductions}
                    isOpen={expandedSections.has("deductions")}
                    onToggle={toggleSection}
                  />

                  {/* Employer cost Accordion */}
                  <AccordionSection
                    title="Employer Contributions"
                    typeKey="employer"
                    icon={<Briefcase size={15} />}
                    accentColor="#4f46e5"
                    components={structure.employer_contributions}
                    labels={EMPLOYER_LABELS}
                    total={structure.employer_cost}
                    isOpen={expandedSections.has("employer")}
                    onToggle={toggleSection}
                  />
                </div>

                {/* Right: Summary Cost Cards */}
                <div className="comp-structure-summary-panel">
                  {/* Net Pay Card */}
                  <div className="comp-struct-hero-card comp-struct-hero-green">
                    <div className="comp-struct-hero-label">
                      <Wallet size={13} /> NET MONTHLY PAY
                    </div>
                    <div className="comp-struct-hero-value">{formatCurrencyFull(String(structure.net_salary))}</div>
                    <div className="comp-struct-hero-sub">Estimated Take-home / month</div>
                  </div>

                  {/* Annual Cost Card */}
                  <div className="comp-struct-hero-card comp-struct-hero-purple">
                    <div className="comp-struct-hero-label">
                      <Briefcase size={13} /> ANNUAL CTC
                    </div>
                    <div className="comp-struct-hero-value">{formatCurrencyFull(String(structure.annual_ctc))}</div>
                    <div className="comp-struct-hero-sub">Total cost to company / year</div>
                  </div>

                  {/* Stat Panel details */}
                  <div className="comp-struct-panel-card">
                    <div className="comp-struct-panel-item">
                      <span className="comp-struct-panel-item-label">Gross Salary</span>
                      <span className="comp-struct-panel-item-value">{formatCurrencyFull(String(structure.gross_salary))}</span>
                    </div>

                    <div className="comp-struct-panel-item">
                      <span className="comp-struct-panel-item-label">Total Deductions</span>
                      <span className="comp-struct-panel-item-value red">-{formatCurrencyFull(String(structure.total_deductions))}</span>
                    </div>

                    <div className="comp-struct-panel-item">
                      <span className="comp-struct-panel-item-label">Employer Cost</span>
                      <span className="comp-struct-panel-item-value">{formatCurrencyFull(String(structure.employer_cost))}</span>
                    </div>
                  </div>

                  {/* Action Link: Download Payslip */}
                  <button
                    className="btn btn-outline"
                    onClick={handleDownloadPayslip}
                    disabled={downloading}
                    style={{ width: "100%", justifyContent: "center", gap: 6, padding: "10px" }}
                  >
                    <Download size={14} /> {downloading ? "Generating..." : "Download Detailed Statement"}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </AsyncState>

      {showModal && (
        <NewRevisionModal
          currentCtc={currentCtc}
          onClose={() => setShowModal(false)}
          onCreated={handleCreated}
        />
      )}
    </div>
  );
}

// ------- Collapsible Accordion Section Helper -------

interface AccordionSectionProps {
  title: string;
  typeKey: string;
  icon: React.ReactNode;
  accentColor: string;
  components: Record<string, number>;
  labels: Record<string, string>;
  total: number;
  isOpen: boolean;
  onToggle: (key: string) => void;
}

function AccordionSection({
  title,
  typeKey,
  icon,
  accentColor,
  components,
  labels,
  total,
  isOpen,
  onToggle,
}: AccordionSectionProps) {
  const entries = Object.entries(components).filter(([_, v]) => v > 0);

  return (
    <div className="comp-struct-section-wrapper">
      <button
        onClick={() => onToggle(typeKey)}
        className="comp-struct-header-btn"
        style={{ borderLeft: `4px solid ${accentColor}` }}
      >
        <span className="comp-struct-title-left">
          <span
            className="comp-struct-icon-badge"
            style={{ backgroundColor: `${accentColor}12`, color: accentColor }}
          >
            {icon}
          </span>
          <span className="comp-struct-title-text">{title}</span>
          <span className="comp-struct-count">({entries.length} items)</span>
        </span>
        <span className="comp-struct-header-right">
          <span className="comp-struct-total-val" style={{ color: accentColor }}>
            {formatCurrencyFull(String(total))}
          </span>
          {isOpen ? <ChevronDown size={16} className="muted" /> : <ChevronRight size={16} className="muted" />}
        </span>
      </button>

      {isOpen && (
        <div className="comp-struct-content">
          {entries.length === 0 ? (
            <p style={{ fontSize: 13, color: "var(--text-muted)", margin: "8px 0" }}>
              No active components configured
            </p>
          ) : (
            <table className="comp-struct-table">
              <tbody>
                {entries.map(([k, v]) => (
                  <tr key={k}>
                    <td className="comp-struct-table-label">{labels[k] || k.replace(/_/g, " ")}</td>
                    <td className="comp-struct-table-value">{formatCurrencyFull(String(v))}</td>
                  </tr>
                ))}
                <tr style={{ borderTop: `2px solid ${accentColor}30` }}>
                  <td style={{ padding: "12px 0 0 0", fontWeight: 700, color: "var(--text)" }}>Total</td>
                  <td
                    style={{
                      padding: "12px 0 0 0",
                      textAlign: "right",
                      fontWeight: 800,
                      color: accentColor,
                      fontSize: "0.95rem",
                    }}
                  >
                    {formatCurrencyFull(String(total))}
                  </td>
                </tr>
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}

/* ─── Pending Revision Card (HR) ─── */

function PendingRevisionCard({ revision, onAction }: { revision: SalaryRevision; onAction: () => void }) {
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  const handleApprove = async () => {
    setLoading(true);
    try {
      await salaryApi.approve(revision.id);
      toast.success("Salary revision approved!");
      onAction();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Failed to approve");
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async () => {
    setLoading(true);
    try {
      await salaryApi.reject(revision.id);
      toast.warning("Salary revision rejected.");
      onAction();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Failed to reject");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="compensation-pending-card">
      <div className="compensation-pending-info">
        <div className="compensation-pending-amounts">
          {formatCurrencyFull(revision.previous_salary)} <span className="compensation-arrow" style={{ display: "inline-flex", alignItems: "center", margin: "0 6px", verticalAlign: "middle" }}><ArrowRight size={14} /></span> <strong>{formatCurrencyFull(revision.revised_salary)}</strong>
          {revision.revision_percentage && (
            <span className="compensation-increment-badge" style={{ marginLeft: 8 }}>
              +{revision.revision_percentage}%
            </span>
          )}
        </div>
        <div className="compensation-pending-meta">
          Effective: {formatDate(revision.effective_date)} · {revision.comments || "No comments"}
        </div>
      </div>
      <div className="compensation-pending-actions">
        <button className="btn btn-sm btn-outline" onClick={handleReject} disabled={loading}>
          Reject
        </button>
        <button className="btn btn-sm" onClick={handleApprove} disabled={loading}>
          Approve
        </button>
      </div>
    </div>
  );
}

/* ─── New Revision Modal ─── */

function NewRevisionModal({
  currentCtc,
  onClose,
  onCreated,
}: {
  currentCtc: string | null | undefined;
  onClose: () => void;
  onCreated: () => void;
}) {
  const currentNum = currentCtc ? Number(currentCtc) : 0;
  const [newCtc, setNewCtc] = useState("");
  const [effectiveDate, setEffectiveDate] = useState("");
  const [reason, setReason] = useState("Annual Appraisal");
  const [remarks, setRemarks] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [employeeId, setEmployeeId] = useState("");

  // Auto-calculate increment
  const newNum = Number(newCtc) || 0;
  const increment = newNum - currentNum;
  const percentage = currentNum > 0 ? ((increment / currentNum) * 100).toFixed(2) : "0.00";

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!newCtc || !effectiveDate) {
      setError("New CTC and effective date are required");
      return;
    }
    if (newNum <= 0) {
      setError("New CTC must be greater than zero");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await salaryApi.addRevision({
        employee_id: employeeId || (undefined as any), // HR should select employee
        effective_date: effectiveDate,
        revised_salary: newCtc,
        previous_salary: currentCtc || undefined,
        comments: `${reason}${remarks ? ` — ${remarks}` : ""}`,
      });
      onCreated();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create revision");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content compensation-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Create Salary Revision</h2>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit} style={{ padding: 24 }}>
          {/* Current CTC display */}
          <div className="compensation-modal-current">
            <span className="compensation-modal-current-label">Current CTC</span>
            <span className="compensation-modal-current-value">{formatCurrencyFull(currentCtc)}</span>
          </div>

          <div className="field">
            <label>Employee ID</label>
            <input
              className="input"
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value)}
              placeholder="UUID of the employee"
            />
          </div>

          <div className="field">
            <label>New CTC *</label>
            <div className="input-with-prefix">
              <span className="input-prefix">₹</span>
              <input
                className="input"
                type="number"
                value={newCtc}
                onChange={(e) => setNewCtc(e.target.value)}
                placeholder="8,00,000"
                min="1"
              />
            </div>
          </div>

          <div className="field">
            <label>Effective Date *</label>
            <input className="input" type="date" value={effectiveDate} onChange={(e) => setEffectiveDate(e.target.value)} />
          </div>

          <div className="field">
            <label>Reason</label>
            <select className="input" value={reason} onChange={(e) => setReason(e.target.value)}>
              <option>Annual Appraisal</option>
              <option>Promotion</option>
              <option>Role Change</option>
              <option>Market Correction</option>
              <option>Retention</option>
              <option>Other</option>
            </select>
          </div>

          <div className="field">
            <label>Remarks</label>
            <textarea className="input" rows={2} value={remarks} onChange={(e) => setRemarks(e.target.value)} placeholder="Additional notes..." />
          </div>

          {/* Auto-calculated increment */}
          {newNum > 0 && (
            <div className="compensation-modal-calc">
              <div className="compensation-modal-calc-row">
                <span>Increment</span>
                <span className={increment >= 0 ? "text-success" : "text-danger"}>
                  {increment >= 0 ? "+" : ""}₹{Math.abs(increment).toLocaleString("en-IN")}
                </span>
              </div>
              <div className="compensation-modal-calc-row">
                <span>Change</span>
                <span className={increment >= 0 ? "text-success" : "text-danger"}>
                  {increment >= 0 ? "+" : ""}{percentage}%
                </span>
              </div>
            </div>
          )}

          {error && <p className="error-text" style={{ marginBottom: 12 }}>{error}</p>}

          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 20 }}>
            <button type="button" className="btn btn-outline btn-sm" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-sm" disabled={submitting}>
              {submitting ? "Saving…" : "Save Revision"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
