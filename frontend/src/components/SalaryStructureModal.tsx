import { useEffect, useState } from "react";
import { ChevronDown, ChevronRight, X, TrendingUp, TrendingDown, Wallet, Download, Briefcase, Calendar } from "lucide-react";
import { salaryStructureApi, type SalaryStructure } from "../api/salaryStructure";
import { payslipApi } from "../api/payslip";

interface Props {
  employeeId: string;
  employeeName?: string;
  employeeRole?: string;
  onClose: () => void;
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

function fmt(val: number): string {
  if (!val) return "₹0";
  return `₹${val.toLocaleString("en-IN")}`;
}

export default function SalaryStructureModal({ employeeId, employeeName, employeeRole, onClose }: Props) {
  const [structure, setStructure] = useState<SalaryStructure | null>(null);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState<Set<string>>(new Set(["earnings", "deductions", "employer"]));
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    const fetcher = employeeId === "my" ? salaryStructureApi.getMy() : salaryStructureApi.get(employeeId);
    fetcher.then(setStructure).catch(() => {}).finally(() => setLoading(false));
  }, [employeeId]);

  const toggle = (s: string) => setExpanded((p) => { const n = new Set(p); n.has(s) ? n.delete(s) : n.add(s); return n; });

  const handleDownload = async () => {
    setDownloading(true);
    try {
      await payslipApi.download(employeeId === "my" ? undefined : employeeId);
    } catch { alert("Failed to download. Ensure salary revision exists."); }
    finally { setDownloading(false); }
  };

  const renderSection = (key: string, title: string, icon: React.ReactNode, color: string, components: Record<string, number>, labels: Record<string, string>, total: number) => {
    const entries = Object.entries(components).filter(([_, v]) => v > 0);
    const isOpen = expanded.has(key);
    return (
      <div style={{ border: "1px solid hsl(var(--border))", borderRadius: 12, marginBottom: 12, overflow: "hidden", background: "hsl(var(--card))" }}>
        <button onClick={() => toggle(key)} style={{ width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 16px", border: "none", borderLeft: `3px solid ${color}`, background: "transparent", cursor: "pointer", fontFamily: "inherit" }}>
          <span style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <span style={{ width: 30, height: 30, borderRadius: 8, background: `${color}18`, display: "flex", alignItems: "center", justifyContent: "center", color }}>{icon}</span>
            <span style={{ fontSize: 14, fontWeight: 600, color: "var(--text)" }}>{title}</span>
            <span style={{ fontSize: 12, color: "var(--text-muted)" }}>({entries.length} items)</span>
          </span>
          <span style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <span style={{ fontSize: 14, fontWeight: 700, color }}>{fmt(total)}</span>
            {isOpen ? <ChevronDown size={16} color="var(--text-muted)" /> : <ChevronRight size={16} color="var(--text-muted)" />}
          </span>
        </button>
        {isOpen && (
          <div style={{ padding: "6px 16px 12px", borderTop: "1px solid hsl(var(--border) / 0.5)" }}>
            {entries.length === 0 ? (
              <p style={{ fontSize: 13, color: "var(--text-muted)", margin: "12px 0" }}>No components configured</p>
            ) : (
              <table style={{ width: "100%", fontSize: 13, borderCollapse: "collapse" }}>
                <tbody>
                  {entries.map(([k, v]) => (
                    <tr key={k} style={{ borderBottom: "1px solid hsl(var(--border) / 0.4)" }}>
                      <td style={{ padding: "9px 0", color: "var(--text-secondary)" }}>{labels[k] || k.replace(/_/g, " ")}</td>
                      <td style={{ padding: "9px 0", textAlign: "right", fontWeight: 600, color: "var(--text)", fontVariantNumeric: "tabular-nums" }}>{fmt(v)}</td>
                    </tr>
                  ))}
                  <tr style={{ borderTop: `2px solid ${color}40` }}>
                    <td style={{ padding: "11px 0", fontWeight: 700, color: "var(--text)" }}>Total</td>
                    <td style={{ padding: "11px 0", textAlign: "right", fontWeight: 700, color, fontSize: 14, fontVariantNumeric: "tabular-nums" }}>{fmt(total)}</td>
                  </tr>
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" style={{ maxWidth: 620, maxHeight: "88vh", display: "flex", flexDirection: "column" }} onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div style={{ padding: "16px 20px", borderBottom: "1px solid hsl(var(--border))", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <h2 style={{ margin: 0, fontSize: 17, fontWeight: 700 }}>Salary Structure</h2>
            {employeeName && (
              <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 4 }}>
                <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>{employeeName}</span>
                {employeeRole && <span style={{ fontSize: 11, padding: "1px 6px", borderRadius: 4, background: "hsl(var(--primary) / 0.08)", color: "var(--primary-color)", fontWeight: 500 }}>{employeeRole}</span>}
              </div>
            )}
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            {structure && (
              <button className="btn btn-outline btn-sm" onClick={handleDownload} disabled={downloading} style={{ display: "flex", alignItems: "center", gap: 4 }}>
                <Download size={13} /> {downloading ? "..." : "Payslip"}
              </button>
            )}
            <button className="btn btn-ghost btn-sm" onClick={onClose}><X size={16} /></button>
          </div>
        </div>

        {/* Body */}
        <div style={{ flex: 1, overflowY: "auto", padding: "16px 20px" }}>
          {loading && <p className="muted" style={{ textAlign: "center", padding: 40 }}>Loading...</p>}

          {!loading && !structure && (
            <div style={{ textAlign: "center", padding: 48 }}>
              <Wallet size={44} style={{ color: "var(--text-muted)", opacity: 0.3, marginBottom: 14 }} />
              <p style={{ fontSize: 15, fontWeight: 600, color: "var(--text)", marginBottom: 4 }}>No Salary Structure</p>
              <p style={{ fontSize: 13, color: "var(--text-muted)" }}>HR needs to configure the salary breakdown for this employee via "Edit Structure".</p>
            </div>
          )}

          {!loading && structure && (
            <>
              {/* Summary Grid — hero cards with solid gradient + white text */}
              <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: 12, marginBottom: 14 }}>
                <div style={{ padding: "18px 18px", borderRadius: 14, background: "linear-gradient(135deg, #059669, #10b981)", boxShadow: "0 4px 14px rgba(16,185,129,0.25)" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 11, fontWeight: 700, color: "rgba(255,255,255,0.85)", letterSpacing: "0.04em" }}>
                    <TrendingUp size={13} /> GROSS SALARY
                  </div>
                  <div style={{ fontSize: 24, fontWeight: 800, color: "#fff", marginTop: 6, fontVariantNumeric: "tabular-nums" }}>{fmt(structure.gross_salary)}</div>
                  <div style={{ fontSize: 11, color: "rgba(255,255,255,0.8)" }}>per month</div>
                </div>
                <div style={{ padding: "18px 18px", borderRadius: 14, background: "linear-gradient(135deg, #4f46e5, #6366f1)", boxShadow: "0 4px 14px rgba(79,70,229,0.25)" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 11, fontWeight: 700, color: "rgba(255,255,255,0.85)", letterSpacing: "0.04em" }}>
                    <Wallet size={13} /> NET SALARY
                  </div>
                  <div style={{ fontSize: 24, fontWeight: 800, color: "#fff", marginTop: 6, fontVariantNumeric: "tabular-nums" }}>{fmt(structure.net_salary)}</div>
                  <div style={{ fontSize: 11, color: "rgba(255,255,255,0.8)" }}>take home</div>
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 10, marginBottom: 22 }}>
                <div style={{ padding: "12px", borderRadius: 10, border: "1px solid hsl(var(--border))", background: "hsl(var(--card))", textAlign: "center" }}>
                  <div style={{ fontSize: 10, fontWeight: 700, color: "var(--text-muted)", letterSpacing: "0.03em" }}>DEDUCTIONS</div>
                  <div style={{ fontSize: 15, fontWeight: 700, color: "#dc2626", marginTop: 3, fontVariantNumeric: "tabular-nums" }}>{fmt(structure.total_deductions)}</div>
                </div>
                <div style={{ padding: "12px", borderRadius: 10, border: "1px solid hsl(var(--border))", background: "hsl(var(--card))", textAlign: "center" }}>
                  <div style={{ fontSize: 10, fontWeight: 700, color: "var(--text-muted)", letterSpacing: "0.03em" }}>MONTHLY CTC</div>
                  <div style={{ fontSize: 15, fontWeight: 700, color: "var(--text)", marginTop: 3, fontVariantNumeric: "tabular-nums" }}>{fmt(structure.monthly_ctc)}</div>
                </div>
                <div style={{ padding: "12px", borderRadius: 10, border: "1px solid hsl(var(--border))", background: "hsl(var(--card))", textAlign: "center" }}>
                  <div style={{ fontSize: 10, fontWeight: 700, color: "var(--text-muted)", letterSpacing: "0.03em" }}>ANNUAL CTC</div>
                  <div style={{ fontSize: 15, fontWeight: 700, color: "var(--text)", marginTop: 3, fontVariantNumeric: "tabular-nums" }}>{fmt(structure.annual_ctc)}</div>
                </div>
              </div>

              {/* Sections */}
              {renderSection("earnings", "Earnings", <TrendingUp size={15} />, "#059669", structure.earnings, EARNING_LABELS, structure.gross_salary)}
              {renderSection("deductions", "Deductions", <TrendingDown size={15} />, "#dc2626", structure.deductions, DEDUCTION_LABELS, structure.total_deductions)}
              {renderSection("employer", "Employer Contributions", <Briefcase size={15} />, "#4f46e5", structure.employer_contributions, EMPLOYER_LABELS, structure.employer_cost)}

              <div style={{ marginTop: 8, padding: "11px 14px", borderRadius: 10, background: "hsl(var(--muted) / 0.5)", border: "1px solid hsl(var(--border))", fontSize: 12, color: "var(--text-secondary)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ display: "flex", alignItems: "center", gap: 5 }}><Calendar size={12} /> Effective: {structure.effective_date}</span>
                <span>Employer cost: <strong style={{ color: "var(--text)" }}>{fmt(structure.employer_cost)}</strong>/month</span>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
