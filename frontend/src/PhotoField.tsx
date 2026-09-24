import { useEffect, useRef, useState, type DragEvent } from "react";

type Props = {
  id: string;
  file: File | null;
  existingUrl?: string | null;
  onChange: (file: File | null) => void;
};

export function PhotoField({ id, file, existingUrl, onChange }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);

  useEffect(() => {
    if (!file) {
      setPreview(null);
      return;
    }
    const url = URL.createObjectURL(file);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  function takeFile(next: File | null) {
    onChange(next);
    if (!next && inputRef.current) {
      inputRef.current.value = "";
    }
  }

  function onDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragOver(false);
    takeFile(event.dataTransfer.files?.[0] ?? null);
  }

  const shown = preview || existingUrl || null;

  return (
    <div className="photo-field">
      <label htmlFor={id}>写真</label>
      <div
        className={`photo-drop${dragOver ? " over" : ""}${shown ? " has-photo" : ""}`}
        onDragOver={(event) => {
          event.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={onDrop}
      >
        {shown ? (
          <img src={shown} alt="選んだ写真のプレビュー" className="photo-preview" />
        ) : (
          <span className="photo-drop-copy">
            <strong>写真を選んでみんなと共有</strong>
            <span>クリック、またはここにドロップ</span>
            <span className="hint">JPEG / PNG / WebP、5MBまで。感想がなくても写真だけで投稿できます</span>
          </span>
        )}
        <input
          id={id}
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={(e) => takeFile(e.target.files?.[0] ?? null)}
        />
      </div>
      {file ? (
        <button type="button" className="btn ghost photo-clear" onClick={() => takeFile(null)}>
          写真をやめる
        </button>
      ) : null}
    </div>
  );
}
