import type { Post } from "./api";
import { AREAS, type Area } from "./areas";
import { diningParagraph } from "./diningTips";

export type PlanStop = {
  postId: number;
  spotName: string;
  displayName: string;
  when: string;
  note: string;
};

export type GuideMemory = {
  area: Area | null;
  days: number | null;
};

export type GuideAnswer = {
  text: string;
  stops: PlanStop[];
  area: Area | null;
  days: number | null;
};

function findArea(text: string): Area | null {
  return AREAS.find((area) => text.includes(area)) ?? null;
}

function findDays(text: string): number | null {
  if (/日帰り|1日/.test(text)) return 1;
  if (/1泊|一泊/.test(text)) return 2;
  if (/3泊|三泊/.test(text)) return 4;
  if (/2泊|二泊/.test(text)) return 3;
  return null;
}

function wantsSuggestion(text: string) {
  return /おすすめ|みんな|一緒|プラン|カフェ|レストラン/.test(text);
}

function wantedOnMap(text: string) {
  return text.includes("ここに行きたい");
}

function placeWords(text: string, area: Area) {
  return text.replace("ここに行きたい", "").replace(area, "").trim();
}

function popularArea(posts: Post[]): Area | null {
  let best: Area | null = null;
  let score = 0;
  for (const area of AREAS) {
    const inArea = posts.filter((post) => post.areaTag === area);
    const next = inArea.reduce((sum, post) => sum + 1 + post.likeCount, 0);
    if (next > score) {
      score = next;
      best = area;
    }
  }
  return best;
}

function uniqueSpots(posts: Post[]): Post[] {
  const seen = new Set<string>();
  const ranked = [...posts].sort(
    (a, b) => b.likeCount - a.likeCount || b.commentCount - a.commentCount || b.id - a.id,
  );
  const spots: Post[] = [];
  for (const post of ranked) {
    if (seen.has(post.spotName)) continue;
    seen.add(post.spotName);
    spots.push(post);
  }
  return spots;
}

function clip(body: string) {
  const text = body.replace(/\s+/g, " ").trim();
  if (!text) return "写真の記録があります";
  return text.length > 42 ? `${text.slice(0, 42)}…` : text;
}

function stayLabel(days: number) {
  return days === 1 ? "日帰り" : `${days - 1}泊${days}日`;
}

export function answerGuide(
  message: string,
  posts: Post[],
  guestName: string,
  previous: GuideMemory,
): GuideAnswer {
  const text = message.trim();
  if (/ありがとう/.test(text)) {
    return {
      text: "どういたしまして。別のエリアも、人数の気分で日数を変えても大丈夫です。",
      stops: [],
      area: previous.area,
      days: previous.days,
    };
  }

  const area = findArea(text) ?? (wantsSuggestion(text) ? popularArea(posts) : previous.area);
  const days = findDays(text) ?? previous.days;
  if (!area) {
    return {
      text: `${guestName}さん、行きたい場所は、世界地図を押すか、北海道、沖縄、近畿のように送ってください。「カフェ」と送ると、その土地のカフェとレストランを案内します。`,
      stops: [],
      area: null,
      days,
    };
  }

  const stayDays = days ?? 3;
  const label = placeWords(text, area) || area;
  const dining = diningParagraph(area, label, posts);
  const spots = uniqueSpots(posts.filter((post) => post.areaTag === area));
  const where = wantedOnMap(text) ? `地図のその場所は${label}です。` : "";
  if (spots.length === 0) {
    const others = AREAS.filter((item) => posts.some((post) => post.areaTag === item));
    const hint = others.length
      ? `いま公開の記録があるのは、${others.join("、")}です。`
      : "公開の記録がまだないので、誰かが場所を残すとプランに入ります。";
    return {
      text: `${guestName}さん、${where}${area}の公開記録は、まだ見当たりません。${hint}${dining}`,
      stops: [],
      area,
      days: stayDays,
    };
  }

  const slotCount = stayDays === 1 ? 2 : stayDays * 2;
  const chosen = spots.slice(0, slotCount);
  const stops: PlanStop[] = chosen.map((post, index) => {
    const day = stayDays === 1 ? 1 : Math.floor(index / 2) + 1;
    const slot = stayDays === 1 ? (index === 0 ? "午前" : "午後") : index % 2 === 0 ? "午前" : "午後";
    return {
      postId: post.id,
      spotName: post.spotName,
      displayName: post.displayName,
      when: `${day}日目 ${slot}`,
      note: clip(post.body),
    };
  });
  const people = [...new Set(stops.map((stop) => `${stop.displayName}さん`))].join("と");
  const short =
    chosen.length < slotCount ? "記録の数に合わせて、訪れる場所は少なめにしています。" : "";
  return {
    text: `${guestName}さん、${where}${people}の記録を合わせて、${area}の${stayLabel(stayDays)}にしました。${short}${dining}気になる場所を押すと、その人の記録が開きます。`,
    stops,
    area,
    days: stayDays,
  };
}
