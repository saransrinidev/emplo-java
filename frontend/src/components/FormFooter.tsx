import Button from "./Button";

interface FormFooterProps {
  onCancel?: () => void;
  cancelLabel?: string;
  submitLabel?: string;
  loading?: boolean;
  loadingText?: string;
  disabled?: boolean;
  danger?: boolean;
  className?: string;
}

/**
 * Reusable form action footer with Cancel + Submit buttons.
 * Usage:
 * ```tsx
 * <FormFooter onCancel={() => setOpen(false)} submitLabel="Save" loading={submitting} />
 * ```
 */
export default function FormFooter({
  onCancel,
  cancelLabel = "Cancel",
  submitLabel = "Submit",
  loading = false,
  loadingText,
  disabled = false,
  danger = false,
  className = "",
}: FormFooterProps) {
  return (
    <div className={`form-footer ${className}`}>
      {onCancel && (
        <Button variant="outline" size="sm" type="button" onClick={onCancel} disabled={loading}>
          {cancelLabel}
        </Button>
      )}
      <Button
        variant={danger ? "destructive" : "primary"}
        size="sm"
        type="submit"
        loading={loading}
        loadingText={loadingText}
        disabled={disabled}
      >
        {submitLabel}
      </Button>
    </div>
  );
}
