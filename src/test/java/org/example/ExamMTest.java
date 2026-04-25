package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExamMTest {

        private static final String BASE_URL = "http://10.82.196.214:8085";
        private static final String API_KEY = "bookstore-2026-secret";

        private static int testBookId;
        private static String testBookIsbn;

        @BeforeAll
        public static void setup() {
                RestAssured.baseURI = BASE_URL;
        }

        // Сценарий 1: Полная проверка книги
        @Test
        @Order(1)
        public void testFullBook() {
                //1. Создать новую книгу
                String isbn = "978-" + UUID.randomUUID().toString().substring(0, 8);
                String createBody = new JSONObject()
                                .put("isbn", isbn)
                                .put("title", "Евгений Онегин")
                                .put("author", "А. Пушкин")
                                .put("genre", "Classic")
                                .put("year", 1990)
                                .put("price", 499)
                                .put("stock", 8)
                                .put("pages", 400)
                                .toString();

                Response createResponse = given()
                                .header("X-API-Key", API_KEY)
                                .contentType(ContentType.JSON)
                                .body(createBody)
                                .when()
                                .post("/books")
                                .then()
                                .statusCode(201)
                                .body("isbn", equalTo(isbn))
                                .extract().response();

                int bookId = createResponse.path("id");

                // 2. Получить книгу по ID
                given()
                                .when()
                                .get("/books/" + bookId)
                                .then()
                                .statusCode(200)
                                .body("isbn", equalTo(isbn));

                // 3. Обновить цену
                String patchBody = "{\"price\": 699}";
                given()
                                .header("X-API-Key", API_KEY)
                                .contentType(ContentType.JSON)
                                .body(patchBody)
                                .when()
                                .patch("/books/" + bookId)
                                .then()
                                .statusCode(200)
                                .body("price", equalTo(699));

                // 4. Проверить наличие
                given()
                                .when()
                                .get("/books/" + bookId + "/stock")
                                .then()
                                .statusCode(200)
                                .body("stock", equalTo(8));

                // 5. Удалить книгу
                given()
                                .header("X-API-Key", API_KEY)
                                .when()
                                .delete("/books/" + bookId)
                                .then()
                                .statusCode(204);
        }
        // создание книг для остальных тестов
        @Test
        @Order(2)
        public void TestBooks() {
                testBookIsbn = "978-" + UUID.randomUUID().toString().substring(0, 8);
                String body = new JSONObject()
                                .put("isbn", testBookIsbn)
                                .put("title", "TestBook")
                                .put("author", "Толстой")
                                .put("price", 999)
                                .put("stock", 10)
                                .toString();

                testBookId = given()
                                .header("X-API-Key", API_KEY)
                                .contentType(ContentType.JSON)
                                .body(body)
                                .when()
                                .post("/books")
                                .then()
                                .statusCode(201)
                                .extract().path("id");
        }
        // Сценарий 2: Покупка (ну почти) и отзыв
        @Test
        @Order(3)
        public void testStockAndReview() {
                // 1. Проверить наличие книги
                given()
                                .when()
                                .get("/books/" + testBookId + "/stock")
                                .then()
                                .statusCode(200)
                                .body("available", equalTo(true));

                // 2. Добавить отзыв на книгу
                String reviewBody = new JSONObject()
                                .put("rating", 5)
                                .put("comment", "Excellent book! Must read!")
                                .put("reviewerName", "Islam")
                                .toString();

                given()
                                .contentType(ContentType.JSON)
                                .body(reviewBody)
                                .when()
                                .post("/books/" + testBookId + "/reviews")
                                .then()
                                .statusCode(201)
                                .body("rating", equalTo(5));
        }
        // Сценарий 3: Фильтрация и пагинация
        @Test
        @Order(4)
        public void Filter() {
                // 1. Получить книги с фильтром по жанру
                given()
                                .queryParam("genre", "Classic")
                                .when()
                                .get("/books")
                                .then()
                                .statusCode(200);

                // 2. Получить книги с пагинацией
                given()
                                .queryParam("page", 0)
                                .queryParam("size", 2)
                                .when()
                                .get("/books")
                                .then()
                                .statusCode(200)
                                .body("size", equalTo(2));

                // 3. Получить книги с фильтром по цене
                given()
                                .queryParam("minPrice", 99)
                                .queryParam("maxPrice", 777)
                                .when()
                                .get("/books")
                                .then()
                                .statusCode(200);
        }
        // Тесты на ошибки
        @Test
        @Order(5)
        public void createBookInvalid() {
                String body = new JSONObject()
                                .put("isbn", "978-missing")
                                .put("title", "New-Book")
                                .toString();

                given()
                                .header("X-API-Key", API_KEY)
                                .contentType(ContentType.JSON)
                                .body(body)
                                .when()
                                .post("/books")
                                .then()
                                .statusCode(400)
                                .body("error", containsString("Missing required fields:author, price are mandatory"));
        }

        @Test
        @Order(6)
        public void authRequired() {
                given()
                                .when()
                                .delete("/books/" + testBookId)
                                .then()
                                .statusCode(401);
        }

        @Test
        @Order(7)
        public void duplicateIsbn() {
                String body = new JSONObject()
                                .put("isbn", testBookIsbn)
                                .put("title", "Gogol")
                                .put("author", "cat")
                                .put("price", 1111)
                                .toString();

                given()
                                .header("X-API-Key", API_KEY)
                                .contentType(ContentType.JSON)
                                .body(body)
                                .when()
                                .post("/books")
                                .then()
                                .statusCode(409)
                                .body("error", containsString("Book with this ISBN already exists"));
        }
}
