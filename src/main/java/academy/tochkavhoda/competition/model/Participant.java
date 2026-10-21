package academy.tochkavhoda.competition.model;

/** Данные участника после регистрации не изменяются. */
public class Participant {
    private final String id;
    private final String firstName;
    private final String lastName;
    private final String companyName;
    private final String login;
    private final String passwordHash;

    public Participant(String id, String firstName, String lastName,
                       String companyName, String login, String passwordHash) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.companyName = companyName;
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
