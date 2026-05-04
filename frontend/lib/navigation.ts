export function isSafeInternalPath(value?: string): value is string {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return false;
  }

  const pathOnly = value.split(/[?#]/, 1)[0];
  try {
    return !decodeURIComponent(pathOnly).split("/").includes("..");
  } catch {
    return false;
  }
}
