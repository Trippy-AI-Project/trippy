"use client";

import {
  MapPin,
  Calendar,
  Users,
  MoreHorizontal,
} from "lucide-react";
import { GlassCard, Badge, Avatar } from "@/components/ui";
import { cn } from "@/lib/utils";

interface TripCardProps {
  title: string;
  destination: string;
  startDate: string;
  endDate: string;
  status: "DRAFT" | "PLANNED" | "ACTIVE" | "COMPLETED" | "CANCELLED";
  participantCount: number;
  coverImageUrl?: string | null;
}

const statusVariant: Record<string, "default" | "success" | "warning" | "accent" | "danger"> = {
  DRAFT: "default",
  PLANNED: "accent",
  ACTIVE: "success",
  COMPLETED: "warning",
  CANCELLED: "danger",
};

export default function TripCard({
  title,
  destination,
  startDate,
  endDate,
  status,
  participantCount,
  coverImageUrl,
}: TripCardProps) {
  return (
    <GlassCard className="group cursor-pointer p-0 overflow-hidden hover:bg-surface-hover transition-all duration-300">
      {/* Cover */}
      <div className="relative h-36 overflow-hidden">
        {coverImageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={coverImageUrl}
            alt={title}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
        ) : (
          <div className="h-full w-full bg-trippy-gradient opacity-30" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent" />

        {/* Status badge */}
        <div className="absolute top-3 left-3">
          <Badge variant={statusVariant[status] ?? "default"}>
            {status}
          </Badge>
        </div>

        {/* Menu icon */}
        <button className="absolute top-3 right-3 text-white/70 hover:text-white transition-colors">
          <MoreHorizontal size={18} />
        </button>
      </div>

      {/* Content */}
      <div className="p-4 space-y-3">
        <h3 className="text-base font-semibold leading-snug line-clamp-1">
          {title}
        </h3>

        <div className="flex items-center gap-1.5 text-sm text-muted">
          <MapPin size={14} className="shrink-0 text-trippy-400" />
          <span className="line-clamp-1">{destination}</span>
        </div>

        <div className="flex items-center gap-1.5 text-sm text-muted">
          <Calendar size={14} className="shrink-0 text-trippy-400" />
          <span>
            {startDate} — {endDate}
          </span>
        </div>

        <div className="flex items-center justify-between pt-1 border-t border-border">
          <div className="flex items-center gap-1.5 text-sm text-muted">
            <Users size={14} className="text-trippy-400" />
            <span>{participantCount} member{participantCount !== 1 && "s"}</span>
          </div>

          {/* Stacked avatars placeholder */}
          <div className="flex -space-x-2">
            {Array.from({ length: Math.min(participantCount, 3) }).map((_, i) => (
              <Avatar key={i} size="sm" className="ring-2 ring-background" />
            ))}
          </div>
        </div>
      </div>
    </GlassCard>
  );
}
