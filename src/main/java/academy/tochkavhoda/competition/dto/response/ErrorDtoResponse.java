package academy.tochkavhoda.competition.dto.response;

public class ErrorDtoResponse {
    private final String error;

    public ErrorDtoResponse(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
