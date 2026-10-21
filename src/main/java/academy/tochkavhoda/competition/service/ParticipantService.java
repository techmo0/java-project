package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.ParticipantDao;
import academy.tochkavhoda.competition.daoimpl.ParticipantDaoImpl;
import academy.tochkavhoda.competition.dto.request.RegisterParticipantDtoRequest;
import academy.tochkavhoda.competition.dto.response.EmptyDtoResponse;
import academy.tochkavhoda.competition.dto.response.ErrorDtoResponse;
import academy.tochkavhoda.competition.exception.CompetitionException;
import academy.tochkavhoda.competition.exception.ErrorCode;
import academy.tochkavhoda.competition.model.Participant;
import academy.tochkavhoda.competition.server.ServerResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ParticipantService {
    private final Gson gson = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private final ParticipantDao participantDao = new ParticipantDaoImpl();

    public ServerResponse registerParticipant(String requestJsonString) {
        try {
            RegisterParticipantDtoRequest request = parseRequest(requestJsonString);
            validate(request);

            Participant participant = new Participant(
                    UUID.randomUUID().toString(),
                    request.getFirstName().strip(),
                    request.getLastName().strip(),
                    request.getCompanyName().strip().replaceAll("(?U)\\s+", " "),
                    request.getLogin().strip().toLowerCase(Locale.ROOT),
                    PasswordHasher.hash(request.getPassword()));
            participantDao.insert(participant);

            return new ServerResponse(200, gson.toJson(new EmptyDtoResponse()));
        } catch (JsonParseException e) {
            return error("Запрос должен быть корректным JSON-объектом");
        } catch (CompetitionException e) {
            return error(e.getMessage());
        }
    }

    private RegisterParticipantDtoRequest parseRequest(String json) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        if (object == null) {
            throw invalid("Запрос должен быть непустым JSON-объектом");
        }
        // Gson умеет преобразовывать числа в String, но наш протокол требует строки.
        for (String field : List.of("firstName", "lastName", "companyName", "login", "password")) {
            JsonElement value = object.get(field);
            if (value != null && !value.isJsonNull()
                    && (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())) {
                throw invalid("Поле " + field + " должно быть строкой");
            }
        }
        return gson.fromJson(object, RegisterParticipantDtoRequest.class);
    }

    private static void validate(RegisterParticipantDtoRequest request) {
        validateText(request.getFirstName(), "Имя руководителя", 100);
        validateText(request.getLastName(), "Фамилия руководителя", 100);
        validateText(request.getCompanyName(), "Название фирмы", 200);

        String login = request.getLogin();
        if (login == null || !login.strip().matches("[A-Za-z0-9_.-]{3,30}")) {
            throw invalid("Логин должен содержать от 3 до 30 символов: латинские буквы, цифры, _, . или -");
        }
        String password = request.getPassword();
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 128) {
            throw invalid("Пароль должен содержать от 8 до 128 символов и не состоять только из пробелов");
        }
    }

    private static void validateText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw invalid(name + ": обязательное поле");
        }
        if (value.strip().length() > maxLength) {
            throw invalid(name + ": максимум " + maxLength + " символов");
        }
    }

    private static CompetitionException invalid(String message) {
        return new CompetitionException(ErrorCode.INVALID_REQUEST, message);
    }

    private ServerResponse error(String message) {
        return new ServerResponse(400, gson.toJson(new ErrorDtoResponse(message)));
    }
}
