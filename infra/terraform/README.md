# AWS 構成（定義のみ・課金しない）

授業のコンテナ定義に合わせた形です。**Git に置いてあるだけでは $0 です。** このリポジトリでは `terraform apply` しません。

| 層 | サービス |
|----|----------|
| フロント | S3 + CloudFront（403/404 → `/index.html`。OAC） |
| API | ECS Fargate。CPU 256 / メモリ 512MB / ポート 8080 |
| ログ | CloudWatch `/ecs/tripdiary` |
| 前段 | ALB |
| DB | RDS PostgreSQL 17（プライベートサブネット） |
| 画像 | S3。CloudFront の `/uploads/*` |

## お金をかけないためにやっていること

授業のコスト表（無料枠が切れたアカウント）では月約 $86〜110。内訳の大きいものは NAT Gateway 約 $45、ALB 約 $22、RDS 約 $12。Fargate も無料枠対象外です。

この定義では:

1. **既定は何も作らない**（`enable_infra = false`）。普通に `terraform apply` しても AWS に課金リソースは立たない
2. **NAT Gateway は入れない**（無料枠対象外で一番高い）。Fargate はパブリック IP で ECR を引く
3. Fargate の `desired_count` 既定は **0**
4. 動作確認するときだけ `enable_infra=true`。見終わったら **すぐ `terraform destroy`**。作りっぱなしにしない

提出の動作確認はローカル `5173` と `8080`（SQLite）で足りる。AWS に常時公開しない。

## 短時間だけ確認する場合

```powershell
cd infra/terraform
terraform apply -var "enable_infra=true" -var "desired_count=1" -var "jwt_secret=長いランダムな値"
# 画面を確認したら
terraform destroy -var "enable_infra=true"
```

destroy してあとで apply し直してよい。RDS のデータは消える。空なら Flyway が表を作り直す。

## コンテナ環境変数（授業と同じ名前）

`SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD`、`JWT_SECRET`、`AWS_S3_BUCKET`、`AWS_S3_REGION`、`CORS_ALLOWED_ORIGINS`

Dockerfile はこのリポジトリが Maven（`mvnw`）なので Gradle ではない。JDK 21 のマルチステージは同じ。
