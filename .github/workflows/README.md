# Care Circle

מערכת לניטור בריאות משפחתי: מזהה חריגות בהרגלים היומיומיים של אדם (שעת קימה, שינה, פעילות) לפי z-score אישי, ושולחת התראות בשתי רמות.

## ארכיטקטורה
- **Java 17**: שרת HTTP פשוט, בלי framework
- **DynamoDB**: שמירת אנשים וצ'ק-אינים
- **SQS**: תור צ'ק-אינים לעיבוד אסינכרוני
- **SNS**: התראות עדינות (|z|>2) ודחופות (|z|>3)
- **LocalStack**: סימולציה מקומית של AWS
- **Docker Compose** ו-**GitHub Actions**

## הרצה
```bash
make docker-up
```
השרת עולה על http://localhost:8080 ו-LocalStack על פורט 4566. הטבלאות, התורים והנושאים נוצרים אוטומטית.

## API
| Method | Path | תיאור |
|---|---|---|
| GET | /health | בדיקת חיות |
| POST | /people | יצירת אדם `{"name","circleId"}` |
| GET | /people/{id} | שליפת אדם |
| POST | /checkins | צ'ק-אין `{"personId","type","value"}` |
| GET | /alerts/{personId} | התראות של אדם |

## בדיקות
```bash
make test
```
הטסטים רצים עם Stores בזיכרון ואינם דורשים LocalStack.

## CI
כל push ו-PR מריצים `mvn verify` ובונים את אימג' ה-Docker.