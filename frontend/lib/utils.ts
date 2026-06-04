import { clsx, type ClassValue } from "clsx";

export function cn(...inputs: ClassValue[]) {
  return clsx(inputs);
}

/**
 * Creates a URL-friendly slug from a trip title + tripId.
 * Format: "trip-title-slug--full-uuid"
 * The double-hyphen separates the readable portion from the ID.
 */
export function tripSlug(title: string, tripId: string): string {
  const slug = title
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .slice(0, 40)
    .replace(/-$/, "");
  return `${slug}--${tripId}`;
}

/**
 * Extracts the full trip UUID from a trip slug.
 * Splits on "--" and returns the UUID portion.
 * Falls back to the full slug if no "--" found (backwards compat).
 */
export function tripIdFromSlug(slug: string): string {
  const idx = slug.indexOf("--");
  if (idx === -1) return slug;
  return slug.slice(idx + 2);
}
