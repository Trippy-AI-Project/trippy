"use client";

import { cn } from "@/lib/utils";
import { type ButtonHTMLAttributes } from "react";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";
type ButtonSize = "sm" | "md" | "lg";

const variantStyles: Record<ButtonVariant, string> = {
  primary:
    "bg-accent-500 text-white border border-accent-500 shadow-md hover:bg-accent-600 hover:border-accent-600 hover:-translate-y-0.5",
  secondary:
    "bg-surface text-trippy-500 border border-trippy-500 shadow-sm hover:bg-shore-50 hover:-translate-y-0.5",
  ghost:
    "bg-transparent text-foreground border border-transparent hover:text-trippy-500 hover:bg-shore-50",
  danger:
    "bg-danger text-white border border-danger shadow-sm hover:bg-accent-700",
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: "px-3.5 py-2 text-sm rounded-lg",
  md: "px-5 py-2.5 text-sm rounded-lg",
  lg: "px-7 py-3.5 text-base rounded-lg",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
}

export default function Button({
  variant = "primary",
  size = "md",
  className,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={cn(
        "inline-flex items-center justify-center gap-2 font-semibold transition-all duration-200 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0 focus-visible:focus-ring",
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
      {...props}
    >
      {children}
    </button>
  );
}
