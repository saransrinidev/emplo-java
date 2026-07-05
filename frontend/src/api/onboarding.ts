import { api } from "./client";

export interface OnboardingTemplate {
  id: string;
  title: string;
  description: string | null;
  category: string;
  sort_order: number;
  is_required: boolean;
  is_active: boolean;
  due_days: number | null;
  action_type: string | null;
  action_url: string | null;
}

export interface OnboardingTask {
  id: string;
  employee_id: string;
  template_id: string | null;
  title: string;
  description: string | null;
  category: string;
  sort_order: number;
  is_required: boolean;
  status: "pending" | "in_progress" | "completed" | "skipped";
  due_date: string | null;
  completed_at: string | null;
  action_type: string | null;
  action_url: string | null;
  notes: string | null;
}

export interface OnboardingProgress {
  total: number;
  completed: number;
  percentage: number;
  is_onboarding: boolean;
}

export interface OnboardingSummaryItem {
  employee_id: string;
  employee_name: string;
  employee_code: string | null;
  department: string | null;
  total_tasks: number;
  completed_tasks: number;
  percentage: number;
  is_complete: boolean;
  date_of_joining: string | null;
}

export const onboardingApi = {
  // Employee
  myTasks: () => api.get<OnboardingTask[]>("/onboarding/my"),
  myProgress: () => api.get<OnboardingProgress>("/onboarding/my/progress"),
  completeTask: (taskId: string, notes?: string) =>
    api.put<OnboardingTask>(`/onboarding/tasks/${taskId}/complete`, { notes }),
  skipTask: (taskId: string) =>
    api.put<OnboardingTask>(`/onboarding/tasks/${taskId}/skip`),

  // HR - Templates
  listTemplates: () => api.get<OnboardingTemplate[]>("/onboarding/templates"),
  createTemplate: (data: Partial<OnboardingTemplate>) =>
    api.post<OnboardingTemplate>("/onboarding/templates", data),
  updateTemplate: (id: string, data: Partial<OnboardingTemplate>) =>
    api.put<OnboardingTemplate>(`/onboarding/templates/${id}`, data),
  deleteTemplate: (id: string) => api.delete(`/onboarding/templates/${id}`),

  // HR - Manage
  initializeOnboarding: (employeeId: string) =>
    api.post<OnboardingTask[]>(`/onboarding/initialize/${employeeId}`),
  getEmployeeTasks: (employeeId: string) =>
    api.get<OnboardingTask[]>(`/onboarding/employee/${employeeId}`),
  getSummary: () => api.get<OnboardingSummaryItem[]>("/onboarding/summary"),
};
