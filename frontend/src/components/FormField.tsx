import type { ReactNode } from "react";

interface FormFieldProps {
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: ReactNode;
  className?: string;
  htmlFor?: string;
}

/**
 * Reusable form field wrapper — provides consistent label, error, and hint layout.
 * Usage:
 * ```tsx
 * <FormField label="Email" required error={errors.email}>
 *   <input className="input" value={email} onChange={...} />
 * </FormField>
 * ```
 */
export default function FormField({
  label,
  required = false,
  error,
  hint,
  children,
  className = "",
  htmlFor,
}: FormFieldProps) {
  return (
    <div className={`form-field ${error ? "form-field-error" : ""} ${className}`}>
      <label className="form-field-label" htmlFor={htmlFor}>
        {label}
        {required && <span className="form-field-required">*</span>}
      </label>
      {children}
      {error && <span className="form-field-error-text">{error}</span>}
      {!error && hint && <span className="form-field-hint">{hint}</span>}
    </div>
  );
}
