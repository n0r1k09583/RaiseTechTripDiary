# E2E（任意）

毎回の `npm test` / CI には載せない。画面と API が起動しているときだけ。

```powershell
cd e2e
npm init -y
npm install -D @playwright/test
npx playwright install chromium
npx playwright test
```

ログイン → みんなの旅 → 行きたいボタンが見えること、までを見る。
