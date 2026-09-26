export type ConsultMemory = {
  place: string | null;
};

export type ConsultAnswer = {
  text: string;
  place: string | null;
};

type Sight = {
  name: string;
  note: string;
};

type PlaceGuide = {
  id: string;
  name: string;
  keys: string[];
  sights: Sight[];
  photo: string;
  first: string;
  day: string;
};

const PLACES: PlaceGuide[] = [
  {
    id: "paris",
    name: "パリ",
    keys: ["パリ", "フランス", "ヨーロッパ", "エッフェル", "ルーブル", "凱旋門", "モンマルトル", "ノートルダム"],
    sights: [
      { name: "エッフェル塔", note: "パリの目印です。夕方が見やすいです" },
      { name: "ルーブル美術館", note: "モナ・リザがあります" },
      { name: "凱旋門", note: "シャンゼリゼ通りの突き当たりです" },
    ],
    photo: "エッフェル塔を川の対岸から撮るのが分かりやすいです。",
    first: "初めてなら、エッフェル塔、セーヌ川、ルーブルの順が動きやすいです。",
    day: "日帰りならエッフェル塔とセーヌ川。2泊3日ならルーブルとモンマルトルを足します。",
  },
  {
    id: "rome",
    name: "ローマ",
    keys: ["ローマ", "イタリア", "コロッセオ", "バチカン", "トレビ", "パンテオン"],
    sights: [
      { name: "コロッセオ", note: "古代ローマの競技場です" },
      { name: "バチカン", note: "サン・ピエトロ大聖堂と美術館です" },
      { name: "トレビの泉", note: "夜がにぎやかです" },
    ],
    photo: "コロッセオは外側の通りから全体が入ります。",
    first: "初めてなら、コロッセオ、フォロ・ロマーノ、トレビの泉の順です。",
    day: "日帰りならコロッセオとトレビの泉。2泊3日ならバチカンを別の日にします。",
  },
  {
    id: "london",
    name: "ロンドン",
    keys: ["ロンドン", "イギリス", "ビッグベン", "大英博物館", "バッキンガム", "タワーブリッジ"],
    sights: [
      { name: "ビッグベン", note: "正式にはエリザベス・タワーです" },
      { name: "大英博物館", note: "入館は無料の日が多いです" },
      { name: "タワーブリッジ", note: "テムズ川の眺めが分かりやすいです" },
    ],
    photo: "タワーブリッジは対岸から橋全体を入れると写ります。",
    first: "初めてなら、ビッグベン、ウェストミンスター寺院、タワーブリッジです。",
    day: "日帰りならテムズ川沿い。2泊3日なら大英博物館を半日取ります。",
  },
  {
    id: "barcelona",
    name: "バルセロナ",
    keys: ["バルセロナ", "スペイン", "サグラダ", "グエル", "ランブラス"],
    sights: [
      { name: "サグラダ・ファミリア", note: "ガウディの教会です。中も見た方がよいです" },
      { name: "グエル公園", note: "丘の上で、街を見下ろせます" },
      { name: "ランブラス通り", note: "港まで歩ける通りです" },
    ],
    photo: "サグラダ・ファミリアは正面の池の側が全体を入れやすいです。",
    first: "初めてなら、サグラダ・ファミリアを先に見て、午後にグエル公園です。",
    day: "日帰りならサグラダ・ファミリア中心。2泊3日ならグエル公園とゴシック地区を足します。",
  },
  {
    id: "hawaii",
    name: "ハワイ",
    keys: ["ハワイ", "ホノルル", "ワイキキ", "ダイヤモンドヘッド"],
    sights: [
      { name: "ワイキキビーチ", note: "海に入るならここが分かりやすいです" },
      { name: "ダイヤモンドヘッド", note: "朝の登山でホノルルを見渡せます" },
      { name: "パールハーバー", note: "歴史を見る場所です" },
    ],
    photo: "ダイヤモンドヘッドの頂上からワイキキ方向が定番です。",
    first: "初めてなら、ワイキキで海を見て、余力があればダイヤモンドヘッドです。",
    day: "日帰りならワイキキ。2泊3日ならダイヤモンドヘッドとパールハーバーを分けます。",
  },
  {
    id: "newyork",
    name: "ニューヨーク",
    keys: ["ニューヨーク", "北アメリカ", "アメリカ", "自由の女神", "セントラルパーク", "タイムズスクエア"],
    sights: [
      { name: "自由の女神", note: "船で近くまで行くと大きさが出ます" },
      { name: "セントラルパーク", note: "街の中の大きな公園です" },
      { name: "タイムズスクエア", note: "夜の看板が有名です" },
    ],
    photo: "自由の女神はフェリーの上から撮るのが簡単です。",
    first: "初めてなら、自由の女神、セントラルパーク、タイムズスクエアの順です。",
    day: "日帰りならセントラルパークとタイムズスクエア。2泊3日なら自由の女神を別日にします。",
  },
  {
    id: "seoul",
    name: "ソウル",
    keys: ["ソウル", "韓国", "景福宮", "明洞", "南山", "北村"],
    sights: [
      { name: "景福宮", note: "ソウルの中心にある宮殿です" },
      { name: "北村韓屋村", note: "景福宮のとなりの古い町並みです" },
      { name: "Nソウルタワー", note: "南山から夜の街が見えます" },
    ],
    photo: "景福宮の光化門前から宮殿の正面が入ります。",
    first: "初めてなら、景福宮と北村を午前、明洞を午後にします。",
    day: "日帰りなら景福宮と北村。2泊3日なら南山の夜景を足します。",
  },
  {
    id: "taipei",
    name: "台北",
    keys: ["台北", "台湾", "故宮", "台北101", "九份", "士林"],
    sights: [
      { name: "故宮博物院", note: "翠玉白菜が有名です" },
      { name: "台北101", note: "街の高い目印です" },
      { name: "九份", note: "坂の町で、夕方の明かりが有名です" },
    ],
    photo: "台北101は象山の展望台から全体が入ります。",
    first: "初めてなら、市内は故宮と台北101。九份は別の半日です。",
    day: "日帰りなら故宮と台北101。2泊3日なら九份を一日にします。",
  },
  {
    id: "bangkok",
    name: "バンコク",
    keys: ["バンコク", "タイ", "東南アジア", "ワットアルン", "王宮", "ワットポー"],
    sights: [
      { name: "ワット・アルン", note: "川向こうの暁の寺です" },
      { name: "王宮", note: "エメラルド仏があります" },
      { name: "ワット・ポー", note: "寝仏が有名です" },
    ],
    photo: "ワット・アルンは対岸の船着き場から撮ると塔が入ります。",
    first: "初めてなら、王宮とワット・ポーを午前、ワット・アルンを午後にします。",
    day: "日帰りなら王宮周辺。2泊3日なら川の両岸を日で分けます。",
  },
  {
    id: "singapore",
    name: "シンガポール",
    keys: ["シンガポール", "マーライオン", "ガーデンズ・バイ・ザ・ベイ", "マリーナベイ"],
    sights: [
      { name: "マーライオン", note: "マリーナベイの目印です" },
      { name: "ガーデンズ・バイ・ザ・ベイ", note: "夜のライトアップが有名です" },
      { name: "セントーサ", note: "海側の島です" },
    ],
    photo: "マーライオン公園からマリーナベイ・サンズを入れる構図が定番です。",
    first: "初めてなら、マーライオンとガーデンズ・バイ・ザ・ベイを同じ日にします。",
    day: "日帰りならマリーナベイ周辺。2泊3日ならセントーサを別日にします。",
  },
  {
    id: "sydney",
    name: "シドニー",
    keys: ["シドニー", "オーストラリア", "オセアニア", "オペラハウス", "ハーバーブリッジ", "ボンダイ"],
    sights: [
      { name: "オペラハウス", note: "港の白い建物です" },
      { name: "ハーバーブリッジ", note: "オペラハウスとセットで見ます" },
      { name: "ボンダイビーチ", note: "市街から出た海岸です" },
    ],
    photo: "ミセス・マッコーリーズ・ポイントからオペラハウスと橋が一緒に入ります。",
    first: "初めてなら、オペラハウスとハーバーブリッジを先に見ます。",
    day: "日帰りなら港。2泊3日ならボンダイを半日足します。",
  },
  {
    id: "brisbane",
    name: "ブリスベン",
    keys: ["ブリスベン", "クイーンズランド", "サウスバンク", "ストーリーブリッジ"],
    sights: [
      { name: "サウスバンク", note: "川沿いの公園で、街の中心です" },
      { name: "ストーリーブリッジ", note: "川に架かる橋で、上から街を見られます" },
      { name: "ローンパイン", note: "コアラを近くで見られる公園です" },
    ],
    photo: "サウスバンクの川岸からストーリーブリッジを入れると分かりやすいです。",
    first: "初めてなら、サウスバンクを歩いて、ストーリーブリッジを見ます。",
    day: "日帰りならサウスバンク。2泊3日ならローンパインを別の半日にします。",
  },
  {
    id: "melbourne",
    name: "メルボルン",
    keys: ["メルボルン", "フリンダース", "クイーン・ビクトリア"],
    sights: [
      { name: "フリンダース・ストリート駅", note: "黄色い建物が目印です" },
      { name: "クイーン・ビクトリア・マーケット", note: "食料品の市場です" },
      { name: "王立植物園", note: "街の中の大きな庭です" },
    ],
    photo: "川の対岸からフリンダース・ストリート駅の正面が入ります。",
    first: "初めてなら、駅の前から市場まで歩いて街の感じを見ます。",
    day: "日帰りなら駅と市場。2泊3日なら植物園を午前にします。",
  },
  {
    id: "cairo",
    name: "カイロ",
    keys: ["カイロ", "エジプト", "ピラミッド", "スフィンクス", "ギザ"],
    sights: [
      { name: "ギザのピラミッド", note: "三大ピラミッドが並びます" },
      { name: "スフィンクス", note: "ピラミッドのそばにあります" },
      { name: "エジプト考古学博物館", note: "ツタンカーメンの遺物があります" },
    ],
    photo: "ピラミッドは正面の広場から三基を入れると分かりやすいです。",
    first: "初めてなら、ギザのピラミッドとスフィンクスを同じ午前にします。",
    day: "日帰りならギザ。2泊3日なら博物館を別の日にします。",
  },
  {
    id: "fuji",
    name: "富士山",
    keys: ["富士山", "河口湖", "箱根"],
    sights: [
      { name: "河口湖", note: "湖面ごしの富士山が有名です" },
      { name: "忠霊塔", note: "富士山を正面に入れやすい場所です" },
      { name: "忍野八海", note: "富士山の湧水が並びます" },
    ],
    photo: "天気の朝、河口湖の北岸から富士山が入ります。",
    first: "初めてなら、河口湖で富士山を見て、天気がよければ忠霊塔です。",
    day: "日帰りなら河口湖。2泊3日なら忍野八海を足します。",
  },
  {
    id: "hokkaido",
    name: "北海道",
    keys: ["北海道", "札幌", "函館", "小樽", "富良野"],
    sights: [
      { name: "函館山", note: "夜景が有名です" },
      { name: "小樽運河", note: "倉庫の並ぶ運河です" },
      { name: "富良野", note: "夏のラベンダーが有名です" },
    ],
    photo: "函館山の展望台から港の夜景を撮るのが定番です。",
    first: "初めてなら、函館山か小樽運河のどちらかを先に決めます。",
    day: "日帰りなら札幌から小樽。2泊3日なら函館を別日にします。",
  },
  {
    id: "okinawa",
    name: "沖縄",
    keys: ["沖縄", "那覇", "首里城", "美ら海", "国際通り"],
    sights: [
      { name: "首里城", note: "那覇の高い場所にある城です" },
      { name: "美ら海水族館", note: "大きな水槽でジンベエザメが見られます" },
      { name: "国際通り", note: "那覇の中心の通りです" },
    ],
    photo: "首里城は正殿の前の広場から全体が入ります。",
    first: "初めてなら、那覇は首里城、北部へ出るなら美ら海水族館です。",
    day: "日帰りなら首里城と国際通り。2泊3日なら美ら海を別日にします。",
  },
  {
    id: "osaka",
    name: "大阪",
    keys: ["大阪", "道頓堀", "通天閣", "大阪城"],
    sights: [
      { name: "大阪城", note: "天守閣から街を見渡せます" },
      { name: "道頓堀", note: "グリコの看板がある川沿いです" },
      { name: "通天閣", note: "新世界の高い塔です" },
    ],
    photo: "大阪城はお堀の外から天守を入れると分かりやすいです。",
    first: "初めてなら、大阪城を先に見て、夕方に道頓堀です。",
    day: "日帰りなら大阪城と道頓堀。2泊3日なら通天閣を別日にします。",
  },
  {
    id: "kyoto",
    name: "京都",
    keys: ["京都", "近畿", "清水寺", "金閣", "伏見稲荷", "嵐山"],
    sights: [
      { name: "清水寺", note: "東山の舞台が有名です" },
      { name: "金閣寺", note: "池に映る金の建物です" },
      { name: "伏見稲荷大社", note: "千本鳥居が続いています" },
    ],
    photo: "金閣寺は池のほとりから建物と映り込みが入ります。",
    first: "初めてなら、清水寺、二年坂、伏見稲荷の順が動きやすいです。",
    day: "日帰りなら清水寺と二年坂。2泊3日なら金閣寺と嵐山を日で分けます。",
  },
  {
    id: "tokyo",
    name: "東京",
    keys: ["東京", "関東", "浅草", "スカイツリー", "渋谷", "明治神宮", "鎌倉"],
    sights: [
      { name: "浅草寺", note: "雷門から仲見世を通って本堂です" },
      { name: "東京スカイツリー", note: "浅草から見て高さが分かります" },
      { name: "明治神宮", note: "原宿のとなりの大きな森です" },
    ],
    photo: "雷門の正面から大提灯を入れるのが定番です。",
    first: "初めてなら、浅草寺とスカイツリーを同じ日にします。",
    day: "日帰りなら浅草。2泊3日なら明治神宮か鎌倉を別日にします。",
  },
  {
    id: "tohoku",
    name: "仙台",
    keys: ["東北", "仙台", "松島", "中尊寺"],
    sights: [
      { name: "松島", note: "湾に島が並ぶ景色です" },
      { name: "瑞巌寺", note: "松島にある伊達家の寺です" },
      { name: "中尊寺", note: "平泉の金色堂が有名です" },
    ],
    photo: "松島は遊覧船の上から島の重なりを撮ると分かりやすいです。",
    first: "初めてなら、仙台から松島へ出て、瑞巌寺を見ます。",
    day: "日帰りなら松島。2泊3日なら平泉の中尊寺を別日にします。",
  },
];

