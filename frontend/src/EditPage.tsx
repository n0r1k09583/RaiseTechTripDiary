import { useEffect, useState, type FormEvent } from "react";
import { getPost, updatePost, type User, type Visibility, type VisitStatus } from "./api";
import { AREAS } from "./areas";
import { AppHeader } from "./AppHeader";
import { MapPicker } from "./MapPicker";
import { PhotoField } from "./PhotoField";

const IMAGE_MAX = 5 * 1024 * 1024;
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

type Props = {
  user: User;
  postId: number;
  onLogout: () => void | Promise<void>;
  onDone: () => void;
  onHome?: () => void;
  onProfile?: () => void;
  onSearch?: (q: string) => void;
};

export function EditPage({ user, postId, onLogout, onDone, onHome, onProfile, onSearch }: Props) {
  const [spotName, setSpotName] = useState("");
  const [areaTag, setAreaTag] = useState("関東");
  const [visitStatus, setVisitStatus] = useState<VisitStatus>("visited");
  const [visibility, setVisibility] = useState<Visibility>("public");
  const [latitude, setLatitude] = useState<number | null>(null);
  const [longitude, setLongitude] = useState<number | null>(null);
  const [body, setBody] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [existingUrl, setExistingUrl] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    getPost(postId)
      .then((post) => {
        if (cancelled) return;
        if (!post.mine) {
          onDone();
          return;
        }
        setSpotName(post.spotName ?? "");
        setAreaTag(post.areaTag || "関東");
        setVisitStatus(post.visitStatus === "want" ? "want" : "visited");
        setVisibility(post.visibility === "private" ? "private" : "public");
        setLatitude(post.latitude);
        setLongitude(post.longitude);
        setBody(post.body);
        setExistingUrl(post.imageUrl);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "投稿を開けません");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [postId, onDone]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const name = spotName.trim();
    const text = body.trim();
    setError("");
    if (!name || name.length > 40) {
      setError("場所名は1〜40文字です");
      return;
    }
    if (text.length > 280) {
      setError("本文は1〜280文字です");
      return;
    }
    if (!text && !image && !existingUrl) {
      setError("感想か写真のどちらかが必要です");
      return;
    }
    if (image && !IMAGE_TYPES.includes(image.type)) {
      setError("JPEG / PNG / WebP のみです");
      return;
    }
    if (image && image.size > IMAGE_MAX) {
      setError("画像は5MBまでです");
      return;
    }
    setBusy(true);
    try {
      await updatePost(postId, {
        spotName: name,
        areaTag,
        visitStatus,
        body: text,
        image,
        visibility,
        latitude,
        longitude,
      });
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : "保存に失敗しました");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="page">
      <AppHeader
        user={user}
        onLogout={onLogout}
        onHome={onHome}
        onProfile={onProfile}
        onSearch={onSearch}
      />
      <section className="card">
        <h1>記録を編集</h1>
        <p className="lead">自分の記録だけ編集できます。新しい写真を選ぶと差し替わります。</p>
        {loading ? (
          <p className="empty">読み込み中…</p>
        ) : (
          <form onSubmit={onSubmit}>
            <label htmlFor="edit-spot">場所名</label>
            <input id="edit-spot" value={spotName} maxLength={40} onChange={(e) => setSpotName(e.target.value)} />
            <label htmlFor="edit-area">エリア</label>
            <select id="edit-area" value={areaTag} onChange={(e) => setAreaTag(e.target.value)}>
              {AREAS.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
            <label htmlFor="edit-map">地図で位置を置く</label>
            <MapPicker
              latitude={latitude}
              longitude={longitude}
              onChange={(lat, lng) => {
                setLatitude(lat);
                setLongitude(lng);
              }}
            />
            <label htmlFor="edit-visibility">公開</label>
            <select
              id="edit-visibility"
              value={visibility}
              onChange={(e) => setVisibility(e.target.value as Visibility)}
            >
              <option value="public">公開（みんなの記録に出す）</option>
              <option value="private">非公開（自分だけ）</option>
            </select>
            <label htmlFor="edit-status">記録の種類</label>
            <select
              id="edit-status"
              value={visitStatus}
              onChange={(e) => setVisitStatus(e.target.value as VisitStatus)}
            >
              <option value="visited">訪問済み</option>
              <option value="want">行きたい</option>
            </select>
            <label htmlFor="edit-body">感想（写真だけでも可）</label>
            <textarea
              id="edit-body"
              value={body}
              maxLength={280}
              onChange={(e) => setBody(e.target.value)}
            />
            <p className="counter">{body.length} / 280</p>
            <PhotoField id="edit-image" file={image} existingUrl={existingUrl} onChange={setImage} />
            <div className="err">{error}</div>
            <div className="row-actions">
              <button type="button" className="btn ghost" onClick={onDone}>
                キャンセル
              </button>
              <button className="btn" type="submit" disabled={busy}>
                保存する
              </button>
            </div>
          </form>
        )}
      </section>
    </main>
  );
}
