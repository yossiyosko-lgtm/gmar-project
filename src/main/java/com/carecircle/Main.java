package com.carecircle;

// HttpServer - מחלקה מובנית ב-Java (בלי framework חיצוני) שמאפשרת להקים שרת HTTP פשוט.
// זה מה שאפשר לנו לכתוב "plain Java" בלי Spring Boot, בדיוק כמו בקורס.
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress; // מייצג כתובת+פורט שהשרת יאזין להם

/**
 * נקודת הכניסה לתוכנית (Entry Point).
 * שלב 1 בפרויקט: שרת מינימלי שיודע לענות רק על /health.
 * בשלבים הבאים נוסיף כאן endpoints נוספים (POST /people, POST /checkins וכו')
 * ונחבר את השרת ל-DynamoDB, SQS ו-SNS דרך LocalStack.
 */
public class Main {

    public static void main(String[] args) throws IOException {

        // קוראים את מספר הפורט ממשתנה סביבה בשם PORT.
        // אם הוא לא הוגדר (למשל כשמריצים מקומית בלי Docker) — נשתמש כברירת מחדל בפורט 8080.
        // זה חשוב כי כשנריץ בתוך Docker Compose, נרצה לשלוט בפורט מבחוץ בלי לשנות קוד.
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // יוצרים את השרת עצמו, ואומרים לו על איזו כתובת/פורט להאזין.
        // ה-0 השני הוא "backlog" - כמה בקשות ממתינות מותר לצבור בתור לפני שדוחים; 0 = ברירת המחדל של המערכת.
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // מגדירים "מסלול" (context): כל בקשה שמגיעה לכתובת /health תופעל דרך הקוד הזה.
        // exchange הוא האובייקט שמייצג את הבקשה שנכנסה ואת התשובה שנרצה לשלוח.
        server.createContext("/health", exchange -> {

            // זו התשובה שנחזיר - מחרוזת JSON פשוטה שאומרת שהשרת חי ותקין.
            String response = "{\"status\":\"ok\"}";

            // מודיעים לדפדפן/ל-curl שהתוכן שאנחנו מחזירים הוא JSON.
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // שולחים את קוד הסטטוס 200 (הצלחה) יחד עם אורך התשובה בבתים.
            exchange.sendResponseHeaders(200, response.getBytes().length);

            // כותבים בפועל את גוף התשובה, וסוגרים את החיבור.
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        // null אומר לשרת להשתמש ב-executor המובנה שלו (thread לכל בקשה) - מספיק לצרכי הפרויקט הזה.
        server.setExecutor(null);

        // מפעילים את השרת בפועל - מהרגע הזה הוא מקשיב לבקשות.
        server.start();

        System.out.println("Care Circle server running on port " + port);
    }
}