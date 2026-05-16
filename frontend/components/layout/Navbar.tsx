"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Map,
  MessageSquare,
  Settings,
  LogOut,
  Menu,
  X,
  CreditCard,
  UserCircle,
} from "lucide-react";
import { useState } from "react";
import Logo from "@/components/Logo";
import { Avatar } from "@/components/ui";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/auth-context";
import NotificationBell from "@/components/notifications/NotificationBell";

const navLinks = [
  { href: "/dashboard", label: "My Trips", icon: Map },
  { href: "/dashboard/chat", label: "Chat", icon: MessageSquare },
  { href: "/dashboard/payments", label: "Billing", icon: CreditCard },
  { href: "/dashboard/settings", label: "Settings", icon: Settings },
  { href: "/dashboard/profile", label: "Profile", icon: UserCircle },
];

export default function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  async function handleLogout() {
    await logout();
    router.push("/login");
  }

  return (
    <nav className="glass-strong sticky top-0 z-50 px-4 lg:px-8">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between">
        {/* Logo */}
        <Link href="/dashboard">
          <Logo size="sm" />
        </Link>

        {/* Desktop nav */}
        <div className="hidden md:flex items-center gap-1">
          {navLinks.map(({ href, label, icon: Icon }) => {
            const active = pathname === href || pathname.startsWith(href + "/");
            return (
              <Link
                key={href}
                href={href}
                className={cn(
                  "flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium transition-all duration-200",
                  active
                    ? "bg-trippy-500/15 text-trippy-400"
                    : "text-muted hover:text-foreground hover:bg-surface"
                )}
              >
                <Icon size={16} />
                {label}
              </Link>
            );
          })}
        </div>

        {/* Right side */}
        <div className="flex items-center gap-2">
          <NotificationBell />
          <Link href="/dashboard/profile">
            <Avatar
              name={user?.displayName ?? "User"}
              src={user?.avatarUrl}
              size="sm"
              className="cursor-pointer hover:ring-2 hover:ring-trippy-400/50 rounded-full transition-all"
            />
          </Link>

          {/* Mobile hamburger */}
          <button
            className="md:hidden text-muted hover:text-foreground transition-colors"
            onClick={() => setMobileOpen(!mobileOpen)}
            aria-label="Toggle navigation"
          >
            {mobileOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>

      {/* Mobile drawer */}
      {mobileOpen && (
        <div className="md:hidden border-t border-border pb-4 pt-2">
          {navLinks.map(({ href, label, icon: Icon }) => {
            const active = pathname === href || pathname.startsWith(href + "/");
            return (
              <Link
                key={href}
                href={href}
                onClick={() => setMobileOpen(false)}
                className={cn(
                  "flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-all",
                  active
                    ? "bg-trippy-500/15 text-trippy-400"
                    : "text-muted hover:text-foreground hover:bg-surface"
                )}
              >
                <Icon size={16} />
                {label}
              </Link>
            );
          })}
          <Link
            href="/dashboard/notifications"
            onClick={() => setMobileOpen(false)}
            className="flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-muted hover:text-foreground hover:bg-surface transition-all"
          >
            Notifications
          </Link>
          <button
            onClick={() => { setMobileOpen(false); handleLogout(); }}
            className="flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-danger hover:bg-danger/10 w-full transition-all"
          >
            <LogOut size={16} />
            Logout
          </button>
        </div>
      )}
    </nav>
  );
}
