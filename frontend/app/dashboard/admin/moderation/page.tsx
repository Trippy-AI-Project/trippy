"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Shield,
  Ban,
  MicOff,
  Trash2,
  RotateCcw,
  Loader2,
  Check,
  AlertCircle,
  Search,
  User as UserIcon,
  X,
} from "lucide-react";

import { useAuth } from "@/lib/auth-context";
import {
  moderationApi,
  usersApi,
  type UserPublicProfile,
} from "@/lib/api";
import { GlassCard } from "@/components/ui";

type ActionState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "success"; message: string }
  | { kind: "error"; message: string };

const idle: ActionState = { kind: "idle" };

export default function ModerationPage() {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  // Each card has its own picked target + state
  const [banTarget, setBanTarget] = useState<UserPublicProfile | null>(null);
  const [banDuration, setBanDuration] = useState("0");
  const [banState, setBanState] = useState<ActionState>(idle);

  const [unbanTarget, setUnbanTarget] = useState<UserPublicProfile | null>(null);
  const [unbanState, setUnbanState] = useState<ActionState>(idle);

  const [muteTarget, setMuteTarget] = useState<UserPublicProfile | null>(null);
  const [muteDuration, setMuteDuration] = useState("0");
  const [muteState, setMuteState] = useState<ActionState>(idle);

  const [unmuteTarget, setUnmuteTarget] = useState<UserPublicProfile | null>(null);
  const [unmuteState, setUnmuteState] = useState<ActionState>(idle);

  const [messageId, setMessageId] = useState("");
  const [delState, setDelState] = useState<ActionState>(idle);

  // ADMIN-only guard
  useEffect(() => {
    if (isLoading) return;
    if (!user) {
      router.replace("/login");
      return;
    }
    if (user.role !== "ADMIN") {
      router.replace("/dashboard");
    }
  }, [user, isLoading, router]);

  if (isLoading || !user || user.role !== "ADMIN") {
    return (
      <div className="flex h-[60vh] items-center justify-center">
        <Loader2 className="size-6 animate-spin text-emerald-400" />
      </div>
    );
  }

  const handleBan = async () => {
    if (!banTarget) {
      setBanState({ kind: "error", message: "Pick a user first." });
      return;
    }
    setBanState({ kind: "loading" });
    try {
      const mins = Number.parseInt(banDuration, 10);
      await moderationApi.banUser(banTarget.id, Number.isFinite(mins) ? mins : 0);
      const who = banTarget.email ?? banTarget.displayName;
      setBanState({
        kind: "success",
        message: `Banned ${who}${mins > 0 ? ` for ${mins} min` : " (max 30 days)"}.`,
      });
    } catch (e) {
      setBanState({ kind: "error", message: (e as Error).message });
    }
  };

  const handleUnban = async () => {
    if (!unbanTarget) {
      setUnbanState({ kind: "error", message: "Pick a user first." });
      return;
    }
    setUnbanState({ kind: "loading" });
    try {
      await moderationApi.unbanUser(unbanTarget.id);
      setUnbanState({
        kind: "success",
        message: `Unbanned ${unbanTarget.email ?? unbanTarget.displayName}.`,
      });
    } catch (e) {
      setUnbanState({ kind: "error", message: (e as Error).message });
    }
  };

  const handleMute = async () => {
    if (!muteTarget) {
      setMuteState({ kind: "error", message: "Pick a user first." });
      return;
    }
    setMuteState({ kind: "loading" });
    try {
      const mins = Number.parseInt(muteDuration, 10);
      await moderationApi.muteUser(muteTarget.id, Number.isFinite(mins) ? mins : 0);
      const who = muteTarget.email ?? muteTarget.displayName;
      setMuteState({
        kind: "success",
        message: `Muted ${who}${mins > 0 ? ` for ${mins} min` : " (max 30 days)"}.`,
      });
    } catch (e) {
      setMuteState({ kind: "error", message: (e as Error).message });
    }
  };

  const handleUnmute = async () => {
    if (!unmuteTarget) {
      setUnmuteState({ kind: "error", message: "Pick a user first." });
      return;
    }
    setUnmuteState({ kind: "loading" });
    try {
      await moderationApi.unmuteUser(unmuteTarget.id);
      setUnmuteState({
        kind: "success",
        message: `Unmuted ${unmuteTarget.email ?? unmuteTarget.displayName}.`,
      });
    } catch (e) {
      setUnmuteState({ kind: "error", message: (e as Error).message });
    }
  };

  const handleDelete = async () => {
    if (!messageId.trim()) {
      setDelState({ kind: "error", message: "Enter a message id." });
      return;
    }
    setDelState({ kind: "loading" });
    try {
      await moderationApi.deleteMessage(messageId.trim());
      setDelState({ kind: "success", message: `Message ${messageId.trim()} soft-deleted.` });
      setMessageId("");
    } catch (e) {
      setDelState({ kind: "error", message: (e as Error).message });
    }
  };

  return (
    <div className="space-y-6 p-6">
      <header className="flex items-center gap-3">
        <Shield className="size-7 text-emerald-400" />
        <div>
          <h1 className="text-2xl font-semibold text-white">Chat Moderation</h1>
          <p className="text-sm text-zinc-400">
            Search users by <span className="text-emerald-300">email or name</span> and apply moderation actions.
            Durations are in minutes — leave blank or 0 for the max (30 days).
          </p>
        </div>
      </header>

      <div className="grid gap-6 md:grid-cols-2">
        {/* BAN */}
        <GlassCard>
          <div className="space-y-3 p-5">
            <div className="flex items-center gap-2 text-red-300">
              <Ban className="size-5" />
              <h2 className="text-lg font-semibold">Ban User</h2>
            </div>
            <UserPicker
              label="Find user"
              value={banTarget}
              onChange={setBanTarget}
              placeholder="Type email or display name…"
            />
            <label className="block text-sm text-zinc-300">
              Duration (minutes, 0 = max)
              <input
                type="number"
                min="0"
                value={banDuration}
                onChange={(e) => setBanDuration(e.target.value)}
                className="mt-1 w-full rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-white outline-none focus:border-red-400"
              />
            </label>
            <button
              onClick={handleBan}
              disabled={banState.kind === "loading"}
              className="inline-flex items-center gap-2 rounded-lg bg-red-500/80 px-4 py-2 text-sm font-semibold text-white hover:bg-red-500 disabled:opacity-50"
            >
              {banState.kind === "loading" ? <Loader2 className="size-4 animate-spin" /> : <Ban className="size-4" />}
              Ban user
            </button>
            <StateLine state={banState} />
          </div>
        </GlassCard>

        {/* UNBAN */}
        <GlassCard>
          <div className="space-y-3 p-5">
            <div className="flex items-center gap-2 text-emerald-300">
              <RotateCcw className="size-5" />
              <h2 className="text-lg font-semibold">Unban User</h2>
            </div>
            <UserPicker
              label="Find user"
              value={unbanTarget}
              onChange={setUnbanTarget}
              placeholder="Type email or display name…"
            />
            <button
              onClick={handleUnban}
              disabled={unbanState.kind === "loading"}
              className="inline-flex items-center gap-2 rounded-lg bg-emerald-500/80 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500 disabled:opacity-50"
            >
              {unbanState.kind === "loading" ? <Loader2 className="size-4 animate-spin" /> : <RotateCcw className="size-4" />}
              Unban user
            </button>
            <StateLine state={unbanState} />
          </div>
        </GlassCard>

        {/* MUTE */}
        <GlassCard>
          <div className="space-y-3 p-5">
            <div className="flex items-center gap-2 text-amber-300">
              <MicOff className="size-5" />
              <h2 className="text-lg font-semibold">Mute User</h2>
            </div>
            <UserPicker
              label="Find user"
              value={muteTarget}
              onChange={setMuteTarget}
              placeholder="Type email or display name…"
            />
            <label className="block text-sm text-zinc-300">
              Duration (minutes, 0 = max)
              <input
                type="number"
                min="0"
                value={muteDuration}
                onChange={(e) => setMuteDuration(e.target.value)}
                className="mt-1 w-full rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-white outline-none focus:border-amber-400"
              />
            </label>
            <button
              onClick={handleMute}
              disabled={muteState.kind === "loading"}
              className="inline-flex items-center gap-2 rounded-lg bg-amber-500/80 px-4 py-2 text-sm font-semibold text-white hover:bg-amber-500 disabled:opacity-50"
            >
              {muteState.kind === "loading" ? <Loader2 className="size-4 animate-spin" /> : <MicOff className="size-4" />}
              Mute user
            </button>
            <StateLine state={muteState} />
          </div>
        </GlassCard>

        {/* UNMUTE */}
        <GlassCard>
          <div className="space-y-3 p-5">
            <div className="flex items-center gap-2 text-emerald-300">
              <RotateCcw className="size-5" />
              <h2 className="text-lg font-semibold">Unmute User</h2>
            </div>
            <UserPicker
              label="Find user"
              value={unmuteTarget}
              onChange={setUnmuteTarget}
              placeholder="Type email or display name…"
            />
            <button
              onClick={handleUnmute}
              disabled={unmuteState.kind === "loading"}
              className="inline-flex items-center gap-2 rounded-lg bg-emerald-500/80 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500 disabled:opacity-50"
            >
              {unmuteState.kind === "loading" ? <Loader2 className="size-4 animate-spin" /> : <RotateCcw className="size-4" />}
              Unmute user
            </button>
            <StateLine state={unmuteState} />
          </div>
        </GlassCard>

        {/* DELETE MESSAGE — messages have no human handle, keep raw ID input */}
        <GlassCard>
          <div className="space-y-3 p-5 md:col-span-2">
            <div className="flex items-center gap-2 text-rose-300">
              <Trash2 className="size-5" />
              <h2 className="text-lg font-semibold">Delete Message</h2>
            </div>
            <label className="block text-sm text-zinc-300">
              Message ID (UUID)
              <input
                type="text"
                value={messageId}
                onChange={(e) => setMessageId(e.target.value)}
                placeholder="00000000-0000-0000-0000-000000000000"
                className="mt-1 w-full rounded-lg border border-white/10 bg-black/30 px-3 py-2 font-mono text-sm text-white outline-none focus:border-rose-400"
              />
            </label>
            <p className="text-xs text-zinc-500">
              Messages don&apos;t have a friendly handle. Grab the ID from chat-service logs or the websocket payload.
            </p>
            <button
              onClick={handleDelete}
              disabled={delState.kind === "loading"}
              className="inline-flex items-center gap-2 rounded-lg bg-rose-500/80 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-500 disabled:opacity-50"
            >
              {delState.kind === "loading" ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
              Delete message
            </button>
            <StateLine state={delState} />
          </div>
        </GlassCard>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  StateLine                                                          */
/* ------------------------------------------------------------------ */

function StateLine({ state }: { state: ActionState }) {
  if (state.kind === "idle") return null;
  if (state.kind === "loading") {
    return (
      <div className="flex items-center gap-2 text-sm text-zinc-400">
        <Loader2 className="size-4 animate-spin" /> Working…
      </div>
    );
  }
  if (state.kind === "success") {
    return (
      <div className="flex items-center gap-2 text-sm text-emerald-300">
        <Check className="size-4" /> {state.message}
      </div>
    );
  }
  return (
    <div className="flex items-center gap-2 text-sm text-red-300">
      <AlertCircle className="size-4" /> {state.message}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  UserPicker — debounced email/name search with dropdown             */
/* ------------------------------------------------------------------ */

interface UserPickerProps {
  label: string;
  value: UserPublicProfile | null;
  onChange: (user: UserPublicProfile | null) => void;
  placeholder?: string;
}

function UserPicker({ label, value, onChange, placeholder }: UserPickerProps) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<UserPublicProfile[]>([]);
  const [open, setOpen] = useState(false);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // Debounced search
  useEffect(() => {
    if (value) return; // already picked, no need to search
    const q = query.trim();
    if (q.length < 2) {
      setResults([]);
      setError(null);
      return;
    }
    setSearching(true);
    setError(null);
    const handle = window.setTimeout(async () => {
      try {
        const r = await usersApi.search(q, 8);
        setResults(r);
        setOpen(true);
      } catch (e) {
        setError((e as Error).message);
        setResults([]);
      } finally {
        setSearching(false);
      }
    }, 250);
    return () => window.clearTimeout(handle);
  }, [query, value]);

  // Close dropdown on outside click
  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!containerRef.current) return;
      if (!containerRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onDocClick);
    return () => document.removeEventListener("mousedown", onDocClick);
  }, []);

  // Picked state: show a "pill" with the user, plus a clear button
  if (value) {
    return (
      <div className="space-y-1">
        <span className="block text-sm text-zinc-300">{label}</span>
        <div className="flex items-center justify-between gap-3 rounded-lg border border-emerald-400/40 bg-emerald-400/5 px-3 py-2">
          <div className="flex min-w-0 items-center gap-2">
            <UserIcon className="size-4 shrink-0 text-emerald-300" />
            <div className="min-w-0">
              <div className="truncate text-sm font-medium text-white">
                {value.displayName}
              </div>
              {value.email && (
                <div className="truncate text-xs text-zinc-400">{value.email}</div>
              )}
            </div>
          </div>
          <button
            type="button"
            onClick={() => {
              onChange(null);
              setQuery("");
              setResults([]);
            }}
            className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs text-zinc-300 hover:bg-white/5 hover:text-white"
          >
            <X className="size-3" /> change
          </button>
        </div>
      </div>
    );
  }

  return (
    <div ref={containerRef} className="relative space-y-1">
      <label className="block text-sm text-zinc-300">{label}</label>
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-zinc-500" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => results.length > 0 && setOpen(true)}
          placeholder={placeholder ?? "Search…"}
          className="w-full rounded-lg border border-white/10 bg-black/30 py-2 pl-9 pr-9 text-white outline-none focus:border-emerald-400"
        />
        {searching && (
          <Loader2 className="absolute right-3 top-1/2 size-4 -translate-y-1/2 animate-spin text-zinc-400" />
        )}
      </div>

      {error && (
        <p className="text-xs text-red-300">{error}</p>
      )}

      {open && results.length > 0 && (
        <ul className="absolute z-10 mt-1 max-h-64 w-full overflow-auto rounded-lg border border-white/10 bg-zinc-900/95 shadow-xl backdrop-blur">
          {results.map((u) => (
            <li key={u.id}>
              <button
                type="button"
                onClick={() => {
                  onChange(u);
                  setOpen(false);
                  setQuery("");
                  setResults([]);
                }}
                className="flex w-full items-center gap-3 px-3 py-2 text-left hover:bg-white/5"
              >
                <UserIcon className="size-4 shrink-0 text-zinc-400" />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm text-white">{u.displayName}</div>
                  {u.email && (
                    <div className="truncate text-xs text-zinc-400">{u.email}</div>
                  )}
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}

      {open && !searching && query.trim().length >= 2 && results.length === 0 && !error && (
        <p className="text-xs text-zinc-500">No users match &ldquo;{query}&rdquo;.</p>
      )}
    </div>
  );
}
