# Trippy — Branding & UI Guidelines

> This document is the single source of truth for Trippy's visual identity and component system. Every team member building UI should follow these guidelines to keep the product consistent.

---

## 1. Brand Identity

| Property | Value |
|---|---|
| **Product name** | Trippy |
| **Tagline** | *Plan your next adventure* |
| **Tone** | Friendly, adventurous, clean, modern |
| **Visual theme** | Coastal — sea glass, lagoon blues, tropical coral, warm shore |

### Logo

The logo is a **Compass icon** inside a rounded `trippy-500` tile, paired with the bold wordmark "Trippy".

```tsx
import Logo from "@/components/Logo";

<Logo size="sm" />   // 18px icon, text-lg
<Logo size="md" />   // 22px icon, text-xl  ← default
<Logo size="lg" />   // 28px icon, text-2xl
```

- Icon: `Compass` from `lucide-react`
- Icon background: `bg-trippy-500` with `rounded-lg`
- Wordmark: `font-bold tracking-tight text-foreground`
- **Never** change the icon or replace it with text alone.

---

## 2. Color Palette

All colors are defined as CSS custom properties in `frontend/app/globals.css` and exposed as Tailwind tokens.

### Primary — Sea Glass / Lagoon

| Token | Light | Dark | Usage |
|---|---|---|---|
| `trippy-50` | `#effbf9` | `#0d2023` | Subtle tinted backgrounds |
| `trippy-100` | `#d8f4ef` | `#133036` | Hover states, chips |
| `trippy-200` | `#afe7dd` | `#18474d` | Borders, dividers |
| `trippy-300` | `#80d7ca` | `#1d626a` | Inactive elements |
| `trippy-400` | `#4ec1b5` | `#239da0` | Focus rings, secondary actions |
| **`trippy-500`** | **`#239da0`** | **`#3cc2c4`** | **Primary brand color — CTAs, logo** |
| `trippy-600` | `#1d7f87` | `#69d9d7` | Hover on primary |
| `trippy-700` | `#1c666e` | `#95e8e2` | Active/pressed states |
| `trippy-800` | `#1b5157` | `#c6f4f0` | Dark surfaces |
| `trippy-900` | `#183f44` | `#ebfcf9` | Deep dark text |

Tailwind usage: `text-trippy-500`, `bg-trippy-100`, `border-trippy-300/35`, etc.

### Accent — Sun Coral / Tropical Bloom

| Token | Light | Dark | Usage |
|---|---|---|---|
| `accent-400` | `#ff8f63` | `#d85f3c` | Highlights, tags |
| **`accent-500`** | **`#ff6f4d`** | **`#ff7b57`** | **Accent CTAs, notifications** |
| `accent-600` | `#ea5530` | `#ff9a78` | Hover on accent |

### Supporting Tones

| Token | Light Value | Usage |
|---|---|---|
| `shore-50/100/200` | Warm creamy tones | Card backgrounds, page wash |
| `lagoon-100/200` | Sky blue | Info highlights |
| `leaf-100/200` | Soft greens | Success-adjacent accents |

### Semantic Colors

| Token | Light | Dark | Usage |
|---|---|---|---|
| `success` | `#2f9d62` | `#45c777` | Success states, confirmations |
| `warning` | `#d89b27` | `#efb94f` | Warnings, pending states |
| `danger` | `#ef4444` | `#f87171` | Errors, destructive actions |

### Environment / Layout

| Token | Description |
|---|---|
| `background` | Page background |
| `foreground` | Primary text color |
| `surface` | Glass card surface (`rgba` with opacity) |
| `surface-hover` | Hovered glass surface |
| `border` | Subtle tinted border |
| `text-muted` | Secondary / helper text |

---

## 3. Typography

### Fonts

| Role | Font | CSS Variable |
|---|---|---|
| **Body / UI** | Geist Sans | `--font-geist-sans` |
| **Code / Mono** | Geist Mono | `--font-geist-mono` |

Both fonts are loaded via `next/font/google` in `frontend/app/layout.tsx`. Always use Tailwind's `font-sans` (maps to Geist Sans) — do not introduce new fonts without team approval.

### Type Scale

