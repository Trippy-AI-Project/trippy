"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  ArrowLeft,
  MapPin,
  Calendar,
  Users,
  MessageSquare,
  Loader2,
  Edit,
  Trash2,
} from "lucide-react";
import { motion } from "framer-motion";
import { GlassCard, Button, Badge, Avatar } from "@/components/ui";
import { tripsApi, type TripDetail } from "@/lib/api";
import { useToast } from "@/lib/toast";

const statusVariant: Record<string, "default" | "success" | "warning" | "accent" | "danger"> = {
  DRAFT: "default",
  PLANNED: "accent",
  ONGOING: "success",
  COMPLETED: "warning",
  CANCELLED: "danger",
};

export default function TripDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { addToast } = useToast();
  const tripId = params.id as string;

  const [trip, setTrip] = useState<TripDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!tripId) return;
    setLoading(true);
    tripsApi
      .get(tripId)
      .then(setTrip)
      .catch(() => setError("Failed to load trip details"))
      .finally(() => setLoading(false));
  }, [tripId]);

  async function handleDelete() {
    if (!confirm("Are you sure you want to delete this trip?")) return;
    try {
      await tripsApi.delete(tripId);
      addToast("Trip deleted", "success");
      router.push("/dashboard");
    } catch {
      addToast("Failed to delete trip", "error");
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <Loader2 size={32} className="animate-spin text-trippy-500" />
      </div>
    );
  }

  if (error || !trip) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4">
        <p className="text-muted">{error || "Trip not found"}</p>
        <Button variant="secondary" onClick={() => router.push("/dashboard")}>
          <ArrowLeft size={16} /> Back to trips
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Back link */}
      <Link
        href="/dashboard"
        className="inline-flex items-center gap-2 text-sm text-muted hover:text-foreground transition-colors"
      >
        <ArrowLeft size={16} /> Back to trips
      </Link>

      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between"
      >
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold sm:text-3xl">{trip.title}</h1>
            <Badge variant={statusVariant[trip.status] ?? "default"}>
              {trip.status}
            </Badge>
          </div>
          {trip.description && (
            <p className="mt-2 text-muted max-w-2xl">{trip.description}</p>
          )}
        </div>
        <div className="flex gap-2">
          <Link href={`/dashboard/chat/${trip.tripId}`}>
            <Button variant="secondary" size="sm">
              <MessageSquare size={14} /> Chat
            </Button>
          </Link>
          <Button variant="secondary" size="sm">
            <Edit size={14} /> Edit
          </Button>
          <Button variant="danger" size="sm" onClick={handleDelete}>
            <Trash2 size={14} /> Delete
          </Button>
        </div>
      </motion.div>

      {/* Trip info cards */}
      <div className="grid gap-6 sm:grid-cols-3">
        <GlassCard className="flex items-center gap-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-trippy-500/10">
            <MapPin size={20} className="text-trippy-500" />
          </div>
          <div>
            <p className="text-xs text-muted">Destination</p>
            <p className="font-medium">{trip.destination}</p>
          </div>
        </GlassCard>

        <GlassCard className="flex items-center gap-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-trippy-500/10">
            <Calendar size={20} className="text-trippy-500" />
          </div>
          <div>
            <p className="text-xs text-muted">Dates</p>
            <p className="font-medium">
              {trip.startDate ?? "TBD"} — {trip.endDate ?? "TBD"}
            </p>
          </div>
        </GlassCard>

        <GlassCard className="flex items-center gap-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-trippy-500/10">
            <Users size={20} className="text-trippy-500" />
          </div>
          <div>
            <p className="text-xs text-muted">Participants</p>
            <p className="font-medium">{trip.participantCount} member{trip.participantCount !== 1 && "s"}</p>
          </div>
        </GlassCard>
      </div>

      {/* Participants */}
      {trip.participants && trip.participants.length > 0 && (
        <GlassCard>
          <h3 className="text-lg font-semibold mb-4">Participants</h3>
          <div className="space-y-3">
            {trip.participants.map((p) => (
              <div
                key={p.participantId}
                className="flex items-center gap-3 rounded-xl p-2 hover:bg-surface transition-colors"
              >
                <Avatar name={p.displayName ?? "User"} src={p.avatarUrl} size="sm" />
                <div className="flex-1">
                  <p className="text-sm font-medium">{p.displayName ?? "Unknown"}</p>
                  <p className="text-xs text-muted capitalize">{p.role.toLowerCase()}</p>
                </div>
                <Badge
                  variant={p.status === "ACCEPTED" ? "success" : p.status === "PENDING" ? "warning" : "default"}
                >
                  {p.status}
                </Badge>
              </div>
            ))}
          </div>
        </GlassCard>
      )}

      {/* Itinerary */}
      {trip.itinerary && trip.itinerary.days && trip.itinerary.days.length > 0 && (
        <GlassCard>
          <h3 className="text-lg font-semibold mb-4">Itinerary</h3>
          <div className="space-y-6">
            {trip.itinerary.days.map((day) => (
              <div key={day.dayPlanId}>
                <h4 className="font-medium text-trippy-500">
                  Day {day.dayNumber}{day.title ? ` — ${day.title}` : ""}
                  {day.date && <span className="text-xs text-muted ml-2">{day.date}</span>}
                </h4>
                <div className="mt-2 space-y-2 pl-4 border-l-2 border-trippy-500/20">
                  {day.activities.map((act) => (
                    <div key={act.activityId} className="pl-3 py-1">
                      <p className="text-sm font-medium">
                        {act.time && <span className="text-muted mr-2">{act.time}</span>}
                        {act.title}
                      </p>
                      {act.description && (
                        <p className="text-xs text-muted mt-0.5">{act.description}</p>
                      )}
                      {act.location && (
                        <p className="text-xs text-muted flex items-center gap-1 mt-0.5">
                          <MapPin size={10} /> {act.location}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </GlassCard>
      )}
    </div>
  );
}
