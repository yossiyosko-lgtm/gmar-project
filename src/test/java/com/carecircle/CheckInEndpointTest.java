package com.carecircle;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.carecircle.store.InMemoryPeopleStore;
import com.carecircle.store.InMemoryCheckInStore;


public class CheckInEndpointTest {
    // פורט ייעודי לטסט, שונה מ-8080 ומשאר הטסטים.
    private static final int TEST_PORT = 8083;

    private static HttpServer server;

    @BeforeAll
    static void startServer() throws Exception {
        // Stores בזיכרון - אין תלות ב-LocalStack
        server = Main.createServer(TEST_PORT, new InMemoryPeopleStore(), new InMemoryCheckInStore());
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void createCheckInReturns201WithGeneratedId() throws Exception {

        Gson gson = new Gson();
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("personId", "0000");
        requestBody.addProperty("type", "wake_time");
        requestBody.addProperty("value", 7.25);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/checkins"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "קוד הסטטוס צריך להיות 201");

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);

        assertEquals("0000", responseJson.get("personId").getAsString());
        assertEquals("wake_time", responseJson.get("type").getAsString());
        assertEquals(7.25, responseJson.get("value").getAsDouble());

        assertNotNull(responseJson.get("id").getAsString());
        assertTrue(responseJson.get("id").getAsString().length() > 0);
    }

    @Test
    void getReturns405Status() throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/checkins"))
                .GET()
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }
}