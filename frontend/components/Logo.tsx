import { useId } from "react";
import { cn } from "@/lib/utils";

interface LogoProps {
  size?: "sm" | "md" | "lg";
  className?: string;
}

const sizeMap = {
  sm: { mark: "h-8 w-8 rounded-lg", text: "text-lg" },
  md: { mark: "h-10 w-10 rounded-xl", text: "text-xl" },
  lg: { mark: "h-12 w-12 rounded-2xl", text: "text-2xl" },
};

export default function Logo({ size = "md", className }: LogoProps) {
  const s = sizeMap[size];
  const id = useId().replaceAll(":", "");
  const gradientId = `trippyLogoBg${id}`;
  const shadowId = `trippyLogoShadow${id}`;

  return (
    <div className={cn("group flex items-center gap-2.5", className)}>
      <div
        className={cn(
          "grid place-items-center overflow-hidden bg-[#123d36] text-white shadow-[0_16px_36px_-24px_rgba(20,47,43,0.82)] transition-transform duration-200 group-hover:-translate-y-0.5",
          s.mark,
        )}
      >
        <svg viewBox="0 0 64 64" aria-hidden="true" className="h-full w-full">
          <defs>
            <linearGradient id={gradientId} x1="10" x2="54" y1="8" y2="58" gradientUnits="userSpaceOnUse">
              <stop stopColor="#174c43" />
              <stop offset="1" stopColor="#0f302b" />
            </linearGradient>
            <filter id={shadowId} x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="2" stdDeviation="1.5" floodColor="#08221e" floodOpacity="0.35" />
            </filter>
          </defs>
          <rect width="64" height="64" fill={`url(#${gradientId})`} rx="16" />
          <path
            d="M16 23.5h22.5c6.9 0 6.9 10.4 0 10.4h-14c-6.8 0-6.8 10.4 0 10.4H47"
            fill="none"
            stroke="#fff8ea"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="5.6"
            filter={`url(#${shadowId})`}
          />
          <path
            d="M46.8 13.8c-6 0-10.8 4.8-10.8 10.8 0 8 10.8 17.5 10.8 17.5s10.8-9.5 10.8-17.5c0-6-4.8-10.8-10.8-10.8Z"
            fill="#f06f4f"
          />
          <circle cx="46.8" cy="24.7" r="4.2" fill="#123d36" />
          <path
            d="m16.7 41.2 2.1 4.7 4.7 2.1-4.7 2.1-2.1 4.7-2.1-4.7-4.7-2.1 4.7-2.1 2.1-4.7Z"
            fill="#f06f4f"
          />
          <circle cx="27.8" cy="49.1" r="2.1" fill="#f06f4f" />
          <circle cx="34.6" cy="49.1" r="2.1" fill="#f06f4f" />
        </svg>
      </div>
      <span className={cn("font-display font-black text-[#17211f]", s.text)}>
        Trippy
      </span>
    </div>
  );
}
