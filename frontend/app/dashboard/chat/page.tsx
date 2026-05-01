"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { MessageSquare, Loader2, Users } from "lucide-react";
import { motion } from "framer-motion";
import { GlassCard, Badge } from "@/components/ui";
import { tripsApi, type Trip } from "@/lib/api";

export default function ChatListPage() {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchTrips = useCallback(async () => {
    setLoading(true);
    try {
      const data = await tripsApi.list(0, 50);
      setTrips(data.content);
    } catch {
      setTrips([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTrips();
  }, [fetchTrips]);

  return (
    <div>
      <motion.h1
        className="text-2xl font-bold tracking-tight sm:text-3xl"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
      >
        Chat
      </motion.h1>
      <p className="mt-1 text-muted">Select a trip to start chatting</p>

      {loading ? (
        <div className="mt-16 flex justify-center">
          <Loader2 size={32} className="animate-spin text-trippy-500" />
        </div>
      ) : trips.length === 0 ? (
        <div className="mt-16 flex flex-col items-center text-center">
          <div className="flex h-20 w-20 items-center justify-center rounded-full bg-trippy-500/10">
            <MessageSquare size={32} className="text-trippy-500" />
          </div>
          <h3 className="mt-4 text-lg font-semibold">No trips yet</h3>
          <p className="mt-2 text-sm text-muted">
            Create a trip first to start chatting with participants.
          </p>
        </div>
      ) : (
        <div className="mt-6 space-y-3">
          {trips.map((trip) => (
            <Link key={trip.tripId} href={`/dashboard/chat/${trip.tripId}`}>
              <GlassCard className="flex items-center gap-4 p-4 hover:bg-surface-hover transition-all cursor-pointer">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-trippy-500/10">
                  <MessageSquare size={20} className="text-trippy-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium truncate">{trip.title}</p>
                  <p className="text-sm text-muted truncate">{trip.destination}</p>
                </div>
                <div className="flex items-center gap-2 text-sm text-muted">
                  <Users size={14} />
                  <span>{trip.participantCount}</span>
                </div>
              </GlassCard>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
