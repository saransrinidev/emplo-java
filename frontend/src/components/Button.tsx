import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from "react";
import { Loader2 } from "lucide-react";

type ButtonVariant = "primary" | "outline" | "ghost" | "destructive" | "destructive-light";
type ButtonSize = "sm" | "md" | "lg" | "icon-sm" | "icon-md";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  loadingText?: string;
  icon?: ReactNode;
  iconRight?: ReactNode;
  fullWidth?: boolean;
}

const variantClassMap: Record<ButtonVariant, string> = {
  primary: "btn",
  outline: "btn btn-outline",
  ghost: "btn btn-ghost",
  destructive: "btn btn-destructive",
  "destructive-light": "btn-destructive-light",
};

const sizeClassMap: Record<ButtonSize, string> = {
  sm: "btn-sm",
  md: "",
  lg: "btn-lg",
  "icon-sm": "btn-icon-sm",
  "icon-md": "btn-icon-md",
};

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = "primary",
      size = "sm",
      loading = false,
      loadingText,
      icon,
      iconRight,
      fullWidth = false,
      className = "",
      disabled,
      children,
      style,
      ...rest
    },
    ref,
  ) => {
    const classes = [
      variantClassMap[variant],
      sizeClassMap[size],
      fullWidth ? "btn-full" : "",
      className,
    ]
      .filter(Boolean)
      .join(" ");

    return (
      <button
        ref={ref}
        className={classes}
        disabled={disabled || loading}
        style={style}
        {...rest}
      >
        {loading ? (
          <>
            <Loader2 size={14} className="btn-spinner" />
            {loadingText ?? children}
          </>
        ) : (
          <>
            {icon && <span className="btn-icon-left">{icon}</span>}
            {children}
            {iconRight && <span className="btn-icon-right">{iconRight}</span>}
          </>
        )}
      </button>
    );
  },
);

Button.displayName = "Button";
export default Button;
