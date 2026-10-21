package academy.tochkavhoda.competition;

import academy.tochkavhoda.competition.dto.request.RegisterParticipantDtoRequest;
import academy.tochkavhoda.competition.dto.response.ErrorDtoResponse;
import academy.tochkavhoda.competition.server.Server;
import academy.tochkavhoda.competition.server.ServerResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Тест выступает клиентом: обращается только к Server и DTO. */
public class TestParticipantRegistration {
    private static final List<String> FIELDS =
            List.of("firstName", "lastName", "companyName", "login", "password");

    private final Gson gson = new Gson();
    private Server server;
    private JsonObject request;

    @BeforeEach
    void setUp() {
        server = new Server();
        // Database общая для всех Server. У каждого теста свои пользователи.
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        RegisterParticipantDtoRequest dto = new RegisterParticipantDtoRequest(
                "Иван", "Петров", "Фирма " + suffix, "user_" + suffix, "password123");
        request = gson.toJsonTree(dto).getAsJsonObject();
    }

    @Test
    void registersParticipant() {
        assertSuccess(register(request));
    }

    @Test
    void registersDifferentParticipants() {
        assertSuccess(register(request));
        request.addProperty("login", "another_" + uniqueSuffix());
        request.addProperty("companyName", "Другая фирма " + uniqueSuffix());
        assertSuccess(register(request));
    }

    @Test
    void rejectsDuplicateLoginWithoutReservingNewCompany() {
        assertSuccess(register(request));
        request.addProperty("companyName", "Новая фирма " + uniqueSuffix());

        assertEquals("Этот логин уже используется", assertError(register(request)));

        request.addProperty("login", "next_" + uniqueSuffix());
        assertSuccess(register(request));
    }

    @Test
    void rejectsDuplicateCompanyWithoutReservingNewLogin() {
        assertSuccess(register(request));
        request.addProperty("login", "next_" + uniqueSuffix());

        assertEquals("Эта фирма уже зарегистрирована", assertError(register(request)));

        request.addProperty("companyName", "Новая фирма " + uniqueSuffix());
        assertSuccess(register(request));
    }

    @Test
    void loginIsCaseInsensitiveAndIgnoresSurroundingSpaces() {
        assertSuccess(register(request));
        request.addProperty("login", "  " + request.get("login").getAsString()
                .toUpperCase(Locale.ROOT) + "  ");
        request.addProperty("companyName", "Другая фирма " + uniqueSuffix());

        assertEquals("Этот логин уже используется", assertError(register(request)));
    }

    @Test
    void companyUniquenessIgnoresCaseAndExtraWhitespace() {
        request.addProperty("companyName", "Фирма Старт " + uniqueSuffix());
        assertSuccess(register(request));
        request.addProperty("login", "next_" + uniqueSuffix());
        request.addProperty("companyName", "  " + request.get("companyName").getAsString()
                .toUpperCase(Locale.ROOT).replace(" ", " \t ") + "  ");

        assertEquals("Эта фирма уже зарегистрирована", assertError(register(request)));
    }

    @Test
    void serverInstancesShareRegisteredParticipants() {
        assertSuccess(register(request));
        Server anotherServer = new Server();

        assertEquals("Этот логин уже используется",
                assertError(anotherServer.registerParticipant(gson.toJson(request))));
    }

    @ParameterizedTest(name = "Обязательное поле: {0}, вариант: {1}")
    @MethodSource("missingOrBlankFields")
    void rejectsMissingOrBlankField(String field, String variant) {
        switch (variant) {
            case "missing" -> request.remove(field);
            case "null" -> request.add(field, JsonNull.INSTANCE);
            case "empty" -> request.addProperty(field, "");
            case "blank" -> request.addProperty(field, " \t\n ");
            default -> throw new IllegalArgumentException(variant);
        }
        assertError(register(request));
    }

    static Stream<Arguments> missingOrBlankFields() {
        return FIELDS.stream().flatMap(field -> Stream.of("missing", "null", "empty", "blank")
                .map(variant -> Arguments.of(field, variant)));
    }

