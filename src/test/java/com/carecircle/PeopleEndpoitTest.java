package com.carecircle;

// JUnit 5 - @Test מסמן טסט; @BeforeAll/@AfterAll רצים פעם אחת לפני/אחרי כל הטסטים במחלקה.
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// assertEquals משווה בין הצפוי לבפועל; אם שונים - הטסט נכשל.
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;

// לקוח ה-HTTP המובנה ב-Java - שולח בקשות אמיתיות לשרת, כמו curl.
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

// גרסאות זיכרון של ה-Stores - כך הטסט לא תלוי ב-LocalStack
import com.carecircle.store.InMemoryPeopleStore;
import com.carecircle.store.InMemoryCheckInStore;


public class PeopleEndpoitTest {
    // פורט ייעודי לטסט, שונה מ-8080, כדי שלא תהיה התנגשות עם שרת שרץ במקביל.
    private static final int TEST_PORT = 8082;

    private static HttpServer server;

    @BeforeAll
    static void startServer() throws Exception {
        // מזריקים Stores בזיכרון - הטסט בודק את ה-endpoint ולא את DynamoDB.
        server = Main.createServer(TEST_PORT, new InMemoryPeopleStore(), new InMemoryCheckInStore());

        // מפעילים את השרת - מכאן הוא מקשיב על TEST_PORT.
        server.start();
    }

    @AfterAll
    static void stopServer() {
        // עוצרים מיידית, כדי לשחרר את הפורט.
        server.stop(0);
    }

    @Test
    void createPersonReturns201Statuse() throws Exception {

        Gson gson = new Gson();
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", "Yossi");
        requestBody.addProperty("circleId", "0000");

        // שלב 1: לקוח HTTP
        HttpClient client = HttpClient.newHttpClient();

        // שלב 2: בקשת POST ל-/people עם גוף JSON
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/people"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .header("Content-Type", "application/json")
                .build();

        // שלב 3: שולחים וממתינים לתשובה (הגוף כטקסט)
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // שלב 4: בדיקות
        assertEquals(201, response.statusCode(), "קוד הסטטוס צריך להיות 201");

        // ממיר את התשובה ל-JSON
        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);

        assertEquals("Yossi", responseJson.get("name").getAsString());
        assertEquals("0000", responseJson.get("circleId").getAsString());

        assertNotNull(responseJson.get("id").getAsString());
        assertTrue(responseJson.get("id").getAsString().length() > 0);
    }

    @Test
    void getReturns405Status() throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        // GET ל-/people אסור (רק POST) - מצפים ל-405
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/people"))
                .GET()
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }
}
