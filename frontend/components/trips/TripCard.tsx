"use client";

import {
  MapPin,
  Calendar,
  Users,
  ArrowUpRight,
  Plane,
} from "lucide-react";
import { cn } from "@/lib/utils";

interface TripCardProps {
  title: string;
  destination: string;
  startDate: string;
  endDate: string;
  status: "DRAFT" | "PLANNED" | "ACTIVE" | "COMPLETED" | "CANCELLED";
  participantCount: number;
  coverImageUrl?: string | null;
  onJoin?: () => void;
  joinLoading?: boolean;
  joinRequested?: boolean;
}

const STATUS_CONFIG: Record<
  string,
  { bg: string; text: string; dot: string; label: string }
> = {
  DRAFT: {
    bg: "bg-shore-200/60",
    text: "text-trippy-500",
    dot: "bg-trippy-400",
    label: "Draft",
  },
  PLANNED: {
    bg: "bg-accent-50",
    text: "text-accent-600",
    dot: "bg-accent-500",
    label: "Planned",
  },
  ACTIVE: {
    bg: "bg-leaf-100",
    text: "text-green-700",
    dot: "bg-green-500",
    label: "Active",
  },
  COMPLETED: {
    bg: "bg-lagoon-100",
    text: "text-teal-700",
    dot: "bg-teal-500",
    label: "Completed",
  },
  CANCELLED: {
    bg: "bg-red-50",
    text: "text-red-600",
    dot: "bg-red-400",
    label: "Cancelled",
  },
};

/** Random placeholder gradients when no cover image */
const PLACEHOLDER_GRADIENTS = [
  "from-trippy-400/30 to-accent-400/20",
  "from-accent-400/25 to-trippy-300/20",
  "from-lagoon-200/50 to-trippy-300/25",
  "from-leaf-200/40 to-lagoon-200/30",
  "from-accent-200/40 to-shore-200/30",
];

function getGradient(title: string) {
  let hash = 0;
  for (let i = 0; i < title.length; i++) {
    hash = title.charCodeAt(i) + ((hash << 5) - hash);
  }
  return PLACEHOLDER_GRADIENTS[
    Math.abs(hash) % PLACEHOLDER_GRADIENTS.length
  ];
}

function formatDateRange(start: string, end: string) {
  if (start === "TBD" && end === "TBD") return "Dates TBD";
  try {
    const opts: Intl.DateTimeFormatOptions = {
      month: "short",
      day: "numeric",
    };
    const s = start !== "TBD" ? new Date(start).toLocaleDateString("en", opts) : "TBD";
    const e = end !== "TBD" ? new Date(end).toLocaleDateString("en", opts) : "TBD";
    return `${s} — ${e}`;
  } catch {
    return `${start} — ${end}`;
  }
}

export default function TripCard({
  title,
  destination,
  startDate,
  endDate,
  status,
  participantCount,
  coverImageUrl,
  onJoin,
  joinLoading,
  joinRequested,
}: TripCardProps) {
  const cfg = STATUS_CONFIG[status] ?? STATUS_CONFIG.DRAFT;

  return (
    <div className="group relative overflow-hidden rounded-2xl border border-border bg-surface transition-all duration-300 hover:shadow-xl hover:-translate-y-1 hover:border-accent-300/50">
      {/* ── Cover area ────────────────────────────────────── */}
      <div className="relative h-40 overflow-hidden">
        {coverImageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={coverImageUrl}
            alt={title}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
          />
        ) : (
          <div
            className={cn(
              "h-full w-full bg-gradient-to-br",
              getGradient(title)
            )}
          >
            {/* Decorative plane icon */}
            <Plane
              size={64}
              className="absolute right-4 bottom-2 text-trippy-400/15 rotate-12 transition-transform duration-500 group-hover:translate-x-2 group-hover:-translate-y-2"
            />
          </div>
        )}
        {/* Gradient overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/10 to-transparent" />

        {/* Status pill */}
        <div className="absolute top-3.5 left-3.5">
          <span
            className={cn(
              "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold backdrop-blur-md",
              cfg.bg,
              cfg.text
            )}
          >
            <span
              className={cn(
                "h-1.5 w-1.5 rounded-full",
                cfg.dot,
                status === "ACTIVE" && "animate-pulse"
              )}
            />
            {cfg.label}
          </span>
        </div>

        {/* Hover arrow */}
        <div className="absolute top-3.5 right-3.5 flex h-8 w-8 items-center justify-center rounded-full bg-white/10 text-white/0 backdrop-blur-sm transition-all duration-300 group-hover:bg-white/20 group-hover:text-white">
          <ArrowUpRight size={14} />
        </div>

        {/* Title overlay on cover */}
        <div className="absolute bottom-0 left-0 right-0 p-4">
          <h3 className="text-base font-bold text-white leading-snug line-clamp-1 drop-shadow-sm">
            {title}
          </h3>
        </div>
      </div>

      {/* ── Card body ─────────────────────────────────────── */}
      <div className="p-4 space-y-3">
        {/* Destination */}
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-accent-50">
            <MapPin size={13} className="text-accent-500" />
          </div>
          <span className="text-sm text-foreground font-medium line-clamp-1">
            {destination}
          </span>
        </div>

        {/* Date + members row */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-xs text-muted">
            <Calendar size={12} className="text-trippy-400" />
            <span>{formatDateRange(startDate, endDate)}</span>
          </div>

          <div className="flex items-center gap-1.5 text-xs text-muted">
            <Users size={12} className="text-trippy-400" />
            <span>
              {participantCount} member
              {participantCount !== 1 && "s"}
            </span>
          </div>
        </div>

        {/* Join button for public trips */}
        {onJoin && !joinRequested && (
          <button
            onClick={(e) => { e.preventDefault(); e.stopPropagation(); onJoin(); }}
            disabled={joinLoading}
            className="w-full rounded-lg bg-accent-500 px-3 py-2 text-xs font-semibold text-white transition-colors hover:bg-accent-600 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {joinLoading ? "Sending request…" : "Join Trip"}
          </button>
        )}
        {joinRequested && (
          <button
            disabled
            className="w-full rounded-lg bg-shore-200 px-3 py-2 text-xs font-semibold text-muted cursor-not-allowed border border-border"
          >
            Requested to Join
          </button>
        )}

        {/* Bottom accent line */}
        <div className="relative h-0.5 w-full overflow-hidden rounded-full bg-border">
          <div className="absolute inset-y-0 left-0 w-0 rounded-full bg-gradient-to-r from-accent-400 to-accent-500 transition-all duration-500 group-hover:w-full" />
        </div>
      </div>
    </div>
  );
}
