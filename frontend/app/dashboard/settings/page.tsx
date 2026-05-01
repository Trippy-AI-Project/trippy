"use client";

import { motion } from "framer-motion";
import { Settings, User, Bell, Shield, Palette } from "lucide-react";
import { GlassCard } from "@/components/ui";

export default function SettingsPage() {
  return (
    <div className="space-y-8">
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Settings</h1>
        <p className="mt-1 text-muted">Manage your account and preferences</p>
      </motion.div>

      <div className="grid gap-4 sm:grid-cols-2">
        <GlassCard className="flex items-start gap-4 cursor-pointer hover:bg-surface-hover transition-all">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-trippy-500/10">
            <User size={20} className="text-trippy-500" />
          </div>
          <div>
            <h3 className="font-semibold">Profile</h3>
            <p className="text-sm text-muted mt-1">Update your name, bio, avatar, and contact info</p>
          </div>
        </GlassCard>

        <GlassCard className="flex items-start gap-4 cursor-pointer hover:bg-surface-hover transition-all">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-trippy-500/10">
            <Bell size={20} className="text-trippy-500" />
          </div>
          <div>
            <h3 className="font-semibold">Notifications</h3>
            <p className="text-sm text-muted mt-1">Configure email and push notification preferences</p>
          </div>
        </GlassCard>

        <GlassCard className="flex items-start gap-4 cursor-pointer hover:bg-surface-hover transition-all">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-trippy-500/10">
            <Shield size={20} className="text-trippy-500" />
          </div>
          <div>
            <h3 className="font-semibold">Security</h3>
            <p className="text-sm text-muted mt-1">Change your password and manage sessions</p>
          </div>
        </GlassCard>

        <GlassCard className="flex items-start gap-4 cursor-pointer hover:bg-surface-hover transition-all">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-trippy-500/10">
            <Palette size={20} className="text-trippy-500" />
          </div>
          <div>
            <h3 className="font-semibold">Appearance</h3>
            <p className="text-sm text-muted mt-1">Toggle dark mode and customize your theme</p>
          </div>
        </GlassCard>
      </div>
    </div>
  );
}
