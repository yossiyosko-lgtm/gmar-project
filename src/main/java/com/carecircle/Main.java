package com.carecircle;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.carecircle.model.Person;
import com.carecircle.model.CheckIn;
import com.carecircle.model.Alert;
import com.carecircle.store.PeopleStore;
import com.carecircle.store.CheckInStore;
import com.carecircle.store.AlertStore;
import com.carecircle.service.SqsService;
import com.carecircle.service.SnsService;
import com.carecircle.worker.CheckInWorker;
import com.carecircle.worker.SnsAlertSubscriber;
import com.google.gson.Gson;

public class Main {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = createServer(port);
        server.start();
        System.out.println("Care Circle server running on port " + port);
    }

    // השרת האמיתי - עובד מול DynamoDB
    static HttpServer createServer(int port) throws IOException {
        return createServer(port, new PeopleStore(), new CheckInStore());
    }

    // גרסה שמקבלת Stores מבחוץ - הטסטים מזריקים גרסת זיכרון
    static HttpServer createServer(int port, PeopleStore peopleStore, CheckInStore checkInStore) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        Gson gson = new Gson();
        AlertStore alertStore = new AlertStore();

        // כתובות SQS ו-SNS — LocalStack יוצר אותם בפורמט הזה
        String queueUrl = System.getenv().getOrDefault(
            "SQS_QUEUE_URL",
            "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/checkin-queue");
        String alertsQueueUrl = System.getenv().getOrDefault(
            "ALERTS_QUEUE_URL",
            "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/alerts-queue");
        String gentleTopicArn = System.getenv().getOrDefault(
            "GENTLE_TOPIC_ARN",
            "arn:aws:sns:us-east-1:000000000000:gentle-alerts");
        String urgentTopicArn = System.getenv().getOrDefault(
            "URGENT_TOPIC_ARN",
            "arn:aws:sns:us-east-1:000000000000:urgent-alerts");

        SqsService sqsService = new SqsService(queueUrl);
        SnsService snsService = new SnsService(gentleTopicArn, urgentTopicArn);

        // /health — בדיקת תקינות
        server.createContext("/health", exchange -> {
            String response = "{\"status\":\"ok\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        // POST /people — יצירת אדם חדש
        server.createContext("/people", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String error = "{\"error\":\"Method Not Allowed\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(405, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
                exchange.close();
                return;
            }
            InputStream inputStream = exchange.getRequestBody();
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            PersonRequest req = gson.fromJson(json, PersonRequest.class);
            Person person = new Person(req.name, req.circleId);
            peopleStore.save(person);
            String responseJson = gson.toJson(person);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
        });

        // GET /people/{id} — שליפת אדם לפי id
        server.createContext("/people/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String error = "{\"error\":\"Method Not Allowed\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(405, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
                exchange.close();
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String id = path.substring("/people/".length());
            Person person = peopleStore.findById(id);
            if (person == null) {
                String error = "{\"error\":\"Person not found\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(404, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
                exchange.close();
                return;
            }
            String responseJson = gson.toJson(person);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
        });

        // POST /checkins — שמירה + שליחה ל-SQS
        server.createContext("/checkins", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String error = "{\"error\":\"Method Not Allowed\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(405, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
                exchange.close();
                return;
            }
            InputStream inputStream = exchange.getRequestBody();
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            CheckInsRequest req = gson.fromJson(json, CheckInsRequest.class);
            CheckIn checkIn = new CheckIn(req.personId, req.type, req.value);
            checkInStore.save(checkIn);
            // שליחה ל-SQS לעיבוד אסינכרוני על ידי CheckInWorker
            try {
                sqsService.sendMessage(gson.toJson(checkIn));
            } catch (Exception e) {
                System.err.println("SQS send failed: " + e.getMessage());
            }
            String responseJson = gson.toJson(checkIn);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
        });

        // GET /alerts/{personId} — שליפת התראות לאדם
        server.createContext("/alerts/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String error = "{\"error\":\"Method Not Allowed\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(405, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
                exchange.close();
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String personId = path.substring("/alerts/".length());
            List<Alert> alerts = alertStore.findByPersonId(personId);
            String responseJson = gson.toJson(alerts);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
        });

        server.setExecutor(null);

        // הפעלת Workers כ-daemon threads (נעצרים אוטומטית עם השרת)
        try {
            CheckInWorker worker = new CheckInWorker(queueUrl, snsService);
            worker.setDaemon(true);
            worker.start();

            SnsAlertSubscriber alertSubscriber = new SnsAlertSubscriber(alertsQueueUrl, alertStore);
            alertSubscriber.setDaemon(true);
            alertSubscriber.start();
        } catch (Exception e) {
            System.err.println("Worker startup failed: " + e.getMessage());
        }

        return server;
    }

    private static class PersonRequest {
        String name;
        String circleId;
    }

    private static class CheckInsRequest {
        String personId;
        String type;
        double value;
    }
}