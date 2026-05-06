"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { GlassCard, Avatar, Button } from "@/components/ui";
import { useAuth } from "@/lib/auth-context";
import { Mail, MapPin, Edit, LogOut } from "lucide-react";

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [loggingOut, setLoggingOut] = useState(false);

  async function handleLogout() {
    setLoggingOut(true);
    await logout();
    router.replace("/login");
  }

  return (
    <div className="space-y-8">
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Profile</h1>
        <p className="mt-1 text-muted">Your public profile information</p>
      </motion.div>

      <GlassCard className="flex flex-col items-center gap-4 sm:flex-row sm:items-start">
        <Avatar name={user?.displayName ?? "User"} src={user?.avatarUrl} size="lg" />
        <div className="flex-1 text-center sm:text-left">
          <h2 className="text-xl font-bold">{user?.displayName ?? "Unknown"}</h2>
          <div className="mt-2 flex flex-col gap-1 text-sm text-muted">
            {user?.email && (
              <p className="flex items-center gap-2 justify-center sm:justify-start">
                <Mail size={14} /> {user.email}
              </p>
            )}
            {user?.country && (
              <p className="flex items-center gap-2 justify-center sm:justify-start">
                <MapPin size={14} /> {user.country}
              </p>
            )}
          </div>
          {user?.bio && <p className="mt-3 text-sm">{user.bio}</p>}
        </div>
        <Button variant="secondary" size="sm">
          <Edit size={14} /> Edit
        </Button>
      </GlassCard>

      <GlassCard className="border-red-500/20">
        <h3 className="text-base font-semibold">Danger Zone</h3>
        <p className="mt-1 text-sm text-muted">Sign out of your account on this device.</p>
        <div className="mt-4">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleLogout}
            disabled={loggingOut}
            className="border-red-500/40 text-red-500 hover:bg-red-500/10"
          >
            <LogOut size={14} />
            {loggingOut ? "Signing out…" : "Sign out"}
          </Button>
        </div>
      </GlassCard>
    </div>
  );
}
