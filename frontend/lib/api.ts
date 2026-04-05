const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const TOKEN_KEY = "trippy_access_token";
const REFRESH_KEY = "trippy_refresh_token";

/* ------------------------------------------------------------------ */
/*  Token helpers                                                      */
/* ------------------------------------------------------------------ */

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(REFRESH_KEY);
}

export function setTokens(access: string, refresh: string) {
  localStorage.setItem(TOKEN_KEY, access);
  localStorage.setItem(REFRESH_KEY, refresh);
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

/* ------------------------------------------------------------------ */
/*  Generic fetch wrapper                                              */
/* ------------------------------------------------------------------ */

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: Record<string, unknown>,
  ) {
    super(typeof body?.message === "string" ? body.message : `API error ${status}`);
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };

  const token = getAccessToken();
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(res.status, body);
  }

  // 204 No Content — nothing to parse
  if (res.status === 204) return undefined as T;
  return res.json();
}

/* ------------------------------------------------------------------ */
/*  Auth API                                                           */
/* ------------------------------------------------------------------ */

export interface UserProfile {
  userId: string;
  email: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  avatarUrl?: string;
  bio?: string;
  country?: string;
  phoneNumber?: string;
  emailVerified: boolean;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  user: UserProfile;
}

export interface RegisterResponse {
  userId: string;
  email: string;
  message: string;
  verificationRequired: boolean;
}

export async function login(
  email: string,
  password: string,
  rememberMe = false,
): Promise<LoginResponse> {
  const data = await request<LoginResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password, rememberMe }),
  });
  setTokens(data.accessToken, data.refreshToken);
  return data;
}

export async function register(
  email: string,
  password: string,
  displayName: string,
): Promise<RegisterResponse> {
  return request<RegisterResponse>("/auth/register", {
    method: "POST",
    body: JSON.stringify({ email, password, displayName }),
  });
}

export async function logout(): Promise<void> {
  const refreshToken = getRefreshToken();
  if (refreshToken) {
    await request("/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }).catch(() => {
      /* best-effort — clear tokens regardless */
    });
  }
  clearTokens();
}

export async function refreshAccessToken(): Promise<LoginResponse> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error("No refresh token");

  const data = await request<LoginResponse>("/auth/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken }),
  });
  setTokens(data.accessToken, data.refreshToken);
  return data;
}

/* ------------------------------------------------------------------ */
/*  Generic helpers (re-export for convenience)                        */
/* ------------------------------------------------------------------ */

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "POST", body: JSON.stringify(body) }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "PUT", body: JSON.stringify(body) }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "PATCH", body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};

/* ------------------------------------------------------------------ */
/*  Trip API                                                           */
/* ------------------------------------------------------------------ */

