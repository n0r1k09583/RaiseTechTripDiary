import { useEffect, useRef, useState, type FormEvent } from "react";
import { MapPicker } from "./MapPicker";
import { areaFromPoint } from "./placeFromMap";
import { consultTravel, type ConsultMemory } from "./travelConsult";

type ChatItem = {
  id: number;
  from: "guide" | "me";
  text: string;
};

const QUICK = ["パリ", "ローマ", "京都", "ニューヨーク", "有名なところ"];

type Props = {
  displayName: string;
};

export function GuideWalker({ displayName }: Props) {
  const [pin, setPin] = useState<{ latitude: number; longitude: number } | null>(null);
  const [draft, setDraft] = useState("");
  const [memory, setMemory] = useState<ConsultMemory>({ place: null });
  const [messages, setMessages] = useState<ChatItem[]>(() => [
    {
      id: 1,
      from: "guide",
      text: `${displayName}さん、案内のそらです。世界の観光地と有名なところを案内します。地図を押すか、行きたい都市や建物の名前を送ってください。`,
    },
  ]);
  const nextId = useRef(2);
  const logRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const log = logRef.current;
    if (log) log.scrollTop = log.scrollHeight;
  }, [messages]);

  function ask(raw: string) {
    const text = raw.trim();
    if (!text) return;
    const answer = consultTravel(text, displayName, memory);
    setMemory({ place: answer.place });
    const id = nextId.current;
    nextId.current += 2;
    setMessages((prev) => [
      ...prev,
      { id, from: "me", text },
      { id: id + 1, from: "guide", text: answer.text },
    ]);
    setDraft("");
  }

  function choosePlace(latitude: number | null, longitude: number | null) {
    if (latitude == null || longitude == null) {
      setPin(null);
      return;
    }
    setPin({ latitude, longitude });
    const place = areaFromPoint(latitude, longitude);
    const label = place.label === place.area ? place.area : `${place.area} ${place.label}`;
    ask(`ここに行きたい ${label}`);
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    ask(draft);
  }

  return (
    <section className="plan-room" aria-label="みんなの旅行プラン">
      <header className="plan-room-head">
        <div className="plan-lane">
          <div className="plan-walker">
            <Stewardess />
          </div>
        </div>
        <p>
          みんなの旅行プラン
          <span>そらとの相談 · 案内のスチュワーデス</span>
        </p>
        <p className="plan-map-label">世界地図を押すと、ここに行きたい</p>
        <MapPicker
          world
          latitude={pin?.latitude ?? null}
          longitude={pin?.longitude ?? null}
          onChange={choosePlace}
        />
      </header>
      <div className="guide-chat">
        <div className="guide-log" ref={logRef}>
          {messages.map((message) => (
            <div key={message.id} className={message.from === "me" ? "guide-bubble mine" : "guide-bubble"}>
              <p>{message.text}</p>
            </div>
          ))}
        </div>
        <div className="guide-quick">
          {QUICK.map((label) => (
            <button key={label} type="button" onClick={() => ask(label)}>
              {label}
            </button>
          ))}
        </div>
        <form className="guide-form" onSubmit={submit}>
          <label className="sr-only" htmlFor="guide-draft">
            そらへの相談
          </label>
          <input
            id="guide-draft"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="どこへ行きたいですか？"
          />
          <button className="btn" type="submit">
            相談する
          </button>
        </form>
      </div>
    </section>
  );
}

function Stewardess() {
  return (
    <svg className="stewardess" viewBox="0 0 168 176" aria-hidden="true">
      <ellipse className="stew-shadow" cx="116" cy="168" rx="28" ry="5" />
      <g className="trip-flag">
        <line className="flag-pole" x1="36" y1="58" x2="36" y2="148" />
        <path className="flag-cloth" d="M38 50 C62 44 84 50 100 42 L100 78 C80 88 58 80 38 88 Z" />
        <text className="flag-word" x="52" y="72">
          trip
        </text>
      </g>
      <path className="hair back" d="M90 58 C86 22 142 18 146 60 C150 92 134 104 122 96 C108 108 94 96 90 58 Z" />
      <ellipse className="face" cx="116" cy="62" rx="26" ry="28" />
      <path className="hair" d="M92 52 C102 28 134 26 144 50 C134 40 126 52 116 44 C106 54 98 40 92 52 Z" />
      <path className="hair" d="M90 60 C80 80 86 108 100 112 C92 90 94 72 90 60 Z" />
      <path className="hat" d="M94 34 H140 C140 24 130 16 116 16 C102 16 94 24 94 34 Z" />
      <rect className="hat-band" x="94" y="30" width="46" height="6" rx="2" />
      <circle className="pin" cx="116" cy="24" r="3.2" />
      <ellipse className="eye-white" cx="106" cy="60" rx="6.5" ry="7.5" />
      <ellipse className="eye-white" cx="128" cy="60" rx="6.5" ry="7.5" />
      <circle className="eye" cx="107" cy="62" r="3.4" />
      <circle className="eye" cx="129" cy="62" r="3.4" />
      <circle className="eye-shine" cx="108.4" cy="60.2" r="1.3" />
      <circle className="eye-shine" cx="130.4" cy="60.2" r="1.3" />
      <ellipse className="blush" cx="96" cy="72" rx="5.5" ry="3.2" />
      <ellipse className="blush" cx="136" cy="72" rx="5.5" ry="3.2" />
      <path className="smile" d="M108 76 Q116 84 124 76" />
      <path className="scarf" d="M106 88 L116 104 L126 88 Z" />
      <path className="jacket" d="M98 92 C98 86 134 86 134 92 L138 122 C138 128 94 128 94 122 Z" />
      <path className="collar" d="M106 92 L116 104 L126 92" />
      <path className="skirt" d="M96 120 H136 L146 150 H86 Z" />
      <g className="leg far">
        <path className="stocking" d="M104 146 H112 V162 H104 Z" />
        <ellipse className="shoe" cx="108" cy="164" rx="8" ry="4" />
      </g>
      <g className="leg near">
        <path className="stocking" d="M120 146 H128 V162 H120 Z" />
        <ellipse className="shoe" cx="124" cy="164" rx="8" ry="4" />
      </g>
      <path className="arm" d="M100 100 C78 110 52 116 38 118" />
      <circle className="glove" cx="38" cy="120" r="5.5" />
      <path className="arm" d="M134 100 C148 110 146 126 136 130" />
      <circle className="glove" cx="136" cy="132" r="5.5" />
    </svg>
  );
}
