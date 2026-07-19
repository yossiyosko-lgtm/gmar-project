package com.carecircle;

// JUnit 5 - ספריית הטסטים. @Test מסמן פונקציה כטסט; @BeforeAll/@AfterAll מסמנים
// פונקציות "הכנה"/"ניקוי" שרצות פעם אחת סביב כל הטסטים במחלקה.
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// assertEquals - הפונקציה שבפועל "בודקת": משווה בין מה שציפינו לבין מה שהתקבל בפועל.
// אם הם לא שווים - הטסט נכשל ו-JUnit מדפיס דוח מדויק.
import static org.junit.jupiter.api.Assertions.assertEquals;

// HttpServer - אותה מחלקה שמשמשת את Main.java. צריך אותה כאן כי startServer() למטה
// מקבל בחזרה אובייקט מסוג הזה מ-Main.createServer(...).
import com.sun.net.httpserver.HttpServer;

// ארבע המחלקות הבאות הן ה"לקוח HTTP" המובנה ב-Java - בעזרתן נשלח בקשה אמיתית
// לשרת שלנו, בדיוק כמו ש-curl היה עושה, אבל מתוך קוד Java שיכול גם לבדוק את התוצאה.
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * טסט ראשון בפרויקט.
 * המטרה: לוודא שנקודת הקצה /health לא רק "קיימת בקוד", אלא באמת מתנהגת נכון -
 * מחזירה קוד סטטוס 200 וגוף תשובה מדויק - כשמפעילים אותה בפועל.
 */
class HealthEndpointTest {

    // קבוע (final = לא ישתנה): הפורט שעליו נריץ את השרת רק לצורך הטסטים.
    // בכוונה שונה מ-8080 (פורט הפיתוח הרגיל), כדי שלא תהיה התנגשות אם השרת
    // ה"אמיתי" כבר רץ לך במקביל בטרמינל אחר בזמן שאתה מריץ make test.
    private static final int TEST_PORT = 8081;

    // המשתנה שיחזיק את השרת שהרמנו, כדי שנוכל לגשת אליו גם ב-startServer
    // וגם ב-stopServer בהמשך. static כי שתי הפונקציות האלה עצמן static (ראה למטה).
    private static HttpServer server;

    /**
     * רץ פעם אחת בלבד, לפני שכל הטסטים במחלקה הזו מתחילים.
     * static - כי JUnit קורא לפונקציה הזו לפני שהוא בכלל יוצר מופע של המחלקה HealthEndpointTest.
     */
    @BeforeAll
    static void startServer() throws Exception {
        // קוראים לאותה פונקציה בדיוק שגם Main.main() קורא לה - כך אנחנו בודקים
        // את השרת האמיתי, ולא איזו גרסת-בדיקה נפרדת שעלולה להתנהג אחרת.
        server = Main.createServer(TEST_PORT);

        // מפעילים בפועל - מהרגע הזה השרת מקשיב לבקשות על TEST_PORT.
        server.start();
    }

    /**
     * רץ פעם אחת בלבד, אחרי שכל הטסטים במחלקה סיימו לרוץ (גם אם טסט נכשל).
     * חשוב לעצור את השרת - אחרת הוא ממשיך לרוץ ברקע גם אחרי שהטסטים הסתיימו,
     * ותופס את הפורט 8081 לריצות הבאות.
     */
    @AfterAll
    static void stopServer() {
        // ה-0 אומר לעצור מיידית, בלי לחכות לבקשות שאולי עדיין באמצע טיפול.
        server.stop(0);
    }

    /**
     * הטסט עצמו. @Test אומר ל-JUnit "זו בדיקה - הרץ אותי ובדוק אם עברתי".
     * שם הפונקציה מתאר בדיוק מה נבדק - זה עוזר לקרוא דוחות טסטים בעתיד.
     */
    @Test
    void healthEndpointReturns200WithOkStatus() throws Exception {

        // שלב 1: בונים "לקוח HTTP" - האובייקט שיודע לשלוח בקשות רשת.
        HttpClient client = HttpClient.newHttpClient();

        // שלב 2: בונים את הבקשה עצמה - GET לכתובת http://localhost:8081/health
        // (בדיוק כמו curl localhost:8081/health).
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/health"))
                .GET()
                .build();

        // שלב 3: שולחים את הבקשה בפועל דרך הרשת (localhost, אבל עדיין רשת אמיתית),
        // וממתינים לתשובה. BodyHandlers.ofString() אומר "תחזיר לי את הגוף כטקסט רגיל".
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // שלב 4: הבדיקות עצמן - כאן הטסט בפועל "שופט" אם השרת עבד נכון.
        // assertEquals(expected, actual, הודעה-אם-נכשל)

        // בודק שקוד הסטטוס שחזר הוא 200 (הצלחה) - לא 404, לא 500, לא שום דבר אחר.
        assertEquals(200, response.statusCode(), "קוד הסטטוס צריך להיות 200");

        // בודק שגוף התשובה הוא בדיוק המחרוזת שהגדרנו ב-Main.java - לא ריק,
        // לא JSON עם שדות אחרים, אלא בדיוק {"status":"ok"}.
        assertEquals("{\"status\":\"ok\"}", response.body(), "גוף התשובה צריך להיות בדיוק כך");
    }
}