export interface Trip {
  tripId: string;
  title: string;
  description?: string;
  destination: string;
  coverImageUrl?: string;
  startDate?: string;
  endDate?: string;
  organizerId: string;
  status: "DRAFT" | "PLANNED" | "ONGOING" | "COMPLETED" | "CANCELLED";
  visibility: "PRIVATE" | "PUBLIC" | "UNLISTED";
  participantCount: number;
  hasItinerary: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TripDetail extends Trip {
  participants: Participant[];
  itinerary?: Itinerary;
}

export interface Participant {
  participantId: string;
  tripId: string;
  userId: string;
  displayName?: string;
  avatarUrl?: string;
  role: "OWNER" | "EDITOR" | "VIEWER";
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "LEFT";
  invitedAt: string;
  joinedAt?: string;
}

export interface Itinerary {
  itineraryId: string;
  tripId: string;
  days: DayPlan[];
  generatedAt?: string;
}

export interface DayPlan {
  dayPlanId: string;
  dayNumber: number;
  date?: string;
  title?: string;
  activities: Activity[];
}

export interface Activity {
  activityId: string;
  time?: string;
  title: string;
  description?: string;
  location?: string;
  category?: string;
  estimatedCost?: string;
}

export interface TripPage {
  content: Trip[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CreateTripRequest {
  title: string;
  destination: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  visibility?: string;
}

export const tripsApi = {
  list: (page = 0, size = 12) =>
    api.get<TripPage>(`/trips?page=${page}&size=${size}`),
  search: (q: string, page = 0, size = 12) =>
    api.get<TripPage>(`/trips?search=${encodeURIComponent(q)}&page=${page}&size=${size}`),
  get: (id: string) => api.get<TripDetail>(`/trips/${id}`),
  create: (data: CreateTripRequest) => api.post<Trip>("/trips", data),
  update: (id: string, data: Partial<CreateTripRequest>) =>
    api.patch<Trip>(`/trips/${id}`, data),
  delete: (id: string) => api.delete<void>(`/trips/${id}`),
};

/* ------------------------------------------------------------------ */
/*  Notification API                                                   */
/* ------------------------------------------------------------------ */

export interface Notification {
  id: string;
  userId: string;
  type: string;
  title: string;
  message: string;
  actionUrl?: string;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  content: Notification[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const notificationsApi = {
  list: (page = 0, size = 10) =>
    api.get<NotificationPage>(`/notifications?page=${page}&size=${size}`),
  unreadCount: () => api.get<{ count: number }>("/notifications/unread/count"),
  markRead: (id: string) => api.put<void>(`/notifications/${id}/read`),
  markAllRead: () => api.put<void>("/notifications/read-all"),
};

/* ------------------------------------------------------------------ */
/*  Payment API                                                        */
/* ------------------------------------------------------------------ */

export interface SubscriptionInfo {
  subscriptionId: string;
  plan: "FREE" | "PREMIUM" | "ENTERPRISE";
  status: "ACTIVE" | "CANCELLED" | "PAST_DUE" | "TRIALING";
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  cancelAtPeriodEnd: boolean;
  priceAmount?: number;
  currency?: string;
}

export interface PaymentMethod {
  paymentMethodId: string;
  brand: string;
  last4: string;
  expiryMonth: number;
  expiryYear: number;
  isDefault: boolean;
  createdAt: string;
}

export interface TransactionRecord {
  transactionId: string;
  userId: string;
  amount: number;
  currency: string;
  type: string;
  status: string;
  description: string;
  createdAt: string;
}

export const paymentsApi = {
  getSubscription: () => api.get<SubscriptionInfo>("/payments/subscription"),
  checkout: (planId: string, paymentMethodId: string) =>
    api.post<{ transactionId: string; status: string }>("/payments/checkout", {
      planId,
      paymentMethodId,
    }),
  cancelSubscription: (cancelImmediately = false) =>
    api.post<SubscriptionInfo>("/payments/subscription/cancel", { cancelImmediately }),
  getMethods: () => api.get<PaymentMethod[]>("/payments/methods"),
  addMethod: (data: { brand: string; last4: string; expiryMonth: number; expiryYear: number; setAsDefault?: boolean }) =>
    api.post<PaymentMethod>("/payments/methods", data),
  deleteMethod: (id: string) => api.delete<void>(`/payments/methods/${id}`),
};

/* ------------------------------------------------------------------ */
/*  Chat API                                                           */
/* ------------------------------------------------------------------ */

export interface ChatMessage {
  messageId: string;
  tripId: string;
  senderId: string;
  senderName?: string;
  senderAvatarUrl?: string;
  type: "TEXT" | "IMAGE" | "FILE" | "SYSTEM";
  content: string;
  attachment?: {
    attachmentId: string;
    fileName: string;
    fileUrl: string;
    fileSize: number;
    contentType: string;
  };
  sentAt: string;
  isEdited: boolean;
  isDeleted: boolean;
}

export interface ChatMessagePage {
  content: ChatMessage[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const chatApi = {
  getMessages: (tripId: string, page = 0, size = 50) =>
    api.get<ChatMessagePage>(`/chats/${tripId}/messages?page=${page}&size=${size}`),
  sendMessage: (tripId: string, content: string) =>
    api.post<ChatMessage>(`/chats/${tripId}/messages`, { content }),
  uploadFile: async (tripId: string, file: File): Promise<ChatMessage> => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("senderId", "");
    const token = getAccessToken();
    const headers: Record<string, string> = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;
    const res = await fetch(`${API_BASE_URL}/chats/${tripId}/messages/file`, {
      method: "POST",
      headers,
      body: formData,
    });
    if (!res.ok) throw new ApiError(res.status, await res.json().catch(() => ({})));
    return res.json();
  },
  getParticipants: (tripId: string) =>
    api.get<string[]>(`/chats/${tripId}/participants`),
};
