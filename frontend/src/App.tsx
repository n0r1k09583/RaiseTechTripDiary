import { useCallback, useEffect, useState } from "react";
import { accessOf, getToken, logout, me, setSession, type User } from "./api";
import { EditPage } from "./EditPage";
import { FollowListPage } from "./FollowListPage";
import { LoginPage } from "./LoginPage";
import { PostDetailPage } from "./PostDetailPage";
import { ProfilePage } from "./ProfilePage";
import { SearchPage } from "./SearchPage";
import { SignupPage } from "./SignupPage";
import { TimelinePage } from "./TimelinePage";
import { applyPrivateSeo, applyPublicSeo, publicScreenFromPath } from "./seo";
import { normalizeUserQuery } from "./searchQuery";

type FollowKind = "followees" | "followers";

type Screen =
  | { name: "login" }
  | { name: "signup" }
  | { name: "timeline" }
  | { name: "edit"; postId: number }
  | { name: "post"; postId: number }
  | { name: "profile"; username: string }
  | { name: "follows"; username: string; kind: FollowKind }
  | { name: "search"; q: string };

function parsePrivatePath(pathname: string, search: string): Screen {
  const post = pathname.match(/^\/posts\/(\d+)$/);
  if (post) {
    return { name: "post", postId: Number(post[1]) };
  }
  const follows = pathname.match(/^\/users\/([^/]+)\/(followees|followers)$/);
  if (follows) {
    return { name: "follows", username: decodeURIComponent(follows[1]), kind: follows[2] as FollowKind };
  }
  const profile = pathname.match(/^\/users\/([^/]+)$/);
  if (profile) {
    return { name: "profile", username: decodeURIComponent(profile[1]) };
  }
  if (pathname === "/search") {
    return { name: "search", q: normalizeUserQuery(new URLSearchParams(search).get("q") ?? "") };
  }
  return { name: "timeline" };
}

function go(path: string) {
  if (`${window.location.pathname}${window.location.search}` !== path) {
    window.history.pushState(null, "", path);
  }
}

export function App() {
  const [screen, setScreen] = useState<Screen>(() => {
    const publicScreen = publicScreenFromPath(window.location.pathname);
    return publicScreen === "signup" ? { name: "signup" } : { name: "login" };
  });
  const [user, setUser] = useState<User | null>(null);
  const [boot, setBoot] = useState(true);

  const applyPath = useCallback((pathname: string, search: string) => {
    setScreen(parsePrivatePath(pathname, search));
  }, []);

  const onLogout = useCallback(async () => {
    await logout();
    setUser(null);
    window.history.replaceState(null, "", "/login");
    setScreen({ name: "login" });
  }, []);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      const next = publicScreenFromPath(window.location.pathname);
      setScreen(next === "signup" ? { name: "signup" } : { name: "login" });
      if (window.location.pathname === "/" || window.location.pathname === "") {
        window.history.replaceState(null, "", "/login");
      }
      setBoot(false);
      return;
    }
    me()
      .then((res) => {
        setUser(res.user);
        const path = window.location.pathname;
        if (path === "/login" || path === "/signup" || path === "" || path === "/") {
          window.history.replaceState(null, "", "/");
          setScreen({ name: "timeline" });
          return;
        }
        applyPath(path, window.location.search);
      })
      .catch(() => setSession(null, null))
      .finally(() => setBoot(false));
  }, [applyPath]);

  useEffect(() => {
    function onPop() {
      if (!getToken()) {
        const next = publicScreenFromPath(window.location.pathname);
        setScreen(next === "signup" ? { name: "signup" } : { name: "login" });
        return;
      }
      applyPath(window.location.pathname, window.location.search);
    }
    window.addEventListener("popstate", onPop);
    return () => window.removeEventListener("popstate", onPop);
  }, [applyPath]);

  useEffect(() => {
    if (boot) {
      return;
    }
    if (!user) {
      applyPublicSeo(screen.name === "signup" ? "signup" : "login");
      return;
    }
    applyPrivateSeo();
  }, [boot, user, screen]);

  function onAuthed(res: { accessToken?: string; refreshToken?: string; token?: string; user: User }) {
    setSession(accessOf(res), res.refreshToken ?? null);
    setUser(res.user);
    go("/");
    setScreen({ name: "timeline" });
  }

  const goTimeline = useCallback(() => {
    go("/");
    setScreen({ name: "timeline" });
  }, []);

  const goProfile = useCallback((username: string) => {
    go(`/users/${encodeURIComponent(username)}`);
    setScreen({ name: "profile", username });
  }, []);

  const goSearch = useCallback((q: string) => {
    const needle = normalizeUserQuery(q);
    const path = needle ? `/search?q=${encodeURIComponent(needle)}` : "/search";
    go(path);
    setScreen({ name: "search", q: needle });
  }, []);

  const goPost = useCallback((id: number) => {
    go(`/posts/${id}`);
    setScreen({ name: "post", postId: id });
  }, []);

  const goEdit = useCallback((id: number) => {
    setScreen({ name: "edit", postId: id });
  }, []);

  const goFollows = useCallback((username: string, kind: FollowKind) => {
    go(`/users/${encodeURIComponent(username)}/${kind}`);
    setScreen({ name: "follows", username, kind });
  }, []);

  if (boot) return <p className="wrap">読み込み中…</p>;

  if (!user) {
    return screen.name === "signup" ? (
      <SignupPage
        onSuccess={onAuthed}
        onGoLogin={() => {
          go("/login");
          setScreen({ name: "login" });
        }}
      />
    ) : (
      <LoginPage
        onSuccess={onAuthed}
        onGoSignup={() => {
          go("/signup");
          setScreen({ name: "signup" });
        }}
      />
    );
  }

  if (screen.name === "edit") {
    return (
      <EditPage
        user={user}
        postId={screen.postId}
        onLogout={onLogout}
        onDone={goTimeline}
        onHome={goTimeline}
        onProfile={() => goProfile(user.username)}
        onSearch={goSearch}
      />
    );
  }

  if (screen.name === "post") {
    return (
      <PostDetailPage
        user={user}
        postId={screen.postId}
        onLogout={onLogout}
        onBack={goTimeline}
        onEdit={goEdit}
        onProfile={goProfile}
        onSearch={goSearch}
      />
    );
  }

  if (screen.name === "profile") {
    return (
      <ProfilePage
        user={user}
        username={screen.username}
        onLogout={onLogout}
        onHome={goTimeline}
        onProfile={goProfile}
        onSearch={goSearch}
        onOpen={goPost}
        onEdit={goEdit}
        onFollows={goFollows}
      />
    );
  }

  if (screen.name === "follows") {
    return (
      <FollowListPage
        user={user}
        username={screen.username}
        kind={screen.kind}
        onLogout={onLogout}
        onHome={goTimeline}
        onProfile={goProfile}
        onSearch={goSearch}
        onFollows={goFollows}
      />
    );
  }

  if (screen.name === "search") {
    return (
      <SearchPage
        user={user}
        q={screen.q}
        onLogout={onLogout}
        onHome={goTimeline}
        onProfile={goProfile}
        onSearch={goSearch}
      />
    );
  }

  return (
    <TimelinePage
      user={user}
      onLogout={onLogout}
      onEdit={goEdit}
      onOpen={goPost}
      onProfile={goProfile}
      onHome={goTimeline}
      onSearch={goSearch}
    />
  );
}
