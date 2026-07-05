import type { ReactNode } from "react";

export interface TabItem {
  key: string;
  label: string;
  icon?: ReactNode;
  count?: number;
}

interface TabsProps {
  items: TabItem[];
  active: string;
  onChange: (key: string) => void;
  variant?: "default" | "pills" | "underline";
  size?: "sm" | "md";
  className?: string;
}

/**
 * Reusable tab bar component.
 * Usage:
 * ```tsx
 * <Tabs
 *   items={[{ key: "all", label: "All", count: 12 }, { key: "unread", label: "Unread" }]}
 *   active={tab}
 *   onChange={setTab}
 * />
 * ```
 */
export default function Tabs({
  items,
  active,
  onChange,
  variant = "default",
  size = "md",
  className = "",
}: TabsProps) {
  return (
    <div className={`ui-tabs ui-tabs-${variant} ui-tabs-${size} ${className}`} role="tablist">
      {items.map((item) => (
        <button
          key={item.key}
          role="tab"
          aria-selected={active === item.key}
          className={`ui-tab-item ${active === item.key ? "ui-tab-active" : ""}`}
          onClick={() => onChange(item.key)}
        >
          {item.icon && <span className="ui-tab-icon">{item.icon}</span>}
          {item.label}
          {item.count !== undefined && (
            <span className="ui-tab-count">{item.count}</span>
          )}
        </button>
      ))}
    </div>
  );
}
