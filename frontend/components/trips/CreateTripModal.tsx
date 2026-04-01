"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X } from "lucide-react";
import { GlassCard, Button, Input } from "@/components/ui";

interface CreateTripModalProps {
  open: boolean;
  onClose: () => void;
}

export default function CreateTripModal({ open, onClose }: CreateTripModalProps) {
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

                <div className="space-y-4">
                  <Input id="title" label="Trip title" placeholder="e.g. Summer in Barcelona" />
                  <Input id="destination" label="Destination" placeholder="e.g. Barcelona, Spain" />

                  <div className="grid gap-4 sm:grid-cols-2">
                    <Input id="startDate" label="Start date" type="date" />
                    <Input id="endDate" label="End date" type="date" />
                  </div>

                  <Input
                    id="description"
                    label="Description (optional)"
                    placeholder="A quick note about the trip..."
                  />
                </div>

                <div className="flex gap-3 pt-2">
                  <Button variant="secondary" className="flex-1" onClick={onClose}>
                    Cancel
                  </Button>
                  <Button className="flex-1">
                    Create trip
                  </Button>
                </div>
              </GlassCard>
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  );
}
