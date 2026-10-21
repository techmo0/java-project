package academy.tochkavhoda.competition.database;

import academy.tochkavhoda.competition.exception.CompetitionException;
import academy.tochkavhoda.competition.exception.ErrorCode;
import academy.tochkavhoda.competition.model.Participant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Единственное хранилище приложения. Данные живут до завершения JVM. */
public final class Database {
    private static final Database INSTANCE = new Database();

    private final Map<String, Participant> participantsByLogin = new HashMap<>();
    private final Set<String> registeredCompanies = new HashSet<>();

    private Database() {
    }

    public static Database getInstance() {
        return INSTANCE;
    }

    public synchronized void insertParticipant(Participant participant) {
        String loginKey = participant.getLogin().toLowerCase(Locale.ROOT);
        String companyKey = participant.getCompanyName()
                .strip().replaceAll("(?U)\\s+", " ").toLowerCase(Locale.ROOT);

        // Обе проверки выполняются до записи: ошибка не оставляет часть данных.
        if (participantsByLogin.containsKey(loginKey)) {
            throw new CompetitionException(ErrorCode.LOGIN_ALREADY_EXISTS,
                    "Этот логин уже используется");
        }
        if (registeredCompanies.contains(companyKey)) {
            throw new CompetitionException(ErrorCode.PARTICIPANT_ALREADY_EXISTS,
                    "Эта фирма уже зарегистрирована");
        }

        participantsByLogin.put(loginKey, participant);
        registeredCompanies.add(companyKey);
    }
}
