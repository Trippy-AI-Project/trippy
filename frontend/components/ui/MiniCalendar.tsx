"use client";

import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

export default function MiniCalendar({ title, date, setDate }: { title: string, date: Date | null, setDate: (d: Date | null) => void }) {
  const [currentMonth, setCurrentMonth] = useState(date ? new Date(date.getFullYear(), date.getMonth(), 1) : new Date(new Date().getFullYear(), new Date().getMonth(), 1));

  const daysInMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 0).getDate();
  const firstDayOfMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1).getDay();

  const handlePrevMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1));
  };

  const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

  return (
    <div className="glass-sm rounded-xl p-3 w-full">
        <div className="flex justify-between items-center mb-3 px-1">
           <h4 className="text-xs font-semibold text-foreground">{title}</h4>
           <div className="flex items-center gap-1">
                <button type="button" onClick={handlePrevMonth} className="p-1 hover:bg-surface rounded-md cursor-pointer text-muted hover:text-foreground">
                    <ChevronLeft size={12} />
                </button>
                <span className="text-[10px] text-trippy-600 font-medium tracking-wide w-[68px] text-center">
                    {monthNames[currentMonth.getMonth()]} {currentMonth.getFullYear()}
                </span>
                <button type="button" onClick={handleNextMonth} className="p-1 hover:bg-surface rounded-md cursor-pointer text-muted hover:text-foreground">
                    <ChevronRight size={12} />
                </button>
           </div>
        </div>
        <div className="grid grid-cols-7 gap-1 text-[10px] text-center text-muted mb-2 font-medium">
            <div>Su</div><div>Mo</div><div>Tu</div><div>We</div><div>Th</div><div>Fr</div><div>Sa</div>
        </div>
        <div className="grid grid-cols-7 gap-1">
            {Array.from({ length: firstDayOfMonth }).map((_, i) => (
                <div key={"empty-" + i} className="aspect-square"></div>
            ))}
            {Array.from({ length: daysInMonth }).map((_, i) => {
                const day = i + 1;
                const isSelected = date?.getDate() === day && date?.getMonth() === currentMonth.getMonth() && date?.getFullYear() === currentMonth.getFullYear();
                return (
                    <button
                        key={day}
                        type="button"
                        onClick={() => setDate(isSelected ? null : new Date(currentMonth.getFullYear(), currentMonth.getMonth(), day))}
                        className={"aspect-square rounded-lg flex items-center justify-center text-xs transition-all cursor-pointer " + (isSelected ? "bg-trippy-500 text-white font-bold shadow-md scale-110" : "hover:bg-surface text-foreground")}
                    >
                        {day}
                    </button>
                )
            })}
        </div>
    </div>
  );
}
