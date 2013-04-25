package edu.mit.printAtMIT.model.touchstone;

import java.util.UUID;

public class MobileTaskException extends Exception {
    public enum Code {
        NO_ERROR(0, "no error"),
        USER_CANCELED(1,"the request was canceled by the user"),
        REQUEST_CANCELED(2, "the request was canceled due to an error"),
        INVALID_JSON(3, "the response contained invalid JSON data"),
        TOUCHSTONE_ERROR(4, "there was an unknown error with the Touchstone service"),
        TOUCHSTONE_UNAVAILABLE(5, "the Touchstone service is currently disabled."),
        INVALID_CREDENTIALS(6, "the Touchstone username or password was invalid or missing"),
        UNKNOWN_ERROR(Integer.MAX_VALUE, "an unknown error has occurred and the operation cannot continue");

        public final int id;
        public final String message;

        private Code(int i, String msg) {
            this.id = i;
            this.message = msg;
        }
    }

    public final Code code;
    public final UUID requestId;

    public MobileTaskException(UUID id, String detailMessage) {
        super(detailMessage);
        this.initCause(null);
        this.code = MobileTaskException.Code.UNKNOWN_ERROR;
        this.requestId = id;
    }

    public MobileTaskException(UUID id, MobileTaskException.Code aCode) {
        super();
        this.initCause(null);
        this.code = aCode;
        this.requestId = id;
    }

    public MobileTaskException(UUID id, String detailMessage, MobileTaskException.Code aCode) {
        super(detailMessage);
        this.initCause(null);
        this.code = aCode;
        this.requestId = id;
    }

    public MobileTaskException(UUID id, String detailMessage, Throwable throwable, MobileTaskException.Code aCode) {
        super(detailMessage, throwable);
        this.code = aCode;
        this.requestId = id;
    }

    public MobileTaskException(UUID id, Throwable throwable, MobileTaskException.Code aCode) {
        super(throwable);
        this.code = aCode;
        this.requestId = id;
    }

    @Override
    public String getMessage() {
        String message;

        if (this.getCause() != null) {
            return this.getCause().getLocalizedMessage();
        } else {
            return this.code.message;
        }
    }
}
