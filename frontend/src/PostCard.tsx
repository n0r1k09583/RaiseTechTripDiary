import { toggleLike, type Post } from "./api";
import { formatTime } from "./formatTime";

type Props = {
  post: Post;
  onOpen?: (id: number) => void;
  onEdit?: (id: number) => void;
  onDelete?: (post: Post) => void;
  onProfile: (username: string) => void;
  onLiked: (post: Post) => void;
  onError: (message: string) => void;
  large?: boolean;
};

export function PostCard({
  post,
  onOpen,
  onEdit,
  onDelete,
  onProfile,
  onLiked,
  onError,
  large,
}: Props) {
  async function onLike() {
    try {
      onLiked(await toggleLike(post.id));
    } catch (err) {
      onError(err instanceof Error ? err.message : "行きたいの更新に失敗しました");
    }
  }

  const spot = post.spotName || "場所未設定";

  return (
    <article className="post">
      <div className="avatar">{(post.displayName || post.username).slice(0, 1)}</div>
      <div>
        <div>
          <button type="button" className="btn link name-btn" onClick={() => onProfile(post.username)}>
            <span className="name">{post.displayName}</span>
            <span className="handle">@{post.username}</span>
          </button>
          <span className="meta">
            {" "}
            · {formatTime(post.createdAt)}
            {post.mine && onEdit ? (
              <>
                {" "}
                ·{" "}
                <button type="button" className="btn link" onClick={() => onEdit(post.id)}>
                  編集
                </button>
              </>
            ) : null}
            {post.mine && onDelete ? (
              <>
                {" · "}
                <button type="button" className="btn link" onClick={() => onDelete(post)}>
                  削除
                </button>
              </>
            ) : null}
          </span>
        </div>
        {onOpen ? (
          <button type="button" className="post-main" onClick={() => onOpen(post.id)}>
            <p className="spot-title">{spot}</p>
            <div className="badges">
              {post.areaTag ? <span className="badge">{post.areaTag}</span> : null}
              <span className={`badge${post.visitStatus === "want" ? " want" : ""}`}>
                {post.visitStatus === "want" ? "行きたい" : "訪問済み"}
              </span>
            </div>
            {post.imageUrl ? <img className="thumb" src={post.imageUrl} alt={`${spot}の写真`} /> : null}
            {post.body ? <p className="body">{post.body}</p> : null}
          </button>
        ) : (
          <>
            <p className="spot-title">{spot}</p>
            <div className="badges">
              {post.areaTag ? <span className="badge">{post.areaTag}</span> : null}
              <span className={`badge${post.visitStatus === "want" ? " want" : ""}`}>
                {post.visitStatus === "want" ? "行きたい" : "訪問済み"}
              </span>
            </div>
            {post.imageUrl ? (
              <img className={`thumb${large ? " large" : ""}`} src={post.imageUrl} alt={`${spot}の写真`} />
            ) : null}
            {post.body ? <p className="body">{post.body}</p> : null}
          </>
        )}
        <div className="stats">
          <button
            type="button"
            className={`btn like${post.likedByMe ? " on" : ""}`}
            aria-pressed={post.likedByMe}
            onClick={() => void onLike()}
          >
            {post.likedByMe ? "♥ 行きたい" : "♡ 行きたい"} {post.likeCount}
          </button>
          {onOpen ? (
            <button type="button" className="btn link" onClick={() => onOpen(post.id)}>
              コメント {post.commentCount}件
            </button>
          ) : (
            <span>コメント {post.commentCount}件</span>
          )}
        </div>
      </div>
    </article>
  );
}
