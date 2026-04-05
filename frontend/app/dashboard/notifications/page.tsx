"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  Bell,
  Plane,
  CreditCard,
  MessageSquare,
  Info,
  CheckCheck,
  Loader2,
  Trash2,
} from "lucide-react";
import { motion } from "framer-motion";
import { GlassCard, Button, Badge } from "@/components/ui";
import { notificationsApi, type Notification } from "@/lib/api";
import { cn } from "@/lib/utils";

const typeIcon: Record<string, typeof Bell> = {
  TRIP_INVITATION: Plane,
  INVITATION_ACCEPTED: Plane,
  INVITATION_DECLINED: Plane,
  TRIP_UPDATED: Plane,
  TRIP_CANCELLED: Plane,
  PAYMENT_SUCCESS: CreditCard,
  PAYMENT_FAILED: CreditCard,
  SUBSCRIPTION_EXPIRING: CreditCard,
  NEW_MESSAGE: MessageSquare,
  SYSTEM: Info,
};

function timeAgo(dateStr: string): string {
  const seconds = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(dateStr).toLocaleDateString();
}

export default function NotificationsPage() {
  const router = useRouter();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const [data, unread] = await Promise.all([
        notificationsApi.list(page, 20),
        notificationsApi.unreadCount(),
      ]);
      setNotifications(data.content);
      setTotalPages(data.totalPages);
      setUnreadCount(unread.count);
    } catch {
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  async function handleMarkAllRead() {
    try {
      await notificationsApi.markAllRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {
      // ignore
    }
  }

  async function handleMarkRead(n: Notification) {
    if (n.read) return;
    try {
      await notificationsApi.markRead(n.id);
      setNotifications((prev) =>
        prev.map((x) => (x.id === n.id ? { ...x, read: true } : x)),
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch {
      // ignore
    }
  }

  function handleClick(n: Notification) {
    handleMarkRead(n);
    if (n.actionUrl) router.push(n.actionUrl);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
            Notifications
          </h1>
          <p className="mt-1 text-muted">
            {unreadCount > 0
              ? `${unreadCount} unread notification${unreadCount !== 1 ? "s" : ""}`
              : "All caught up!"}
          </p>
        </motion.div>

        {unreadCount > 0 && (
          <Button variant="secondary" size="sm" onClick={handleMarkAllRead}>
            <CheckCheck size={14} /> Mark all as read
          </Button>
        )}
      </div>

      {loading ? (
        <div className="mt-16 flex justify-center">
          <Loader2 size={32} className="animate-spin text-trippy-500" />
        </div>
      ) : notifications.length === 0 ? (
        <motion.div
          className="mt-16 flex flex-col items-center text-center"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="flex h-24 w-24 items-center justify-center rounded-full bg-trippy-500/10">
            <Bell size={40} className="text-trippy-500" />
          </div>
          <h3 className="mt-6 text-lg font-semibold">You&apos;re all caught up!</h3>
          <p className="mt-2 text-muted max-w-sm">
            No notifications yet. When something happens, you&apos;ll see it here.
          </p>
        </motion.div>
      ) : (
        <>
          <div className="space-y-2">
            {notifications.map((n, i) => {
              const Icon = typeIcon[n.type] ?? Bell;
              return (
                <motion.div
                  key={n.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.03 }}
                >
                  <GlassCard
                    className={cn(
                      "flex items-start gap-4 p-4 cursor-pointer hover:bg-surface-hover transition-all",
                      !n.read && "ring-1 ring-trippy-500/20 bg-trippy-500/5",
                    )}
                    onClick={() => handleClick(n)}
                  >
                    <div
                      className={cn(
                        "mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-full",
                        !n.read
                          ? "bg-trippy-500/15 text-trippy-500"
                          : "bg-surface text-muted",
                      )}
                    >
                      <Icon size={18} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <p className={cn("text-sm", !n.read && "font-semibold")}>
                          {n.title}
                        </p>
                        {!n.read && (
                          <span className="h-2 w-2 rounded-full bg-trippy-500 shrink-0" />
                        )}
                      </div>
                      <p className="text-sm text-muted mt-0.5">{n.message}</p>
                      <p className="text-xs text-muted mt-1">{timeAgo(n.createdAt)}</p>
                    </div>
                  </GlassCard>
                </motion.div>
              );
            })}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 pt-4">
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
    </div>
  );
}
