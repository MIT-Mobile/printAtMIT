package edu.mit.printAtMIT.model.touchstone.internal;

public class ConnectionResult {
    public final String mimeType;
    public final String contentBody;
    public final int responseCode;
    public final String responseMessage;

    public ConnectionResult(String mimeType, String contentBody, int responseCode, String responseMessage) {
        this.mimeType = mimeType;
        this.contentBody = contentBody;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }
}