import type { Post } from "./api";
import type { Area } from "./areas";

type Shop = { name: string; note: string };

const FOOD = /カフェ|珈琲|コーヒー|レストラン|食堂|ご飯|ランチ|スイーツ|甘味|寿司|そば|ラーメン|食事/;

const SHOPS: Record<Area, { cafes: Shop[]; restaurants: Shop[] }> = {
  北海道: {
    cafes: [{ name: "森彦", note: "札幌・円山の自家焙煎" }],
    restaurants: [{ name: "スープカレー ピカンティ", note: "札幌のスープカレー" }],
  },
  東北: {
    cafes: [{ name: "蔵カフェ", note: "仙台のレンガ倉庫あたり" }],
    restaurants: [{ name: "牛タン通りの店", note: "仙台牛タン" }],
  },
  関東: {
    cafes: [{ name: "猿田彦珈琲", note: "東京の珈琲" }],
    restaurants: [{ name: "たいめいけん", note: "洋食のオムライス" }],
  },
  中部: {
    cafes: [{ name: "コメダ珈琲", note: "名古屋名物のモーニング" }],
    restaurants: [{ name: "ひつまぶし系の店", note: "名古屋のうなぎ" }],
  },
  近畿: {
    cafes: [{ name: "スマート珈琲店", note: "京都の名曲とコーヒー" }],
    restaurants: [{ name: "大阪の串カツ店", note: "新世界や難波" }],
  },
  中国: {
    cafes: [{ name: "尾道の坂カフェ", note: "坂の途中の一軒" }],
    restaurants: [{ name: "広島お好み焼き", note: "広島市内" }],
  },
  四国: {
    cafes: [{ name: "金比羅さんの門前カフェ", note: "香川" }],
    restaurants: [{ name: "讃岐うどん", note: "高松や丸亀" }],
  },
  九州: {
    cafes: [{ name: "福岡の珈琲店", note: "天神や博多" }],
    restaurants: [{ name: "もつ鍋", note: "福岡の夕食" }],
  },
  沖縄: {
    cafes: [{ name: "カフェくるくま", note: "国際通り近く" }],
    restaurants: [{ name: "沖縄そば", note: "那覇の昼ごはん" }],
  },
  海外: {
    cafes: [{ name: "地元のカフェ", note: "朝はここで" }],
    restaurants: [{ name: "地元の食堂", note: "昼はその土地の定番" }],
  },
};

const ABROAD: Record<string, { cafes: Shop[]; restaurants: Shop[] }> = {
  ヨーロッパ: {
    cafes: [{ name: "カフェ・ド・フロール", note: "パリの定番" }],
    restaurants: [{ name: "ビストロ", note: "昼の定食がねらい目" }],
  },
  北アメリカ: {
    cafes: [{ name: "近所のコーヒーショップ", note: "朝の一杯" }],
    restaurants: [{ name: "ダイナー", note: "ブランチに向いています" }],
  },
  ハワイ: {
    cafes: [{ name: "島のカフェ", note: "朝はアサイー" }],
    restaurants: [{ name: "プレートランチ", note: "昼はこれで十分" }],
  },
  東南アジア: {
    cafes: [{ name: "ローカルカフェ", note: "氷コーヒー" }],
    restaurants: [{ name: "屋台とローカル食堂", note: "昼はここが楽しい" }],
  },
  オセアニア: {
    cafes: [{ name: "フラットホワイトの店", note: "朝カフェ" }],
    restaurants: [{ name: "魚介の店", note: "港の近く" }],
  },
};

function line(shops: Shop[]) {
  return shops.map((shop) => `${shop.name}（${shop.note}）`).join("、");
}

export function diningParagraph(area: Area, label: string, posts: Post[]) {
  const shops = area === "海外" ? (ABROAD[label] ?? SHOPS.海外) : SHOPS[area];
  const seen = new Set<string>();
  const fromRecords = posts.filter((post) => {
    if (post.areaTag !== area || !FOOD.test(`${post.spotName}${post.body}`)) return false;
    if (seen.has(post.spotName)) return false;
    seen.add(post.spotName);
    return true;
  });
  const record =
    fromRecords.length > 0
      ? `みんなの記録では、${fromRecords
          .slice(0, 2)
          .map((post) => `${post.displayName}さんの「${post.spotName}」`)
          .join("と")}が食事の手がかりです。`
      : "";
  return `カフェは${line(shops.cafes)}。レストランは${line(shops.restaurants)}。${record}営業は行く前に確認してください。`;
}
