"use client";

import { cn } from "@/lib/utils";
import { motion, type HTMLMotionProps } from "framer-motion";

type GlassVariant = "default" | "sm" | "strong";

const variantClass: Record<GlassVariant, string> = {
  default: "glass",
  sm: "glass-sm",
  strong: "glass-strong",
};

interface GlassCardProps extends HTMLMotionProps<"div"> {
  variant?: GlassVariant;
}

export default function GlassCard({
  variant = "default",
  className,
  children,
  ...props
}: GlassCardProps) {
  return (
    <motion.div
      className={cn(variantClass[variant], "p-6", className)}
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.5, ease: "easeOut" }}
      {...props}
    >
      {children}
    </motion.div>
  );
}
