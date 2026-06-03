"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { Shield, Ban, MicOff, Trash2, RotateCcw, Loader2, Check, AlertCircle } from "lucide-react";
import { GlassCard, Button, Input } from "@/components/ui";
import { useAuth } from "@/lib/auth-context";
import { moderationApi, ApiError } from "@/lib/api";

type ActionState = { kind: "idle" } | { kind: "loading" } | { kind: "success"; message: string } | { kind: "error"; message: string };

export default function ModerationPage() {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  // Gate: ADMIN-only. Members get redirected; UI also hides itself.
  useEffect(() => {
    if (!isLoading && user && user.role !== "ADMIN") {
      router.replace("/dashboard");
    }
  }, [isLoading, user, router]);

  const [banUserId, setBanUserId] = useState("");
  const [banDuration, setBanDuration] = useState("60");
  const [muteUserId, setMuteUserId] = useState("");
  const [muteDuration, setMuteDuration] = useState("30");
  const [messageId, setMessageId] = useState("");
  const [unbanUserId, setUnbanUserId] = useState("");
  const [unmuteUserId, setUnmuteUserId] = useState("");

  const [banState, setBanState] = useState<ActionState>({ kind: "idle" });
  const [muteState, setMuteState] = useState<ActionState>({ kind: "idle" });
  const [deleteState, setDeleteState] = useState<ActionState>({ kind: "idle" });
  const [unbanState, setUnbanState] = useState<ActionState>({ kind: "idle" });
  const [unmuteState, setUnmuteState] = useState<ActionState>({ kind: "idle" });

  if (isLoading || !user || user.role !== "ADMIN") {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-6 w-6 animate-spin text-muted" />
      </div>
    );
  }

  async function run(
    setState: (s: ActionState) => void,
    successMsg: string,
    fn: () => Promise<unknown>,
  ) {
    setState({ kind: "loading" });
    try {
      await fn();
      setState({ kind: "success", message: successMsg });
    } catch (err) {
      const message = err instanceof ApiError
        ? err.status === 403
          ? "Forbidden — ADMIN role required"
          : (err.message ?? `Request failed (${err.status})`)
        : err instanceof Error
          ? err.message
          : "Unknown error";
      setState({ kind: "error", message });
    }
  }

  return (
    <div>
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-6 flex items-center gap-3"
      >
        <Shield className="h-7 w-7 text-trippy-400" />
        <div>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Chat Moderation</h1>
          <p className="mt-1 text-sm text-muted">Ban, mute or delete content in chat rooms</p>
        </div>
      </motion.div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* ----- BAN ----- */}
        <GlassCard className="p-6">
          <div className="mb-4 flex items-center gap-2">
            <Ban className="h-5 w-5 text-danger" />
            <h2 className="text-lg font-semibold">Ban user</h2>
          </div>
          <p className="mb-4 text-sm text-muted">Banned users cannot send chat messages. Use <code>0</code> for the maximum (30 days).</p>
          <div className="space-y-3">
            <Input placeholder="User ID (UUID)" value={banUserId} onChange={(e) => setBanUserId(e.target.value)} />
            <Input placeholder="Duration (minutes)" type="number" min="0" value={banDuration} onChange={(e) => setBanDuration(e.target.value)} />
            <Button
              variant="danger"
              disabled={!banUserId || banState.kind === "loading"}
              onClick={() => run(setBanState, "User banned", () =>
                moderationApi.banUser(banUserId.trim(), Number(banDuration) || 0))}
            >
              {banState.kind === "loading" ? <Loader2 className="h-4 w-4 animate-spin" /> : <Ban className="h-4 w-4" />}
              Ban user
            </Button>
            <StateLine state={banState} />
          </div>
        </GlassCard>

        {/* ----- UNBAN ----- */}
        <GlassCard className="p-6">
          <div className="mb-4 flex items-center gap-2">
            <RotateCcw className="h-5 w-5 text-trippy-400" />
            <h2 className="text-lg font-semibold">Lift ban</h2>
          </div>
          <p className="mb-4 text-sm text-muted">Immediately remove an active ban.</p>
          <div className="space-y-3">
            <Input placeholder="User ID (UUID)" value={unbanUserId} onChange={(e) => setUnbanUserId(e.target.value)} />
            <Button
              disabled={!unbanUserId || unbanState.kind === "loading"}
              onClick={() => run(setUnbanState, "Ban lifted", () =>
                moderationApi.unbanUser(unbanUserId.trim()))}
            >
              {unbanState.kind === "loading" ? <Loader2 className="h-4 w-4 animate-spin" /> : <RotateCcw className="h-4 w-4" />}
              Unban
            </Button>
            <StateLine state={unbanState} />
          </div>
        </GlassCard>

        {/* ----- MUTE ----- */}
        <GlassCard className="p-6">
          <div className="mb-4 flex items-center gap-2">
            <MicOff className="h-5 w-5 text-accent-400" />
            <h2 className="text-lg font-semibold">Mute user</h2>
          </div>
          <p className="mb-4 text-sm text-muted">Muted users can read but not send messages.</p>
          <div className="space-y-3">
            <Input placeholder="User ID (UUID)" value={muteUserId} onChange={(e) => setMuteUserId(e.target.value)} />
            <Input placeholder="Duration (minutes)" type="number" min="0" value={muteDuration} onChange={(e) => setMuteDuration(e.target.value)} />
            <Button
              disabled={!muteUserId || muteState.kind === "loading"}
              onClick={() => run(setMuteState, "User muted", () =>
                moderationApi.muteUser(muteUserId.trim(), Number(muteDuration) || 0))}
            >
              {muteState.kind === "loading" ? <Loader2 className="h-4 w-4 animate-spin" /> : <MicOff className="h-4 w-4" />}
              Mute user
            </Button>
            <StateLine state={muteState} />
          </div>
        </GlassCard>

        {/* ----- UNMUTE ----- */}
        <GlassCard className="p-6">
          <div className="mb-4 flex items-center gap-2">
            <RotateCcw className="h-5 w-5 text-trippy-400" />
            <h2 className="text-lg font-semibold">Lift mute</h2>
          </div>
          <p className="mb-4 text-sm text-muted">Restore message-sending privileges immediately.</p>
          <div className="space-y-3">
            <Input placeholder="User ID (UUID)" value={unmuteUserId} onChange={(e) => setUnmuteUserId(e.target.value)} />
            <Button
              disabled={!unmuteUserId || unmuteState.kind === "loading"}
              onClick={() => run(setUnmuteState, "Mute lifted", () =>
                moderationApi.unmuteUser(unmuteUserId.trim()))}
            >
              {unmuteState.kind === "loading" ? <Loader2 className="h-4 w-4 animate-spin" /> : <RotateCcw className="h-4 w-4" />}
              Unmute
            </Button>
            <StateLine state={unmuteState} />
          </div>
        </GlassCard>

        {/* ----- DELETE MESSAGE ----- */}
        <GlassCard className="p-6 md:col-span-2">
          <div className="mb-4 flex items-center gap-2">
            <Trash2 className="h-5 w-5 text-danger" />
            <h2 className="text-lg font-semibold">Delete message</h2>
          </div>
          <p className="mb-4 text-sm text-muted">Soft-deletes the message (still in DB, hidden from clients).</p>
          <div className="flex flex-col gap-3 sm:flex-row">
            <Input
              placeholder="Message ID (UUID)"
              value={messageId}
              onChange={(e) => setMessageId(e.target.value)}
              className="flex-1"
            />
            <Button
              variant="danger"
              disabled={!messageId || deleteState.kind === "loading"}
              onClick={() => run(setDeleteState, "Message deleted", () =>
                moderationApi.deleteMessage(messageId.trim()))}
            >
              {deleteState.kind === "loading" ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
              Delete
            </Button>
          </div>
          <div className="mt-3">
            <StateLine state={deleteState} />
          </div>
        </GlassCard>
      </div>
    </div>
  );
}

function StateLine({ state }: { state: ActionState }) {
  if (state.kind === "idle" || state.kind === "loading") return null;
  const isSuccess = state.kind === "success";
  return (
    <div className={`flex items-center gap-2 text-sm ${isSuccess ? "text-trippy-400" : "text-danger"}`}>
      {isSuccess ? <Check className="h-4 w-4" /> : <AlertCircle className="h-4 w-4" />}
      <span>{state.message}</span>
    </div>
  );
}
