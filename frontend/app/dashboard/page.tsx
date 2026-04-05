"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Plus, Search, Filter, MapPin, Loader2 } from "lucide-react";
import { motion } from "framer-motion";
import { Button, Input } from "@/components/ui";
import TripCard from "@/components/trips/TripCard";
import CreateTripModal from "@/components/trips/CreateTripModal";
import { tripsApi, type Trip, type CreateTripRequest } from "@/lib/api";
import { useToast } from "@/lib/toast";

export default function DashboardPage() {
  const router = useRouter();
  const { addToast } = useToast();
  const [createOpen, setCreateOpen] = useState(false);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filterStatus, setFilterStatus] = useState<string>("");

  const fetchTrips = useCallback(async () => {
    setLoading(true);
    try {
      const data = searchQuery
        ? await tripsApi.search(searchQuery, page)
        : await tripsApi.list(page);
      setTrips(data.content);
      setTotalPages(data.totalPages);
    } catch {
      // If API is not available, show empty state
      setTrips([]);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, [searchQuery, page]);

  useEffect(() => {
    fetchTrips();
  }, [fetchTrips]);

  async function handleCreateTrip(data: CreateTripRequest) {
    try {
      const trip = await tripsApi.create(data);
      addToast("Trip created!", "success");
      setCreateOpen(false);
      router.push(`/dashboard/trips/${trip.tripId}`);
    } catch {
      addToast("Failed to create trip", "error");
    }
  }

  const filteredTrips = filterStatus
    ? trips.filter((t) => t.status === filterStatus)
    : trips;

  return (
    <>
      <CreateTripModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreate={handleCreateTrip}
      />

      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <motion.h1
            className="text-2xl font-bold tracking-tight sm:text-3xl"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
          >
            My Trips
          </motion.h1>
          <p className="mt-1 text-muted">
            {loading ? "Loading..." : `${filteredTrips.length} trip${filteredTrips.length !== 1 ? "s" : ""}`}
          </p>
        </div>

        <Button onClick={() => setCreateOpen(true)}>
          <Plus size={16} /> New Trip
        </Button>
      </div>

      {/* Filters */}
      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search
            size={16}
            className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-muted"
          />
          <Input
            placeholder="Search trips..."
            className="pl-10"
            value={searchQuery}
            onChange={(e) => { setSearchQuery(e.target.value); setPage(0); }}
          />
        </div>
        <div className="flex gap-2">
          {["", "DRAFT", "PLANNED", "ONGOING", "COMPLETED"].map((status) => (
            <Button
              key={status}
              variant={filterStatus === status ? "primary" : "secondary"}
              size="sm"
              onClick={() => setFilterStatus(status)}
            >
              {status || "All"}
            </Button>
          ))}
        </div>
      </div>

      {/* Content */}
      {loading ? (
        <div className="mt-16 flex justify-center">
          <Loader2 size={32} className="animate-spin text-trippy-500" />
        </div>
      ) : filteredTrips.length === 0 ? (
        /* Empty state */
        <motion.div
          className="mt-16 flex flex-col items-center text-center"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="flex h-24 w-24 items-center justify-center rounded-full bg-trippy-500/10">
            <MapPin size={40} className="text-trippy-500" />
          </div>
          <h3 className="mt-6 text-lg font-semibold">
            {searchQuery ? "No trips found" : "Plan your first adventure!"}
          </h3>
          <p className="mt-2 max-w-sm text-muted">
            {searchQuery
              ? "Try a different search term or clear your filters."
              : "Create a trip to start planning your next journey with friends."}
          </p>
          {!searchQuery && (
            <Button className="mt-6" onClick={() => setCreateOpen(true)}>
              <Plus size={16} /> Create your first trip
            </Button>
          )}
        </motion.div>
      ) : (
        <>
          {/* Trip grid */}
          <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {filteredTrips.map((trip) => (
              <div
                key={trip.tripId}
                onClick={() => router.push(`/dashboard/trips/${trip.tripId}`)}
                className="cursor-pointer"
              >
                <TripCard
                  title={trip.title}
                  destination={trip.destination}
                  startDate={trip.startDate ?? "TBD"}
                  endDate={trip.endDate ?? "TBD"}
                  status={trip.status === "ONGOING" ? "ACTIVE" : trip.status as "DRAFT" | "PLANNED" | "ACTIVE" | "COMPLETED" | "CANCELLED"}
                  participantCount={trip.participantCount}
                  coverImageUrl={trip.coverImageUrl}
                />
              </div>
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-8 flex items-center justify-center gap-2">
              <Button
                variant="secondary"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                Previous
              </Button>
              <span className="text-sm text-muted">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}
    </>
  );
}
