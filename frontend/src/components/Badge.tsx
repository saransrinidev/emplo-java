import type { ReactNode } from "react";

type BadgeVariant =
  | "default"
  | "success"
  | "warning"
  | "danger"
  | "info"
  | "indigo"
  | "amber"
  | "green"
  | "blue"
  | "rose"
  | "navy";

interface BadgeProps {
  children: ReactNode;
  variant?: BadgeVariant;
  dot?: boolean;
  className?: string;
  size?: "sm" | "md";
}

/**
 * Multipurpose badge/pill component.
 * Usage:
 * ```tsx
 * <Badge variant="success">Active</Badge>
 * <Badge variant="warning" dot>Pending</Badge>
 * <Badge variant="danger" size="sm">Expired</Badge>
 * ```
 */
export default function Badge({
  children,
  variant = "default",
  dot = false,
  className = "",
  size = "md",
}: BadgeProps) {
  return (
    <span className={`ui-badge ui-badge-${variant} ui-badge-${size} ${className}`}>
      {dot && <span className="ui-badge-dot" />}
      {children}
    </span>
  );
}

// Pre-built role badge
export function RoleBadge({ role }: { role: string | null | undefined }) {
  const roleConfig: Record<string, { label: string; variant: BadgeVariant }> = {
    hr_admin: { label: "HR Admin", variant: "indigo" },
    manager: { label: "Manager", variant: "amber" },
    employee: { label: "Employee", variant: "green" },
  };

  const config = roleConfig[role ?? "employee"] ?? roleConfig.employee;
  return <Badge variant={config.variant} size="sm">{config.label}</Badge>;
}

// Pre-built status badge
export function StatusBadge({ status }: { status: string }) {
  const map: Record<string, { label: string; variant: BadgeVariant }> = {
    active: { label: "Active", variant: "success" },
    Active: { label: "Active", variant: "success" },
    terminated: { label: "Terminated", variant: "danger" },
    Terminated: { label: "Terminated", variant: "danger" },
    resigned: { label: "Resigned", variant: "warning" },
    Resigned: { label: "Resigned", variant: "warning" },
    uploaded: { label: "Uploaded", variant: "info" },
    verified: { label: "Verified", variant: "success" },
    rejected: { label: "Rejected", variant: "danger" },
    approved: { label: "Approved", variant: "success" },
    pending: { label: "Pending", variant: "warning" },
    forwarded_to_hr: { label: "Forwarded to HR", variant: "blue" },
  };

  const config = map[status] ?? { label: status, variant: "default" as BadgeVariant };
  return <Badge variant={config.variant}>{config.label}</Badge>;
}
