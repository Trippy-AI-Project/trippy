import { cn } from "@/lib/utils";

type BadgeVariant = "default" | "success" | "warning" | "danger" | "accent";

const variantStyles: Record<BadgeVariant, string> = {
  default: "bg-trippy-500/14 text-trippy-700 border border-trippy-300/35",
  success: "bg-success/15 text-success",
  warning: "bg-warning/15 text-warning",
  danger: "bg-danger/15 text-danger",
  accent: "bg-accent-400/15 text-accent-600 border border-accent-300/35",
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
        "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium backdrop-blur-sm",
        variantStyles[variant],
        className
      )}
    >
      {children}
    </span>
  );
}
