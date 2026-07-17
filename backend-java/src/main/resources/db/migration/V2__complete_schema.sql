-- ============================================================================
-- V2: Complete schema definition with all tables, constraints, indexes, enums.
-- This migration is idempotent — uses IF NOT EXISTS where possible.
-- ============================================================================

-- ─── Enum types ──────────────────────────────────────────────────────────────
DO $$ BEGIN
  CREATE TYPE role_name AS ENUM ('employee', 'manager', 'hr_admin');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE address_type AS ENUM ('current', 'permanent');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE document_type AS ENUM ('school', 'intermediate', 'degree', 'transcript', 'other');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE verification_status AS ENUM ('uploaded', 'verified', 'rejected');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE certification_category AS ENUM ('microsoft', 'aws', 'azure', 'google', 'meta', 'scrum', 'pmp', 'cisco', 'comptia', 'ibm', 'coursera', 'power_bi', 'other');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE approval_status AS ENUM ('pending', 'approved', 'rejected');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE editable_section AS ENUM ('address', 'phone', 'certifications');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE leave_type_enum AS ENUM ('casual', 'sick', 'earned', 'maternity', 'paternity', 'unpaid');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE leave_status AS ENUM ('pending', 'forwarded_to_hr', 'approved', 'rejected');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE ticket_type AS ENUM ('leave', 'wfh', 'document_update', 'profile_edit', 'certification', 'salary_query', 'general');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE ticket_priority AS ENUM ('low', 'medium', 'high');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE ticket_status AS ENUM ('open', 'in_progress', 'resolved', 'closed', 'rejected');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE task_priority AS ENUM ('low', 'medium', 'high', 'urgent');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE task_status AS ENUM ('open', 'in_progress', 'completed', 'closed');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE attendance_status AS ENUM ('present', 'absent', 'half_day', 'on_leave', 'holiday', 'weekend');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE edit_request_status AS ENUM ('pending', 'approved', 'changes_submitted', 'confirmed', 'rejected', 'expired');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE change_status AS ENUM ('pending', 'approved', 'rejected');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE onboarding_category AS ENUM ('personal_info', 'documents', 'it_setup', 'compliance', 'team_intro', 'training', 'custom');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE onboarding_task_status AS ENUM ('pending', 'in_progress', 'completed', 'skipped');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE reimbursement_status AS ENUM ('pending', 'manager_approved', 'manager_rejected', 'hr_approved', 'hr_rejected', 'paid');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE reimbursement_category AS ENUM ('travel', 'food', 'accommodation', 'office_supplies', 'internet_phone', 'medical', 'training', 'client_entertainment', 'other');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE policy_category AS ENUM ('code_of_conduct', 'leave_policy', 'attendance_policy', 'compensation_benefits', 'it_security', 'health_safety', 'anti_harassment', 'travel_expense', 'remote_work', 'general');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ─── Unique constraints (add if not existing) ─────────────────────────────────
-- These are critical for data integrity

-- users.email must be unique
DO $$ BEGIN
  ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
EXCEPTION WHEN duplicate_object OR duplicate_table THEN NULL; END $$;

-- users.employee_id must be unique (one user per employee)
DO $$ BEGIN
  ALTER TABLE users ADD CONSTRAINT uk_users_employee_id UNIQUE (employee_id);
EXCEPTION WHEN duplicate_object OR duplicate_table THEN NULL; END $$;

-- employees.employee_code must be unique
DO $$ BEGIN
  ALTER TABLE employees ADD CONSTRAINT uk_employees_employee_code UNIQUE (employee_code);
EXCEPTION WHEN duplicate_object OR duplicate_table THEN NULL; END $$;

-- employees.email — unique per active employee
DO $$ BEGIN
  CREATE UNIQUE INDEX IF NOT EXISTS uk_employees_email_active ON employees(email) WHERE is_active = true;
EXCEPTION WHEN duplicate_object OR duplicate_table THEN NULL; END $$;

-- roles.name must be unique
DO $$ BEGIN
  ALTER TABLE roles ADD CONSTRAINT uk_roles_name UNIQUE (name);
EXCEPTION WHEN duplicate_object OR duplicate_table THEN NULL; END $$;

-- leave_balances(employee_id, leave_type_id, year) unique
DO $$ BEGIN
  ALTER TABLE leave_balances ADD CONSTRAINT uk_leave_balance_emp_type_year UNIQUE (employee_id, leave_type_id, year);
