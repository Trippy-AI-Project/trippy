"use client";

import { useState } from "react";
import { Plus, Search, Filter } from "lucide-react";
import { motion } from "framer-motion";
import { Button, Input } from "@/components/ui";
import TripCard from "@/components/trips/TripCard";
import CreateTripModal from "@/components/trips/CreateTripModal";

// Demo data — will be replaced by API calls
const SAMPLE_TRIPS = [
  {
    id: "1",
    title: "Summer in Barcelona",
    destination: "Barcelona, Spain",
    startDate: "Jun 15",
    endDate: "Jun 25",
    status: "PLANNED" as const,
    participantCount: 4,
    coverImageUrl: null,
  },
  {
    id: "2",
    title: "Tokyo Adventure",
    destination: "Tokyo, Japan",
    startDate: "Sep 1",
    endDate: "Sep 14",
    status: "DRAFT" as const,
    participantCount: 2,
    coverImageUrl: null,
  },
  {
    id: "3",
    title: "Road Trip California",
    destination: "California, USA",
    startDate: "Jul 10",
    endDate: "Jul 20",
    status: "ACTIVE" as const,
    participantCount: 5,
    coverImageUrl: null,
  },
  {
    id: "4",
    title: "Bali Retreat",
    destination: "Bali, Indonesia",
    startDate: "Nov 5",
    endDate: "Nov 15",
    status: "COMPLETED" as const,
    participantCount: 3,
    coverImageUrl: null,
  },
];

export default function DashboardPage() {
  const [createOpen, setCreateOpen] = useState(false);

  return (
    <>
      <CreateTripModal open={createOpen} onClose={() => setCreateOpen(false)} />

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
            {SAMPLE_TRIPS.length} trip{SAMPLE_TRIPS.length !== 1 ? "s" : ""}
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
          <Input placeholder="Search trips..." className="pl-10" />
        </div>
        <Button variant="secondary" size="md">
          <Filter size={14} />
          Filters
        </Button>
      </div>

      {/* Trip grid */}
      <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {SAMPLE_TRIPS.map((trip) => (
          <TripCard key={trip.id} {...trip} />
        ))}
      </div>
    </>
  );
}
