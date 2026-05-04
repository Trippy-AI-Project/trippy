"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Bell,
  Plane,
  CreditCard,
  MessageSquare,
  CheckCheck,
  Info,
  Trash2,
} from "lucide-react";
import { notificationsApi, type Notification } from "@/lib/api";
import { isSafeInternalPath } from "@/lib/navigation";
import { cn } from "@/lib/utils";

const typeIcon: Record<string, typeof Bell> = {
  TRIP_INVITE: Plane,
  TRIP_JOINED: Plane,
  ITINERARY_READY: Plane,
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
  return `${days}d ago`;
}

export default function NotificationBell() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const fetchUnreadCount = useCallback(async () => {
    try {
      const data = await notificationsApi.unreadCount();
      setUnreadCount(data.count);
    } catch {
      // ignore
    }
  }, []);

  const loadRecentNotifications = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true);
    try {
      const data = await notificationsApi.list(0, 10);
      setNotifications(data.content);
    } catch {
      setNotifications([]);
    } finally {
      if (showLoading) setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initial = window.setTimeout(fetchUnreadCount, 0);
    const interval = setInterval(fetchUnreadCount, 30000);
    return () => {
      window.clearTimeout(initial);
      clearInterval(interval);
    };
  }, [fetchUnreadCount]);

  useEffect(() => {
    if (!open) return;
    const interval = setInterval(() => {
      loadRecentNotifications(false);
    }, 30000);
    return () => clearInterval(interval);
  }, [loadRecentNotifications, open]);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    if (open) document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open]);

  async function handleMarkAllRead() {
    try {
      await notificationsApi.markAllRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {
      // ignore
    }
  }

  function handleToggleOpen() {
    const nextOpen = !open;
    setOpen(nextOpen);
    if (nextOpen) {
      loadRecentNotifications(true);
    }
  }

  async function handleClickNotification(n: Notification) {
    // Mark as read
    if (!n.read) {
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
    // Navigate
    setOpen(false);
    if (isSafeInternalPath(n.actionUrl)) {
      router.push(n.actionUrl);
    }
  }

  async function handleDeleteNotification(e: React.MouseEvent, id: string) {
    e.stopPropagation();
    try {
      await notificationsApi.deleteNotification(id);
      const removed = notifications.find((n) => n.id === id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      if (removed && !removed.read) {
        setUnreadCount((c) => Math.max(0, c - 1));
      }
    } catch {
      // ignore
    }
  }

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Bell icon with badge */}
      <button
        onClick={handleToggleOpen}
        className="relative flex items-center justify-center rounded-xl p-2 text-muted hover:text-foreground hover:bg-surface transition-all"
        aria-label="Notifications"
      >
        <Bell size={18} />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-danger px-1 text-[10px] font-bold text-white">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown panel */}
      {open && (
        <div className="fixed inset-x-3 top-16 z-50 flex max-h-[calc(100vh-5rem)] flex-col glass-strong shadow-2xl sm:absolute sm:inset-auto sm:right-0 sm:top-full sm:mt-2 sm:w-96 sm:max-h-[28rem]">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-border">
            <h3 className="font-semibold text-sm">Notifications</h3>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                className="flex items-center gap-1 text-xs text-trippy-400 hover:text-trippy-300 transition-colors"
              >
                <CheckCheck size={12} /> Mark all read
              </button>
            )}
          </div>

          {/* Notification list */}
          <div className="flex-1 overflow-y-auto">
            {loading ? (
              <div className="p-8 text-center text-sm text-muted">Loading...</div>
            ) : notifications.length === 0 ? (
              <div className="p-8 text-center">
                <Bell size={24} className="mx-auto mb-2 text-muted" />
                <p className="text-sm text-muted">You&apos;re all caught up!</p>
              </div>
            ) : (
              notifications.map((n) => {
                const Icon = typeIcon[n.type] ?? Bell;
                return (
                  <button
                    key={n.id}
                    onClick={() => handleClickNotification(n)}
                    className={cn(
                      "group flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-surface transition-colors",
                      !n.read && "bg-trippy-500/5",
                    )}
                  >
                    <div
                      className={cn(
                        "mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full",
                        !n.read ? "bg-trippy-500/15 text-trippy-500" : "bg-surface text-muted",
                      )}
                    >
                      <Icon size={14} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className={cn("text-sm line-clamp-1", !n.read && "font-medium")}>
                        {n.title}
                      </p>
                      <p className="text-xs text-muted line-clamp-2 mt-0.5">
                        {n.message}
                      </p>
                      <p className="text-[10px] text-muted mt-1">
                        {timeAgo(n.createdAt)}
                      </p>
                    </div>
                    {!n.read && (
                      <span className="mt-2 h-2 w-2 shrink-0 rounded-full bg-trippy-500" />
                    )}
                    <button
                      onClick={(e) => handleDeleteNotification(e, n.id)}
                      className="mt-1 shrink-0 rounded p-1 text-muted hover:text-danger hover:bg-danger/10 transition-colors opacity-0 group-hover:opacity-100"
                      aria-label="Delete notification"
                    >
                      <Trash2 size={12} />
                    </button>
                  </button>
                );
              })
            )}
          </div>

          {/* Footer */}
          <div className="border-t border-border px-4 py-2">
            <Link
              href="/notifications"
              onClick={() => setOpen(false)}
              className="text-xs text-trippy-400 hover:text-trippy-300 transition-colors"
            >
              View All →
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