| Class | Size | Weight | Use |
|---|---|---|---|
| `h1` | `2.25rem` / `3rem` (md+) | 700 | Page titles |
| `h2` | `1.875rem` / `2.25rem` (md+) | 700 | Section headers |
| `h3` | `1.5rem` / `1.875rem` (md+) | 600 | Card titles |
| `h4` | `1.25rem` | 600 | Sub-sections |
| `h5` | `1.125rem` | 500 | Labels, group headers |
| `h6` | `1rem` | 500 | Small headings |
| `.text-body` | `1rem` | — | Default paragraph text |
| `.text-body-sm` | `0.875rem` | — | Helper text, descriptions |
| `.text-caption` | `0.75rem` | — | Timestamps, footnotes (muted) |

> Always use `tracking-tight` on bold headings. Page-level titles use `font-bold tracking-tight`.

---

## 4. Glassmorphism System

Trippy's surfaces use a glassmorphism aesthetic. Three utility classes are defined in `globals.css`:

| Class | Blur | Use |
|---|---|---|
| `.glass` | `16px` | Default cards, panels, modals |
| `.glass-sm` | `8px` | Input fields, small chips |
| `.glass-strong` | `24px` | Hero cards, featured content |

All glass utilities include:
- `background: var(--surface)` with opacity
- `backdrop-filter: blur(…) saturate(…)`
- `border: 1px solid var(--border)`
- `border-radius: 1rem`
- Layered `box-shadow` with soft ambient + warm accent

### GlassCard Component

```tsx
import { GlassCard } from "@/components/ui";

<GlassCard>…</GlassCard>                    // default (.glass)
<GlassCard variant="sm">…</GlassCard>       // .glass-sm
<GlassCard variant="strong">…</GlassCard>   // .glass-strong
```

- Wraps `framer-motion` `<motion.div>` with a fade-up entrance animation
- Default padding: `p-6`
- Use `className` to override padding or add borders

---

## 5. Buttons

Source: `frontend/components/ui/Button.tsx`

```tsx
import { Button } from "@/components/ui";

<Button>Primary</Button>
<Button variant="secondary">Secondary</Button>
<Button variant="ghost">Ghost</Button>
<Button variant="danger">Delete</Button>

<Button size="sm">Small</Button>
<Button size="md">Medium</Button>   {/* default */}
<Button size="lg">Large</Button>
```

### Variants

| Variant | Style | When to use |
|---|---|---|
| `primary` | `bg-trippy-gradient`, white text, shadow | Main CTA, form submits |
| `secondary` | `glass`, foreground text, border | Secondary actions, toggles |
| `ghost` | Transparent, foreground text | Tertiary actions, icon-only in toolbars |
| `danger` | `bg-danger`, white text | Destructive: delete, remove, sign out |

### Sizes

| Size | Padding | Radius | Font |
|---|---|---|---|
| `sm` | `px-3 py-1.5` | `rounded-lg` | `text-sm` |
| `md` | `px-5 py-2.5` | `rounded-xl` | `text-sm` |
| `lg` | `px-7 py-3.5` | `rounded-xl` | `text-base` |

### Rules
- Always use `gap-2` when pairing an icon with text (already baked in).
- Disabled state: `opacity-50 cursor-not-allowed` (automatic).
- **Never** create a raw `<button>` — always use `<Button>`.
- For sign-out and other destructive flows: prefer `variant="secondary"` with a red border override (`className="border-red-500/40 text-red-500 hover:bg-red-500/10"`) for a softer feel over the full `danger` variant.

---

## 6. Inputs

Source: `frontend/components/ui/Input.tsx`

```tsx
import { Input } from "@/components/ui";

<Input label="Email" id="email" type="email" placeholder="you@example.com" />
<Input label="Password" id="password" type="password" error="Must be 8+ characters" />
```

- Base style: `.glass-sm` surface
- Focus ring: `ring-2 ring-trippy-400/40`, border `trippy-300/50`
- Error state: `ring-2 ring-danger/50`, error text in `text-danger text-xs`
- Label: `text-sm font-medium text-foreground`
- Placeholder: `placeholder:text-muted`

---

## 7. Badges

Source: `frontend/components/ui/Badge.tsx`

```tsx
import { Badge } from "@/components/ui";

<Badge>Default</Badge>
<Badge variant="success">Confirmed</Badge>
<Badge variant="warning">Pending</Badge>
<Badge variant="danger">Cancelled</Badge>
<Badge variant="accent">New</Badge>
```

