package com.carecircle;

// HttpServer - מחלקה מובנית ב-Java (בלי framework חיצוני) שמאפשרת להקים שרת HTTP פשוט.
// זה מה שאפשר לנו לכתוב "plain Java" בלי Spring Boot, בדיוק כמו בקורס.
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress; // מייצג כתובת+פורט שהשרת יאזין להם
import com.carecircle.model.Person;
import com.carecircle.model.CheckIn;
import com.carecircle.store.PeopleStore;
import com.carecircle.store.CheckInStore;
import com.google.gson.Gson;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.carecircle.service.SqsService;

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
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // בונים את השרת דרך הפונקציה המשותפת (ראה למטה) - כך גם main וגם הטסטים
        // משתמשים באותו קוד בדיוק, ולא כותבים אותו פעמיים.
        HttpServer server = createServer(port);

        // מפעילים את השרת בפועל - מהרגע הזה הוא מקשיב לבקשות.
        server.start();

        System.out.println("Care Circle server running on port " + port);
    }

    /**
     * בונה HttpServer מוכן עם כל ה-endpoints מוגדרים, אבל עדיין לא מריץ אותו (לא קוראים start()).
     * זה מאפשר לכל צד שקורא לפונקציה הזו (main, או טסטים) להחליט בעצמו מתי להריץ ומתי לעצור.
     *
     * static חשוב כאן: אין לנו אובייקט Main שנוצר (לא כתבנו new Main()), אז הפונקציה חייבת
     * להיות static כדי שנוכל לקרוא לה ישירות דרך שם המחלקה: Main.createServer(...)
     */
    static final String queueUrl = "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/checkin-queue";

    
    static HttpServer createServer(int port) throws IOException {

        // יוצרים את השרת עצמו, ואומרים לו על איזו כתובת/פורט להאזין.
        // ה-0 השני הוא "backlog" - כמה בקשות ממתינות מותר לצבור בתור לפני שדוחים; 0 = ברירת המחדל של המערכת.
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        Gson gson = new Gson();

        //המשתנה שיצרנו שמכיל את כל הפרופילים
        PeopleStore peopleStore = new PeopleStore();
        
        //המשתנה שמכיל את כל המידע על המשתמשים (שעת קימה זיכרון והכל)
        CheckInStore checkInStore = new CheckInStore();

        SqsService sqsService = new SqsService(queueUrl);
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
        
        // יצירת בקשה מסוג people
        server.createContext("/people", exchange -> {
            if (!"post".equalsIgnoreCase(exchange.getRequestMethod())){
                String response = "{\"error\":\"post method require\"}"; 
                
            // מודיעים לדפדפן/ל-curl שהתוכן שאנחנו מחזירים הוא JSON.
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // שולחים את קוד הסטטוס 405 כישלון יחד עם אורך התשובה בבתים.
            exchange.sendResponseHeaders(405, response.getBytes().length);

            // כותבים בפועל את גוף התשובה, וסוגרים את החיבור.
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
            
            return;
        }
            
        InputStream inputStream = exchange.getRequestBody();
        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        PersonRequest req = gson.fromJson(json, PersonRequest.class);
        Person person = new Person(req.name, req.circleId);
        peopleStore.save(person);
        String responseJson = gson.toJson(person);

            // מודיעים לדפדפן/ל-curl שהתוכן שאנחנו מחזירים הוא JSON.
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // שולחים את קוד הסטטוס 200 (הצלחה) יחד עם אורך התשובה בבתים.
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);

            // כותבים בפועל את גוף התשובה, וסוגרים את החיבור.
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
        });

        //יצירת האזנה לבקשת קיבול נתונים על משתמש אחרי שנוצר, על ידי שליחת המזהה שלו
        server.createContext("/people/", exchange -> {
           
            if(!"Get".equalsIgnoreCase(exchange.getRequestMethod())){
                String response = "{\"error\":\"get method require\"}";
            
             // מודיעים לדפדפן/ל-curl שהתוכן שאנחנו מחזירים הוא JSON.
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // שולחים את קוד הסטטוס 405 כישלון יחד עם אורך התשובה בבתים.
            exchange.sendResponseHeaders(405, response.getBytes().length);

            // כותבים בפועל את גוף התשובה, וסוגרים את החיבור.
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
            
            return;
            }
            String path = exchange.getRequestURI().getPath();
            String id = path.substring("/people/".length());

            Person person = peopleStore.findById(id);

            if(person == null){
                String response = "{\"error\":\"id require\"}";
            
            // מודיעים לדפדפן/ל-curl שהתוכן שאנחנו מחזירים הוא JSON.
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // שולחים את קוד הסטטוס 404 כישלון יחד עם אורך התשובה בבתים.
            exchange.sendResponseHeaders(404, response.getBytes().length);

            // כותבים בפועל את גוף התשובה, וסוגרים את החיבור.
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
            
            return;
          
        }else{
            
            String responseJson = gson.toJson(person);

            // מודיעים לדפדפן/ל-curl שהתוכן שאנחנו מחזירים הוא JSON.
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // שולחים את קוד הסטטוס 200 (הצלחה) יחד עם אורך התשובה בבתים.
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);

            // כותבים בפועל את גוף התשובה, וסוגרים את החיבור.
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();

            }
        });

        server.createContext("/checkins", exchange ->{
             if (!"post".equalsIgnoreCase(exchange.getRequestMethod())){
                String response = "{\"error\":\"post method require\"}"; 
                
            // מודיעים לדפדפן/ל-curl שהתוכן שאנחנו מחזירים הוא JSON.
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // שולחים את קוד הסטטוס 405 כישלון יחד עם אורך התשובה בבתים.
            exchange.sendResponseHeaders(405, response.getBytes().length);

            // כותבים בפועל את גוף התשובה, וסוגרים את החיבור.
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
            
            return;
        }
            
        InputStream inputStream = exchange.getRequestBody();
        String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        CheckInsRequest req = gson.fromJson(json, CheckInsRequest.class);
        CheckIn checkIn = new CheckIn(req.personId, req.type, req.value);
        checkInStore.save(checkIn);
        sqsService.sendMessage(gson.toJson(checkIn));
        String responseJson = gson.toJson(checkIn);

            // מודיעים לדפדפן/ל-curl שהתוכן שאנחנו מחזירים הוא JSON.
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // שולחים את קוד הסטטוס 201 (הצלחה) יחד עם אורך התשובה בבתים.
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);

            // כותבים בפועל את גוף התשובה, וסוגרים את החיבור.
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
        });


        // null אומר לשרת להשתמש ב-executor המובנה שלו (thread לכל בקשה) - מספיק לצרכי הפרויקט הזה.
        server.setExecutor(null);

        return server;
    }

    private static class PersonRequest{
        String name;
        String circleId;
    }

    private static class CheckInsRequest{
        String personId;
        String type;
        double value;
    }
}
