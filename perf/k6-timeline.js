import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 5,
  duration: "20s",
  thresholds: {
    http_req_duration: ["p(95)<800"],
  },
};

const BASE = __ENV.BASE_URL || "http://127.0.0.1:8080";

export default function () {
  const login = http.post(
    `${BASE}/api/login`,
    JSON.stringify({ email: "yamada@example.com", password: "password123" }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(login, { "login 200": (r) => r.status === 200 });
  const token = login.json("accessToken");
  const list = http.get(`${BASE}/api/posts?tab=all&limit=20`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  check(list, { "list 200": (r) => r.status === 200 });
  sleep(1);
}
