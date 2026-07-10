import { api } from "./client";

export type ReimbursementCategory =
  | "travel" | "food" | "accommodation" | "office_supplies"
  | "internet_phone" | "medical" | "training" | "client_entertainment" | "other";

export type ReimbursementStatus =
  | "pending" | "manager_approved" | "manager_rejected"
  | "hr_approved" | "hr_rejected" | "paid";

export interface Reimbursement {
  id: string;
  claim_number: string;
  employee_id: string;
  claimant_name: string;
  category: ReimbursementCategory;
  title: string;
  description: string | null;
  amount: string;
  expense_date: string | null;
  bill_url: string | null;
  status: ReimbursementStatus;
  manager_id: string | null;
  manager_remarks: string | null;
  manager_acted_at: string | null;
  hr_id: string | null;
  hr_remarks: string | null;
  hr_acted_at: string | null;
  paid_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface SubmitClaimRequest {
  claimant_name?: string;
  category: ReimbursementCategory;
  title: string;
  description?: string;
  amount: number;
  expense_date?: string;
  bill_url?: string;
}

export const reimbursementsApi = {
  submit: (data: SubmitClaimRequest) => api.post<Reimbursement>("/reimbursements", data),
  my: () => api.get<Reimbursement[]>("/reimbursements/my"),
  team: () => api.get<Reimbursement[]>("/reimbursements/team"),
  pendingHr: () => api.get<Reimbursement[]>("/reimbursements/pending-hr"),
  listAll: (status?: ReimbursementStatus) =>
    api.get<Reimbursement[]>(`/reimbursements${status ? `?status=${status}` : ""}`),
  get: (id: string) => api.get<Reimbursement>(`/reimbursements/${id}`),
  managerApprove: (id: string, remarks?: string) =>
    api.put<Reimbursement>(`/reimbursements/${id}/manager-approve`, { remarks }),
  managerReject: (id: string, remarks?: string) =>
    api.put<Reimbursement>(`/reimbursements/${id}/manager-reject`, { remarks }),
  hrApprove: (id: string, remarks?: string) =>
    api.put<Reimbursement>(`/reimbursements/${id}/hr-approve`, { remarks }),
  hrReject: (id: string, remarks?: string) =>
    api.put<Reimbursement>(`/reimbursements/${id}/hr-reject`, { remarks }),
  markPaid: (id: string) => api.put<Reimbursement>(`/reimbursements/${id}/mark-paid`),
};