EXCEPTION WHEN duplicate_object OR duplicate_table THEN NULL; END $$;

-- policy_acknowledgements(policy_id, employee_id) unique
DO $$ BEGIN
  ALTER TABLE policy_acknowledgements ADD CONSTRAINT uk_policy_ack_policy_employee UNIQUE (policy_id, employee_id);
EXCEPTION WHEN duplicate_object OR duplicate_table THEN NULL; END $$;

-- ─── Indexes for common query patterns ────────────────────────────────────────

-- Employee lookups
CREATE INDEX IF NOT EXISTS idx_employees_manager_id ON employees(manager_id);
CREATE INDEX IF NOT EXISTS idx_employees_employment_status ON employees(employment_status);
CREATE INDEX IF NOT EXISTS idx_employees_is_active ON employees(is_active);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department);

-- User lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_employee_id ON users(employee_id);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);

-- Leave requests
CREATE INDEX IF NOT EXISTS idx_leave_requests_employee_id ON leave_requests(employee_id);
CREATE INDEX IF NOT EXISTS idx_leave_requests_status ON leave_requests(status);
CREATE INDEX IF NOT EXISTS idx_leave_requests_employee_status ON leave_requests(employee_id, status);
CREATE INDEX IF NOT EXISTS idx_leave_requests_start_date ON leave_requests(start_date);

-- Attendance records
CREATE INDEX IF NOT EXISTS idx_attendance_records_employee_id ON attendance_records(employee_id);
CREATE INDEX IF NOT EXISTS idx_attendance_records_work_date ON attendance_records(work_date);
CREATE INDEX IF NOT EXISTS idx_attendance_records_emp_date ON attendance_records(employee_id, work_date);

-- Notifications
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);

-- Documents
CREATE INDEX IF NOT EXISTS idx_documents_employee_id ON documents(employee_id);
CREATE INDEX IF NOT EXISTS idx_documents_type_status ON documents(document_type, status);

-- Audit logs
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_time ON audit_logs(actor_id, created_at DESC);

-- Tickets
CREATE INDEX IF NOT EXISTS idx_tickets_employee_id ON tickets(employee_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tickets_emp_status ON tickets(employee_id, status);

-- Salary revisions
CREATE INDEX IF NOT EXISTS idx_salary_revisions_employee_id ON salary_revisions(employee_id);
CREATE INDEX IF NOT EXISTS idx_salary_revisions_approval ON salary_revisions(approval_status);

-- Certifications
CREATE INDEX IF NOT EXISTS idx_certifications_employee_id ON certifications(employee_id);
CREATE INDEX IF NOT EXISTS idx_certifications_expiry ON certifications(expiry_date);

-- Messages
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver_unread ON messages(receiver_id, is_read);

-- Tasks
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to ON tasks(assigned_to);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_by ON tasks(assigned_by);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);

-- Reimbursements
CREATE INDEX IF NOT EXISTS idx_reimbursements_employee_id ON reimbursements(employee_id);
CREATE INDEX IF NOT EXISTS idx_reimbursements_status ON reimbursements(status);

-- Onboarding
CREATE INDEX IF NOT EXISTS idx_onboarding_tasks_employee ON onboarding_tasks(employee_id);
CREATE INDEX IF NOT EXISTS idx_onboarding_tasks_status ON onboarding_tasks(employee_id, status);

-- Holidays
CREATE INDEX IF NOT EXISTS idx_holidays_date ON holidays(holiday_date);

-- ─── Soft delete columns (add if missing) ─────────────────────────────────────
DO $$ BEGIN
  ALTER TABLE employees ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE employees ADD COLUMN IF NOT EXISTS terminated_at TIMESTAMPTZ;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE employees ADD COLUMN IF NOT EXISTS terminated_by UUID;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE employees ADD COLUMN IF NOT EXISTS termination_reason TEXT;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

-- ─── Optimistic locking version columns ───────────────────────────────────────
DO $$ BEGIN
  ALTER TABLE employees ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE reimbursements ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE salary_revisions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE salary_structures ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE policies ADD COLUMN IF NOT EXISTS version_lock BIGINT NOT NULL DEFAULT 0;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE performance_reviews ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE tickets ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;