    @ParameterizedTest(name = "Неверный тип поля {0}: {1}")
    @MethodSource("nonStringFields")
    void rejectsNonStringField(String field, String value) {
        request.add(field, JsonParser.parseString(value));

        assertEquals("Поле " + field + " должно быть строкой", assertError(register(request)));
    }

    static Stream<Arguments> nonStringFields() {
        return FIELDS.stream().flatMap(field -> Stream.of("12345678", "true", "{}", "[]")
                .map(value -> Arguments.of(field, value)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "null", "[]", "[{}]", "123", "true", "\"text\"",
            "{", "{unquoted: 1}", "{'login': 'ivan'}", "{} {}", "{} trailing",
            "/* comment */ {}", "{\"login\":\"ivan\",}"})
    void rejectsMalformedJsonOrNonObject(String json) {
        assertError(server.registerParticipant(json));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "a b", "логин", "user/name", "x" + "123456789012345678901234567890"})
    void rejectsInvalidLogin(String login) {
        request.addProperty("login", login);
        assertTrue(assertError(register(request)).startsWith("Логин должен"));
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 129})
    void rejectsPasswordOutsideLengthLimits(int length) {
        request.addProperty("password", "x".repeat(length));
        assertTrue(assertError(register(request)).startsWith("Пароль должен"));
    }

    @Test
    void rejectsPasswordConsistingOnlyOfSpaces() {
        request.addProperty("password", " ".repeat(8));
        assertTrue(assertError(register(request)).startsWith("Пароль должен"));
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 128})
    void acceptsPasswordAtLengthLimits(int length) {
        request.addProperty("password", "x".repeat(length));
        assertSuccess(register(request));
    }

    @Test
    void acceptsLoginAtMinimumLength() {
        // Короткий фиксированный логин используется только в этом тесте.
        request.addProperty("login", "a.b");
        assertSuccess(register(request));
    }

    @Test
    void acceptsLoginAtMaximumLength() {
        request.addProperty("login", "u".repeat(14) + uniqueSuffix());
        assertSuccess(register(request));
    }

    @ParameterizedTest
    @MethodSource("textLengthLimits")
    void checksTextLengthLimits(String field, int maxLength) {
        JsonObject invalidRequest = request.deepCopy();
        invalidRequest.addProperty(field, "я".repeat(maxLength + 1));
        assertTrue(assertError(register(invalidRequest)).contains("максимум " + maxLength));

        request.addProperty(field, "я".repeat(maxLength));
        assertSuccess(register(request));
    }

    static Stream<Arguments> textLengthLimits() {
        return Stream.of(Arguments.of("firstName", 100), Arguments.of("lastName", 100),
                Arguments.of("companyName", 200));
    }

    @Test
    void invalidRequestDoesNotReserveLoginOrCompany() {
        JsonObject invalidRequest = request.deepCopy();
        invalidRequest.addProperty("password", "short");
        assertError(register(invalidRequest));

        assertSuccess(register(request));
    }

    @Test
    void acceptsUnknownFields() {
        request.addProperty("extraField", "ignored");
        assertSuccess(register(request));
    }

    private ServerResponse register(JsonObject json) {
        return server.registerParticipant(gson.toJson(json));
    }

    private void assertSuccess(ServerResponse response) {
        assertEquals(200, response.getResponseCode(), response.getResponseData());
        assertEquals(new JsonObject(), JsonParser.parseString(response.getResponseData()));
    }

    private String assertError(ServerResponse response) {
        assertEquals(400, response.getResponseCode(), response.getResponseData());
        JsonElement data = JsonParser.parseString(response.getResponseData());
        assertTrue(data.isJsonObject());
        assertEquals(1, data.getAsJsonObject().size());
        assertTrue(data.getAsJsonObject().has("error"));
        ErrorDtoResponse error = gson.fromJson(data, ErrorDtoResponse.class);
        assertNotNull(error.getError());
        assertFalse(error.getError().isBlank());
        return error.getError();
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
