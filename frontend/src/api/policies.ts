import { api } from "./client";

export type PolicyCategory =
  | "code_of_conduct" | "leave_policy" | "attendance_policy" | "compensation_benefits"
  | "it_security" | "health_safety" | "anti_harassment" | "travel_expense"
  | "remote_work" | "general";

export interface Policy {
  id: string;
  title: string;
  category: PolicyCategory;
  content: string;
  attachment_url: string | null;
  version: number;
  requires_acknowledgement: boolean;
  is_published: boolean;
  created_by: string | null;
  created_at: string;
  updated_at: string;
}

export interface CreatePolicyRequest {
  title: string;
  category: PolicyCategory;
  content: string;
  attachment_url?: string;
  requires_acknowledgement?: boolean;
  is_published?: boolean;
}

export const policiesApi = {
  list: (category?: PolicyCategory) =>
    api.get<Policy[]>(`/policies${category ? `?category=${category}` : ""}`),
  listAll: () => api.get<Policy[]>("/policies/all"),
  pendingAcknowledgement: () => api.get<Policy[]>("/policies/pending-acknowledgement"),
  get: (id: string) => api.get<Policy>(`/policies/${id}`),
  hasAcknowledged: (id: string) => api.get<{ acknowledged: boolean }>(`/policies/${id}/acknowledged`),
  acknowledge: (id: string) => api.post<void>(`/policies/${id}/acknowledge`),
  stats: (id: string) =>
    api.get<{ total_employees: number; acknowledged_count: number; pending_count: number; percentage: number }>(
      `/policies/${id}/stats`,
    ),
  create: (data: CreatePolicyRequest) => api.post<Policy>("/policies", data),
  update: (id: string, data: Partial<CreatePolicyRequest>) => api.put<Policy>(`/policies/${id}`, data),
  delete: (id: string) => api.delete<void>(`/policies/${id}`),
};
