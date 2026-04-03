"use client";

import Link from "next/link";
import { Mail, Lock, ArrowRight } from "lucide-react";
import { motion } from "framer-motion";
import Logo from "@/components/Logo";
import { GlassCard, Button, Input } from "@/components/ui";

export default function LoginPage() {
  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-12">
      {/* Ambient gradients */}
      <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
        <div className="absolute -top-40 -right-40 h-[500px] w-[500px] rounded-full bg-trippy-600/15 blur-3xl" />
        <div className="absolute bottom-0 left-0 h-[400px] w-[400px] rounded-full bg-accent-500/10 blur-3xl" />
      </div>

      <motion.div
        className="w-full max-w-md"
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
      >
        <div className="mb-8 text-center">
          <Link href="/" className="inline-block">
            <Logo size="lg" className="justify-center" />
          </Link>
          <p className="mt-3 text-muted">Welcome back, traveler</p>
        </div>

        <GlassCard variant="strong" className="space-y-5">
          <div className="space-y-4">
            <div className="relative">
              <Mail
                size={16}
                className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-muted"
              />
              <Input
                id="email"
                type="email"
                placeholder="Email address"
                className="pl-10"
              />
            </div>

            <div className="relative">
              <Lock
                size={16}
                className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-muted"
              />
              <Input
                id="password"
                type="password"
                placeholder="Password"
                className="pl-10"
              />
            </div>
          </div>

          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2 text-muted">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-border bg-surface accent-trippy-500"
              />
              Remember me
            </label>
            <Link
              href="/forgot-password"
              className="text-trippy-400 hover:text-trippy-300 transition-colors"
            >
              Forgot password?
            </Link>
          </div>

          <Button className="w-full" size="lg">
            Sign in <ArrowRight size={16} />
          </Button>

          <div className="relative my-2">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-border" />
            </div>
            <div className="relative flex justify-center text-xs">
              <span className="bg-transparent px-3 text-muted">or</span>
            </div>
          </div>

          <p className="text-center text-sm text-muted">
            Don&apos;t have an account?{" "}
            <Link
              href="/register"
              className="font-medium text-trippy-400 hover:text-trippy-300 transition-colors"
            >
              Sign up
            </Link>
          </p>
        </GlassCard>
      </motion.div>
    </div>
  );
}
