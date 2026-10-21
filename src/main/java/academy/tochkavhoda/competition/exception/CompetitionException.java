package academy.tochkavhoda.competition.exception;

public class CompetitionException extends RuntimeException {
    private final ErrorCode errorCode;

    public CompetitionException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
