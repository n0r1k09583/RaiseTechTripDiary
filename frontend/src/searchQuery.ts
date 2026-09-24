export function normalizeUserQuery(raw: string) {
  return raw.trim().replace(/^@+/u, "").trim();
}

export function queryFromSearchForm(form: HTMLFormElement) {
  return normalizeUserQuery(String(new FormData(form).get("q") ?? ""));
}

export function isImeConfirmKey(event: { key: string; nativeEvent: { isComposing?: boolean }; keyCode?: number }) {
  return event.key === "Enter" && (event.nativeEvent.isComposing === true || event.keyCode === 229);
}
