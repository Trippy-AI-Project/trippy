"use client";

import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Calendar, X } from "lucide-react";

export default function DateRangePicker({
  startDate,
  endDate,
  onChange,
}: {
  startDate: string;
  endDate: string;
  onChange: (start: string, end: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [viewYear, setViewYear] = useState(() => {
    const d = startDate ? new Date(startDate) : new Date();
    return d.getFullYear();
  });
  const [viewMonth, setViewMonth] = useState(() => {
    const d = startDate ? new Date(startDate) : new Date();
    return d.getMonth();
  });
  const [hoverDate, setHoverDate] = useState("");
  const pickerRef = useRef<HTMLDivElement>(null);
  const today = new Date().toISOString().slice(0, 10);

  useEffect(() => {
    if (!open) return;
    function onMouseDown(e: MouseEvent) {
      if (pickerRef.current && !pickerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onMouseDown);
    return () => document.removeEventListener("mousedown", onMouseDown);
  }, [open]);

  function toDateStr(y: number, m: number, d: number) {
    return `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
  }

  function handleDayClick(dateStr: string) {
    if (dateStr < today) return;
    if (!startDate || (startDate && endDate)) {
      onChange(dateStr, "");
    } else if (dateStr < startDate) {
      onChange(dateStr, startDate);
      setOpen(false);
    } else if (dateStr === startDate) {
      onChange("", "");
    } else {
      onChange(startDate, dateStr);
      setOpen(false);
    }
  }

  function isInRange(d: string) {
    const s = startDate;
    const e = endDate || hoverDate;
    if (!s || !e) return false;
    const [lo, hi] = s <= e ? [s, e] : [e, s];
    return d > lo && d < hi;
  }

  function prevMonth() {
    if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1); }
    else setViewMonth(m => m - 1);
  }
  function nextMonth() {
    if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1); }
    else setViewMonth(m => m + 1);
  }

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const firstDay = new Date(viewYear, viewMonth, 1).getDay();
  const MONTHS = ["January","February","March","April","May","June","July","August","September","October","November","December"];
  const DAY_NAMES = ["Su","Mo","Tu","We","Th","Fr","Sa"];

  let displayLabel = "Select travel dates";
  if (startDate && endDate) {
    const s = new Date(startDate + "T00:00:00").toLocaleDateString(undefined, { month: "short", day: "numeric" });
    const e = new Date(endDate + "T00:00:00").toLocaleDateString(undefined, { month: "short", day: "numeric" });
    const nights = Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86400000);
    displayLabel = `${s} → ${e}  ·  ${nights} night${nights !== 1 ? "s" : ""}`;
  } else if (startDate) {
    const s = new Date(startDate + "T00:00:00").toLocaleDateString(undefined, { month: "short", day: "numeric" });
    displayLabel = `${s} · 1 day`;
  }

  return (
    <div className="relative" ref={pickerRef}>
      <button
        type="button"
        onClick={() => setOpen(p => !p)}
        className={`w-full flex items-center gap-2 rounded-lg border bg-transparent px-3 py-2 text-sm outline-none text-left transition-colors cursor-pointer ${
          open ? "border-trippy-500/60 bg-trippy-500/5" : "border-border hover:border-trippy-500/40"
        } ${!startDate && !endDate ? "text-muted/70" : "text-foreground"}`}
      >
        <Calendar size={14} className="text-trippy-500 shrink-0" />
        <span className="flex-1 truncate">{displayLabel}</span>
        {(startDate || endDate) && (
          <span
            role="button"
            tabIndex={0}
            onClick={(e) => { e.stopPropagation(); onChange("", ""); }}
            onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.stopPropagation(); onChange("", ""); } }}
            className="text-muted hover:text-foreground transition-colors shrink-0 cursor-pointer"
          >
            <X size={12} />
          </span>
        )}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 6, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 4, scale: 0.97 }}
            transition={{ duration: 0.15 }}
            className="absolute top-full left-0 mt-1.5 z-[200] bg-white border border-border rounded-2xl shadow-2xl p-4 w-[300px]"
          >
            {/* Month navigation */}
            <div className="flex items-center justify-between mb-3">
              <button type="button" onClick={prevMonth}
                className="w-8 h-8 rounded-xl flex items-center justify-center hover:bg-surface text-muted hover:text-foreground transition-colors cursor-pointer text-lg"
              >‹</button>
              <span className="text-sm font-semibold text-foreground">{MONTHS[viewMonth]} {viewYear}</span>
              <button type="button" onClick={nextMonth}
                className="w-8 h-8 rounded-xl flex items-center justify-center hover:bg-surface text-muted hover:text-foreground transition-colors cursor-pointer text-lg"
              >›</button>
            </div>

            {/* Day names */}
            <div className="grid grid-cols-7 mb-1">
              {DAY_NAMES.map(d => (
                <div key={d} className="text-center text-[10px] text-muted font-semibold py-1">{d}</div>
              ))}
            </div>

            {/* Day cells */}
            <div className="grid grid-cols-7">
              {Array.from({ length: firstDay }, (_, i) => <div key={`e${i}`} />)}
              {Array.from({ length: daysInMonth }, (_, i) => {
                const day = i + 1;
                const ds = toDateStr(viewYear, viewMonth, day);
                const isPast = ds < today;
                const isToday = ds === today;
                const isStart = ds === startDate;
                const isEnd = ds === endDate;
                const inRange = isInRange(ds);
                const isSelected = isStart || isEnd;

                return (
                  <div key={day} className={`relative h-8 flex items-center justify-center ${inRange ? "bg-trippy-500/10" : ""} ${isStart && endDate ? "rounded-l-full" : ""} ${isEnd ? "rounded-r-full" : ""} ${isStart && !endDate ? "rounded-full" : ""}`}>
                    <button
                      type="button"
                      disabled={isPast}
                      onClick={() => handleDayClick(ds)}
                      onMouseEnter={() => { if (startDate && !endDate) setHoverDate(ds); }}
                      onMouseLeave={() => setHoverDate("")}
                      className={`w-8 h-8 rounded-full text-xs transition-all relative
                        ${isPast ? "text-muted/30 cursor-not-allowed" : "cursor-pointer"}
                        ${isSelected ? "bg-trippy-500 text-white font-bold shadow-sm" : ""}
                        ${!isPast && !isSelected ? "hover:bg-trippy-500/15 text-foreground" : ""}
                        ${isToday && !isSelected ? "font-bold text-trippy-600" : ""}
                        ${inRange && !isSelected ? "text-trippy-700 font-medium" : ""}
                      `}
                    >
                      {day}
                      {isToday && !isSelected && (
                        <span className="absolute bottom-0.5 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-trippy-500" />
                      )}
                    </button>
                  </div>
                );
              })}
            </div>

            {/* Status */}
            <div className="mt-3 pt-2.5 border-t border-border/50 text-center">
              {!startDate ? (
                <p className="text-[11px] text-muted">Tap a date to start</p>
              ) : !endDate ? (
                <p className="text-[11px] text-trippy-600 font-medium">One-day trip selected</p>
              ) : (
                <p className="text-[11px] text-green-600 font-medium">
                  ✓ {Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86400000)} nights selected
                </p>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