const FAMOUS = ["エッフェル塔", "コロッセオ", "自由の女神", "清水寺", "オペラハウス"];

function findPlace(text: string): { place: PlaceGuide; key: string } | null {
  let best: PlaceGuide | null = null;
  let key = "";
  for (const place of PLACES) {
    for (const word of place.keys) {
      if (text.includes(word) && word.length > key.length) {
        best = place;
        key = word;
      }
    }
  }
  return best ? { place: best, key } : null;
}

function byName(name: string | null) {
  return PLACES.find((place) => place.name === name) ?? null;
}

function sightLine(place: PlaceGuide) {
  return place.sights.map((sight) => `${sight.name}（${sight.note}）`).join("、");
}

function leadSight(place: PlaceGuide, key: string) {
  return place.sights.find((sight) => key.includes(sight.name.slice(0, 2)) || sight.name.includes(key)) ?? null;
}

export function consultTravel(message: string, guestName: string, previous: ConsultMemory): ConsultAnswer {
  const text = message.trim();
  if (/ありがとう/.test(text)) {
    return {
      text: `${guestName}さん、また行きたい場所があれば、地図を押すか、都市や有名な建物の名前を送ってください。`,
      place: previous.place,
    };
  }
  if (/^(こんにちは|こんばんは|やあ|はじめまして)/.test(text) && !findPlace(text)) {
    return {
      text: `${guestName}さん、案内のそらです。世界の観光地と有名なところを案内します。パリ、ローマ、京都のように送るか、世界地図を押してください。`,
      place: previous.place,
    };
  }

  const found = findPlace(text);
  const pinned = text.includes("ここに行きたい");
  const followUp =
    /日帰り|1日|１日|1泊|一泊|2泊|二泊|3泊|三泊|何日|プラン|写真|映え|撮|初めて|はじめて|最初|他には|別の|ほかの|違う|ありがとう|こんにちは|こんばんは/.test(
      text,
    ) || text === "有名なところ";
  const place = found?.place ?? (followUp || pinned ? byName(previous.place) : null);
  if (!found && !followUp && !pinned) {
    const topic = text
      .replace(/[？?！!。、\s]/g, "")
      .replace(/(について|を教えて|教えて|の観光地|の有名なところ|は)$/g, "")
      .slice(0, 24);
    if (topic) {
      return {
        text: `${guestName}さん、${topic}は${previous.place ? `${previous.place}とは別の場所です。` : ""}その土地の城、橋、展望、古い街並みから、有名なところを見ると回りやすいです。建物の名前が分かれば、それも案内できます。`,
        place: topic,
      };
    }
  }
  const wantsOther = /他には|別の|ほかの|違うところ/.test(text);
  const wantsList = /おすすめ|有名|観光|見どころ|何が|なにが|どこが|教えて|案内/.test(text);
  const wantsPhoto = /写真|映え|撮/.test(text);
  const wantsFirst = /初めて|はじめて|最初/.test(text);
  const wantsDays = /日帰り|1日|１日|1泊|一泊|2泊|二泊|3泊|三泊|何日|プラン/.test(text);

  if (wantsOther) {
    const next = PLACES.find((item) => item.name !== place?.name) ?? PLACES[0];
    return {
      text: `${guestName}さん、ほかには${next.name}も有名です。${sightLine(next)}。気になる名前を送ってください。`,
      place: next.name,
    };
  }

  if (!place) {
    return {
      text: `${guestName}さん、観光地と有名なところなら案内できます。たとえば${FAMOUS.join("、")}です。行きたい都市を送るか、世界地図を押してください。`,
      place: null,
    };
  }

  const named = found ? leadSight(place, found.key) : null;
  const pin = pinned ? "地図のピン、受け取りました。" : "";
  let body = `${place.name}の有名なところは、${sightLine(place)}です。`;
  if (named && found && found.key !== place.name && !place.keys.slice(0, 3).includes(found.key)) {
    body = `${named.name}は${place.name}の有名なところです。${named.note}。あわせて${sightLine(place)}も押さえると回りやすいです。`;
  } else if (wantsPhoto) {
    body = `${place.name}で写真を撮るなら、${place.photo}`;
  } else if (wantsFirst) {
    body = place.first;
  } else if (wantsDays) {
    body = place.day;
  } else if (wantsList || pinned) {
    body = `${place.name}の有名なところは、${sightLine(place)}です。${place.first}`;
  }

  return {
    text: `${guestName}さん、${pin}${body}ほかの都市や、日帰りかも続けて聞けます。`,
    place: place.name,
  };
}
