const SITE_NAME = "TripDiary";
const SITE_SUB = "旅の記録";
const PUBLIC_DESCRIPTION =
  "TripDiary の旅の記録。行った場所と行きたい場所を写真と感想で残すアプリ。投稿はログイン後だけ使えます。";

function setMeta(name: string, content: string, attr: "name" | "property" = "name") {
  let el = document.head.querySelector(`meta[${attr}="${name}"]`);
  if (!el) {
    el = document.createElement("meta");
    el.setAttribute(attr, name);
    document.head.appendChild(el);
  }
  el.setAttribute("content", content);
}

function setCanonical(path: string) {
  let link = document.head.querySelector('link[rel="canonical"]');
  if (!link) {
    link = document.createElement("link");
    link.setAttribute("rel", "canonical");
    document.head.appendChild(link);
  }
  link.setAttribute("href", new URL(path, window.location.origin).toString());
}

export function applyPublicSeo(kind: "login" | "signup") {
  const path = kind === "signup" ? "/signup" : "/login";
  const title =
    kind === "signup" ? `新規登録 — ${SITE_NAME}` : `ログイン — ${SITE_NAME}`;
  document.title = title;
  setMeta("robots", "index, follow");
  setMeta("description", PUBLIC_DESCRIPTION);
  setMeta("og:title", `${title} ${SITE_SUB}`, "property");
  setMeta("og:description", PUBLIC_DESCRIPTION, "property");
  setMeta("og:type", "website", "property");
  setMeta("og:locale", "ja_JP", "property");
  setCanonical(path);
}

export function applyPrivateSeo() {
  document.title = `${SITE_SUB} — ${SITE_NAME}`;
  setMeta("robots", "noindex, nofollow");
  setCanonical("/");
}

export function publicScreenFromPath(pathname: string): "login" | "signup" {
  return pathname === "/signup" || pathname.startsWith("/signup/") ? "signup" : "login";
}
