"use client";

import { useState, type FormEvent } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Loader2 } from "lucide-react";
import { GlassCard, Button, Input } from "@/components/ui";
import type { CreateTripRequest } from "@/lib/api";

interface CreateTripModalProps {
  open: boolean;
  onClose: () => void;
  onCreate?: (data: CreateTripRequest) => Promise<void>;
}

export default function CreateTripModal({ open, onClose, onCreate }: CreateTripModalProps) {
  const [title, setTitle] = useState("");
  const [destination, setDestination] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!onCreate) return;
    setLoading(true);
    try {
      await onCreate({
        title,
        destination,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        description: description || undefined,
      });
      // Reset form
      setTitle("");
      setDestination("");
      setStartDate("");
      setEndDate("");
      setDescription("");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AnimatePresence>
      {open && (
        <>
          {/* Backdrop */}
          <motion.div
            className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />

          {/* Modal */}
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              className="w-full max-w-lg"
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              transition={{ duration: 0.25 }}
            >
              <GlassCard variant="strong" className="relative space-y-5">
                <button
                  onClick={onClose}
                  className="absolute top-4 right-4 text-muted hover:text-foreground transition-colors"
                  aria-label="Close modal"
                >
                  <X size={20} />
                </button>

                <h2 className="text-xl font-bold">Create a new trip</h2>
                <p className="text-sm text-muted">
                  Fill in the basics — you can always add more details later.
                </p>

                <form onSubmit={handleSubmit} className="space-y-4">
                  <Input
                    id="title"
                    label="Trip title"
                    placeholder="e.g. Summer in Barcelona"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    required
                  />
                  <Input
                    id="destination"
                    label="Destination"
                    placeholder="e.g. Barcelona, Spain"
                    value={destination}
                    onChange={(e) => setDestination(e.target.value)}
                    required
                  />

                  <div className="grid gap-4 sm:grid-cols-2">
                    <Input
                      id="startDate"
                      label="Start date"
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                    />
                    <Input
                      id="endDate"
                      label="End date"
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                    />
                  </div>

                  <Input
                    id="description"
                    label="Description (optional)"
                    placeholder="A quick note about the trip..."
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                  />

                  <div className="flex gap-3 pt-2">
                    <Button variant="secondary" className="flex-1" type="button" onClick={onClose}>
                      Cancel
                    </Button>
                    <Button className="flex-1" type="submit" disabled={loading}>
                      {loading ? <Loader2 size={16} className="animate-spin" /> : "Create trip"}
                    </Button>
                  </div>
                </form>
              </GlassCard>
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  );
}
