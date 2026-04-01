"use client";

import Link from "next/link";
import { Mail, Lock, User, ArrowRight } from "lucide-react";
import { motion } from "framer-motion";
import Logo from "@/components/Logo";
import { GlassCard, Button, Input } from "@/components/ui";

export default function RegisterPage() {
  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-12">
      {/* Ambient gradients */}
      <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
        <div className="absolute -top-40 -left-40 h-[500px] w-[500px] rounded-full bg-accent-500/12 blur-3xl" />
        <div className="absolute bottom-20 right-0 h-[400px] w-[400px] rounded-full bg-trippy-600/15 blur-3xl" />
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
          <p className="mt-3 text-muted">Start your journey</p>
        </div>

        <GlassCard variant="strong" className="space-y-5">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="relative">
              <User
                size={16}
                className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-muted"
              />
              <Input
                id="firstName"
                placeholder="First name"
                className="pl-10"
              />
            </div>
            <div className="relative">
              <User
                size={16}
                className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-muted"
              />
              <Input
                id="lastName"
                placeholder="Last name"
                className="pl-10"
              />
            </div>
          </div>

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
              placeholder="Create password"
              className="pl-10"
            />
          </div>

          <div className="relative">
            <Lock
              size={16}
              className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-muted"
            />
            <Input
              id="confirmPassword"
              type="password"
              placeholder="Confirm password"
              className="pl-10"
            />
          </div>

          <label className="flex items-start gap-2 text-sm text-muted">
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 rounded border-border bg-surface accent-trippy-500"
            />
            <span>
              I agree to the{" "}
              <span className="text-trippy-400 cursor-pointer hover:text-trippy-300">
                Terms of Service
              </span>{" "}
              and{" "}
              <span className="text-trippy-400 cursor-pointer hover:text-trippy-300">
                Privacy Policy
              </span>
            </span>
          </label>

          <Button className="w-full" size="lg">
            Create account <ArrowRight size={16} />
          </Button>

          <p className="text-center text-sm text-muted">
            Already have an account?{" "}
            <Link
              href="/login"
              className="font-medium text-trippy-400 hover:text-trippy-300 transition-colors"
            >
              Sign in
            </Link>
          </p>
        </GlassCard>
      </motion.div>
    </div>
  );
}
