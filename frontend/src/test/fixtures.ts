import type { Comment, Post, User } from "../api";

export const user: User = {
  id: 1,
  email: "yamada@example.com",
  username: "yamada",
  displayName: "山田",
};

export function post(overrides: Partial<Post> = {}): Post {
  return {
    id: 10,
    userId: 1,
    username: "yamada",
    displayName: "山田",
    spotName: "函館山",
    areaTag: "北海道",
    visitStatus: "visited",
    body: "本文です",
    imageUrl: null,
    latitude: null,
    longitude: null,
    visibility: "public",
    createdAt: "2026-09-01 12:00:00",
    updatedAt: "2026-09-01 12:00:00",
    mine: true,
    commentCount: 0,
    likeCount: 0,
    likedByMe: false,
    favoritedByMe: false,
    ...overrides,
  };
}

export function comment(overrides: Partial<Comment> = {}): Comment {
  return {
    id: 3,
    postId: 10,
    userId: 1,
    username: "yamada",
    displayName: "山田",
    body: "コメントです",
    createdAt: "2026-09-01 12:05:00",
    mine: true,
    ...overrides,
  };
}
