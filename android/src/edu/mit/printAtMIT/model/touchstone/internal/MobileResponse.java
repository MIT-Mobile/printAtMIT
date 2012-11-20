package edu.mit.printAtMIT.model.touchstone.internal;

import android.net.Uri;
import java.net.HttpURLConnection;
import org.apache.http.HttpResponse;

public class MobileResponse {

    public final HttpResponse httpResponse;
    public final String contentBody;
    public Uri targetUri; 

    public MobileResponse(HttpResponse response, String body)
    {
        this.httpResponse = response;
        this.contentBody = body;
    }

    
    public boolean isRedirect()
    {
        int statusCode = this.httpResponse.getStatusLine().getStatusCode();
        
        return ((statusCode == HttpURLConnection.HTTP_MULT_CHOICE) ||
                (statusCode == HttpURLConnection.HTTP_MOVED_PERM) ||
                (statusCode == HttpURLConnection.HTTP_MOVED_TEMP));
    }
}