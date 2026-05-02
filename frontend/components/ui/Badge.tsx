import { cn } from "@/lib/utils";

type BadgeVariant = "default" | "success" | "warning" | "danger" | "accent";

const variantStyles: Record<BadgeVariant, string> = {
  default: "bg-shore-100 text-trippy-500 border border-border",
  success: "bg-success text-white border border-success",
  warning: "bg-warning text-white border border-warning",
  danger: "bg-danger text-white border border-danger",
  accent: "bg-accent-500 text-white border border-accent-500",
};

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
}

export default function Badge({
  variant = "default",
  children,
  className,
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md px-2.5 py-1 text-xs font-semibold",
        variantStyles[variant],
        className
      )}
    >
      {children}
    </span>
  );
}