| Variant | Colors |
|---|---|
| `default` | `trippy-500/14` bg, `trippy-700` text, `trippy-300/35` border |
| `success` | `success/15` bg, `success` text |
| `warning` | `warning/15` bg, `warning` text |
| `danger` | `danger/15` bg, `danger` text |
| `accent` | `accent-400/15` bg, `accent-600` text, `accent-300/35` border |

All badges: `rounded-full`, `text-xs font-medium`, `backdrop-blur-sm`.

---

## 8. Avatar

Source: `frontend/components/ui/Avatar.tsx`

```tsx
import { Avatar } from "@/components/ui";

<Avatar name="Jane Doe" />
<Avatar name="Jane Doe" src="/path/to/photo.jpg" size="lg" />
```

| Size | Dimensions |
|---|---|
| `sm` | 32×32px |
| `md` | 40×40px (default) |
| `lg` | 56×56px |

- Falls back to initials derived from `name` when no `src` is provided.
- Initials background uses `trippy-gradient`.

---

## 9. Gradients & Backgrounds

### Defined utilities (globals.css)

| Class | Description |
|---|---|
| `.bg-trippy-gradient` | `135deg` linear — `trippy-500 → trippy-400 → #57c7dd` |
| `.bg-trippy-mesh` | Organic radial mesh of shore, lagoon, leaf, accent tones |

### Page background
The `<body>` uses a multi-layer radial + linear gradient (fixed attachment) with warm shore tones at corners and a clean center. Do not override the body background per-page — design pages to sit within it.

---

## 10. Spacing & Layout

- **Base unit**: `4px` (Tailwind default)
- **Card padding**: `p-6` (24px)
- **Section gap**: `space-y-8` between major page sections
- **Content max-width**: Follow the dashboard layout's container constraints
- **Border radius standards**:
  - Small elements (badges, tags): `rounded-full`
  - Inputs, small buttons: `rounded-lg` (8px)
  - Cards, modals, large buttons: `rounded-xl` (12px)
  - Extra-large surfaces: `rounded-2xl` (16px)

---

## 11. Motion & Animation

Animations use **Framer Motion**. Follow these standards:

| Pattern | Values |
|---|---|
| Fade-in up (page sections) | `initial: {opacity:0, y:10}` → `animate: {opacity:1, y:0}` |
| Card entrance | `initial: {opacity:0, y:20}` → `whileInView: {opacity:1, y:0}` |
| Duration | `0.4s – 0.5s`, `ease: "easeOut"` |
| Viewport | `viewport={{ once: true }}` — animate only once |

- Prefer subtle, quick transitions (`200ms`) on hover/focus states via Tailwind `transition-all duration-200`.
- Do not use heavy keyframe animations or large motion distances.

---

## 12. Icons

Icon library: **`lucide-react`**

- Default size in body text: `16px`
- Paired with button text: `14px`
- Standalone / hero icons: `20–24px`
- Always match icon color to its surrounding text color (inherit, not hardcoded).

---

## 13. Dark Mode

Dark mode is toggled via `data-theme="dark"` on `<html>`. All CSS variables automatically swap. Rules:

- **Never hardcode hex colors** in components — always use CSS variable tokens or Tailwind token classes.
- Test every new component in both `light` and `dark` themes.
- The `ThemeToggle` component (`frontend/components/ui/ThemeToggle.tsx`) handles toggling and persists preference to `localStorage`.

---

## 14. Do's and Don'ts

### ✅ Do
- Use `<Button>`, `<GlassCard>`, `<Input>`, `<Badge>`, `<Avatar>` from `@/components/ui` for all UI.
- Use CSS variable tokens (e.g. `text-trippy-500`, `bg-surface`) via Tailwind.
- Keep `gap-2` between icon + text pairs.
- Wrap page sections in `<motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>`.
- Use `text-muted` for helper/secondary text.

### ❌ Don't
- Don't hardcode colors like `#239da0` — use `trippy-500`.
- Don't create new button or input styles from scratch — extend the existing component via `className`.
- Don't introduce new fonts.
- Don't use `rounded-md` for cards — use `rounded-xl` or `.glass`.
- Don't skip dark mode testing.
