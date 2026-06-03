"use client";

import { cn } from "@/lib/utils";

interface AmbientBackgroundProps {
  className?: string;
  fixed?: boolean;
}

export default function AmbientBackground({
  className,
  fixed = true,
}: AmbientBackgroundProps) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        "pointer-events-none inset-0 z-0 overflow-hidden bg-[#f8efe1]",
        fixed ? "fixed" : "absolute",
        className,
      )}
    >
      <div className="absolute inset-0 bg-[url('/trippy-landing-background.png')] bg-cover bg-center bg-no-repeat" />
      <div className="absolute inset-0 bg-[linear-gradient(rgba(23,33,31,0.048)_1px,transparent_1px),linear-gradient(90deg,rgba(23,33,31,0.042)_1px,transparent_1px)] bg-[size:78px_78px] opacity-50" />
      <div className="absolute inset-0 bg-[linear-gradient(to_bottom,rgba(255,250,242,0.38)_0%,rgba(255,250,242,0.12)_38%,rgba(248,239,225,0.42)_100%)]" />
    </div>
  );
}
