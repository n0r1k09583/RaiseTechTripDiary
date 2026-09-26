const ACCESS_KEY = "trip-jwt";
const REFRESH_KEY = "trip-refresh";

let accessMem: string | null = null;
let refreshMem: string | null = null;

function readStore(key: string): string | null {
  try {
    return sessionStorage.getItem(key) ?? localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStore(key: string, value: string | null) {
  try {
    if (value) {
      sessionStorage.setItem(key, value);
      localStorage.setItem(key, value);
    } else {
      sessionStorage.removeItem(key);
      localStorage.removeItem(key);
    }
  } catch {
    // Cursor のブラウザなど、storage が使えない環境でもメモリだけでログインする
  }
}

accessMem = readStore(ACCESS_KEY);
refreshMem = readStore(REFRESH_KEY);

export type User = {
  id: number;
  email: string;
  username: string;
  displayName: string;
};

type AuthPayload = {
  accessToken?: string;
  refreshToken?: string;
  token?: string;
  user: User;
};

export function getToken(): string | null {
  return accessMem ?? readStore(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  return refreshMem ?? readStore(REFRESH_KEY);
}

export function setSession(accessToken: string | null, refreshToken: string | null = null) {
  accessMem = accessToken;
  refreshMem = refreshToken;
  writeStore(ACCESS_KEY, accessToken);
  writeStore(REFRESH_KEY, refreshToken);
}

export function setToken(token: string | null) {
  setSession(token, token ? getRefreshToken() : null);
}

export function accessOf(res: AuthPayload) {
  return res.accessToken || res.token || "";
}

function isPublicAuthPath(path: string) {
  return (
    path.startsWith("/api/login") ||
    path.startsWith("/api/signup") ||
    path.startsWith("/api/refresh") ||
    path.startsWith("/api/logout")
  );
}

let refreshInFlight: Promise<boolean> | null = null;

async function refreshSession(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return false;
  }
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const res = await fetch("/api/refresh", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
        const data = (await res.json().catch(() => ({}))) as AuthPayload;
        if (!res.ok || !accessOf(data)) {
          return false;
        }
        setSession(accessOf(data), data.refreshToken ?? refreshToken);
        return true;
      } catch {
        return false;
      }
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

async function request<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  const headers = new Headers(init.headers);
  const isForm = init.body instanceof FormData;
  if (!isForm) headers.set("Content-Type", "application/json");
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let res: Response;
  try {
    res = await fetch(path, { ...init, headers });
  } catch {
    throw new Error("サーバーに接続できません。バックエンドを起動してください");
  }
  if (res.status === 401 && !retried && !isPublicAuthPath(path) && (await refreshSession())) {
    return request<T>(path, init, true);
  }
  if (res.status === 204) return undefined as T;
  const data = (await res.json().catch(() => ({}))) as T & { error?: string };
  if (!res.ok) {
    throw new Error(
      data.error ||
        (res.status >= 500
          ? "サーバーに接続できません。バックエンドを起動してください"
          : "リクエストに失敗しました"),
    );
  }
  return data;
}

export function signup(body: {
  username: string;
  displayName: string;
  email: string;
  password: string;
  confirm: string;
}) {
  return request<AuthPayload>("/api/signup", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function login(email: string, password: string) {
  return request<AuthPayload>("/api/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function me() {
  return request<{ user: User }>("/api/me");
}

export async function logout() {
  const refreshToken = getRefreshToken();
  try {
    await request("/api/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    });
  } catch {
    // 画面はトークンを消してログインへ戻す。API が古いときも同じ
  } finally {
    setSession(null, null);
  }
}

export type VisitStatus = "visited" | "want";
export type Visibility = "public" | "private";

export type Post = {
  id: number;
  userId: number;
  username: string;
  displayName: string;
  spotName: string;
  areaTag: string;
  visitStatus: VisitStatus;
  body: string;
  imageUrl: string | null;
  latitude: number | null;
  longitude: number | null;
  visibility: Visibility;
  createdAt: string;
  updatedAt: string;
  mine: boolean;
  commentCount: number;
  likeCount: number;
  likedByMe: boolean;
  favoritedByMe: boolean;
};

export type PostList = {
  posts: Post[];
  hasMore: boolean;
};

type ListQuery = {
  tab?: "all" | "following" | "visited" | "want" | "photos" | "favorites";
  area?: string;
  limit?: number;
  beforeCreatedAt?: string;
  beforeId?: number;
  afterCreatedAt?: string;
  afterId?: number;
};

export function listPosts(query: ListQuery = {}) {
  const params = new URLSearchParams();
  if (query.tab) params.set("tab", query.tab);
  if (query.area) params.set("area", query.area);
  if (query.limit != null) params.set("limit", String(query.limit));
  if (query.beforeCreatedAt) params.set("beforeCreatedAt", query.beforeCreatedAt);
  if (query.beforeId != null) params.set("beforeId", String(query.beforeId));
  if (query.afterCreatedAt) params.set("afterCreatedAt", query.afterCreatedAt);
  if (query.afterId != null) params.set("afterId", String(query.afterId));
  const qs = params.toString();
  return request<PostList>(`/api/posts${qs ? `?${qs}` : ""}`);
}

export function getPost(id: number) {
  return request<Post>(`/api/posts/${id}`);
}

export type PostInput = {
  spotName: string;
  areaTag: string;
  visitStatus: VisitStatus;
  body: string;
  image?: File | null;
  visibility?: Visibility;
  latitude?: number | null;
  longitude?: number | null;
};

export function createPost(input: PostInput) {
  const form = new FormData();
  form.append("spotName", input.spotName);
  form.append("areaTag", input.areaTag);
  form.append("visitStatus", input.visitStatus);
  form.append("body", input.body);
  form.append("visibility", input.visibility ?? "public");
  form.append("latitude", input.latitude == null ? "" : String(input.latitude));
  form.append("longitude", input.longitude == null ? "" : String(input.longitude));
  if (input.image) form.append("image", input.image);
  return request<Post>("/api/posts", { method: "POST", body: form });
}

export function updatePost(id: number, input: PostInput) {
  const form = new FormData();
  form.append("spotName", input.spotName);
  form.append("areaTag", input.areaTag);
  form.append("visitStatus", input.visitStatus);
  form.append("body", input.body);
  form.append("visibility", input.visibility ?? "public");
  form.append("latitude", input.latitude == null ? "" : String(input.latitude));
  form.append("longitude", input.longitude == null ? "" : String(input.longitude));
  if (input.image) form.append("image", input.image);
  return request<Post>(`/api/posts/${id}`, { method: "PATCH", body: form });
}

export function deletePost(id: number) {
  return request<void>(`/api/posts/${id}`, { method: "DELETE" });
}

export type Comment = {
  id: number;
  postId: number;
  userId: number;
  username: string;
  displayName: string;
  body: string;
  createdAt: string;
  mine: boolean;
};

export function listComments(postId: number) {
  return request<{ comments: Comment[] }>(`/api/posts/${postId}/comments`);
}

export function createComment(postId: number, body: string) {
  return request<Comment>(`/api/posts/${postId}/comments`, {
    method: "POST",
    body: JSON.stringify({ body }),
  });
}

export function deleteComment(id: number) {
  return request<void>(`/api/comments/${id}`, { method: "DELETE" });
}

export function toggleLike(postId: number) {
  return request<Post>(`/api/posts/${postId}/likes`, { method: "POST" });
}

export function toggleFavorite(postId: number) {
  return request<Post>(`/api/posts/${postId}/favorites`, { method: "POST" });
}

export type Profile = {
  id: number;
  username: string;
  displayName: string;
  followingCount: number;
  followerCount: number;
  followedByMe: boolean;
  mine: boolean;
};

export type UserSummary = {
  id: number;
  username: string;
  displayName: string;
  followedByMe: boolean;
  mine: boolean;
};

export async function searchUsers(q: string) {
  const params = new URLSearchParams();
  params.set("q", q);
  const res = await request<{ users?: UserSummary[] }>(`/api/users?${params.toString()}`);
  return { users: Array.isArray(res.users) ? res.users : [] };
}

export function getProfile(username: string) {
  return request<Profile>(`/api/users/${encodeURIComponent(username)}`);
}

export function listUserPosts(username: string, query: ListQuery = {}) {
  const params = new URLSearchParams();
  if (query.limit != null) params.set("limit", String(query.limit));
  if (query.beforeCreatedAt) params.set("beforeCreatedAt", query.beforeCreatedAt);
  if (query.beforeId != null) params.set("beforeId", String(query.beforeId));
  if (query.afterCreatedAt) params.set("afterCreatedAt", query.afterCreatedAt);
  if (query.afterId != null) params.set("afterId", String(query.afterId));
  const qs = params.toString();
  return request<PostList>(`/api/users/${encodeURIComponent(username)}/posts${qs ? `?${qs}` : ""}`);
}

export function listFollowees(username: string) {
  return request<{ users: UserSummary[] }>(`/api/users/${encodeURIComponent(username)}/followees`);
}

export function listFollowers(username: string) {
  return request<{ users: UserSummary[] }>(`/api/users/${encodeURIComponent(username)}/followers`);
}

export function followUser(username: string) {
  return request<Profile>(`/api/users/${encodeURIComponent(username)}/follow`, { method: "POST" });
}

export function unfollowUser(username: string) {
  return request<Profile>(`/api/users/${encodeURIComponent(username)}/follow`, { method: "DELETE" });
}
