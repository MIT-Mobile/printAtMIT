package edu.mit.printAtMIT.model.touchstoneOld;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

public class RequestError
{
    public static final int NO_ERROR = 0;
    public static final int REQUEST_CANCELED = 1;
    public static final int INVALID_JSON = 2;
    public static final int TOUCHSTONE_ERROR = 3;
    public static final int INVALID_CREDENTIALS = 4;
    public static final int UNKNOWN_ERROR = Integer.MAX_VALUE;
    
    private String _message;
    private int _code = RequestError.NO_ERROR;
    private HttpUriRequest _request;
    private HttpResponse _response;
    
    public RequestError(String message, int errorCode)
    {
        _message = message;
        _code = errorCode;
    }

    public int getCode() {
        return _code;
    }

    public String getMessage() {
        return _message;
    }

    public HttpUriRequest getRequest() {
        return _request;
    }

    public HttpResponse getResponse() {
        return _response;
    }

    public void setRequest(HttpUriRequest _request) {
        this._request = _request;
    }

    public void setResponse(HttpResponse _response) {
        this._response = _response;
    }
    
    
}
