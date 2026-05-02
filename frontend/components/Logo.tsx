import { Compass } from "lucide-react";
import { cn } from "@/lib/utils";

interface LogoProps {
  size?: "sm" | "md" | "lg";
  className?: string;
}

const sizeMap = {
  sm: { icon: 18, text: "text-lg" },
  md: { icon: 22, text: "text-xl" },
  lg: { icon: 28, text: "text-2xl" },
};

export default function Logo({ size = "md", className }: LogoProps) {
  const s = sizeMap[size];
  return (
    <div className={cn("group flex items-center gap-2", className)}>
      <div className="grid place-items-center rounded-2xl bg-trippy-500 p-1.5 text-white shadow-sm transition-transform duration-200 group-hover:-translate-y-0.5">
        <Compass size={s.icon} className="text-white" />
      </div>
      <span className={cn("font-display font-bold text-foreground", s.text)}>
        Trippy
      </span>
    </div>
  );
}
