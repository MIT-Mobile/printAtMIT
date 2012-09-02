/*
 * Class that performs the steps for Touchstone authentication
 */
package edu.mit.printAtMIT.model.touchstone;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import edu.mit.printAtMIT.controller.client.PrinterClientException;
import edu.mit.printAtMIT.model.touchstone.authn.AuthenticationResult;
import edu.mit.printAtMIT.model.touchstone.authn.IAuthenticationCallback;
import edu.mit.printAtMIT.model.touchstone.authn.IMobileAuthenticationHandler;
import edu.mit.printAtMIT.model.touchstone.internal.ChainedSSLSocketFactory;
import edu.mit.printAtMIT.model.touchstone.internal.MobileRequest;
import edu.mit.printAtMIT.model.touchstone.internal.MobileRequestState;
import edu.mit.printAtMIT.model.touchstone.internal.MobileResponse;
import edu.mit.printAtMIT.model.touchstone.internal.TouchstoneResponse;

public class MobileAPI extends AsyncTask<IMobileAuthenticationHandler, Void, Void> {

    private static final int REQUEST_IDLE = 0;
    private static final int REQUEST_OK = 1;
    private static final int REQUEST_AUTHENTICATE = 2;
    private static final int REQUEST_OK_AUTH = 3;
    private static final int REQUEST_ERROR = 4;
    private static ClientConnectionManager _sharedConnectionManager;
    private static SSLSocketFactory _sharedSocketFactory;
    private static CookieStore _sharedCookieStore;
    private HttpClient _httpClient;
    private String _requestBaseURL;
    private String _module;
    private String _command;
    private Map<String, String> _parameters;
    private MobileResponseHandler _responseHandler;
    private boolean _usesPOST;
    private IMobileAuthenticationHandler _authenticationHandler;
    private String _responseData;
    private RequestError _error;
    private boolean _isFinished;
    private MobileRequest _originalRequest;
    private MobileRequestState _activeRequest;
    private MobileRequestState _pendingRequest;
    private Uri _lastRedirectedUri;
    private String _touchstoneUsername;
    private String _touchstonePassword;
    private static final String TAG = "MobileAPI";
    static {
        _sharedCookieStore = new BasicCookieStore();
        
        initConnectionManager(null);
    }
    
    private static String getDefaultUserAgent()
    {
        return "MIT Mobile Touchstone Test/1.0.0 (2.3.1-57-g657cca4;) Android/4.0.3 (armeabi-v7a; Motorola Xoom;)";
    }

    private static void initConnectionManager(KeyStore trustStore) {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

        SSLSocketFactory sslFactory = null;
        try {
            sslFactory = new ChainedSSLSocketFactory(trustStore);
        } catch (NoSuchAlgorithmException ex) {
            Log.e("MAPI:ConnMan[s]", "Unsupported algorithm", ex);
        } catch (KeyStoreException ex) {
            Log.e("MAPI:ConnMan[s]", "Invalid key store", ex);
        } catch (KeyManagementException ex) {
            Log.e("MAPI:ConnMan[s]", "Key management error", ex);
        } catch (UnrecoverableKeyException ex) {
            Log.e("MAPI:ConnMan[s]", "Unknown key error", ex);
        }

        if (sslFactory == null)
        {
            sslFactory = SSLSocketFactory.getSocketFactory();
        }
        
        registry.register(new Scheme("https", sslFactory, 443));
        _sharedSocketFactory = sslFactory;


        HttpParams managerParams = new BasicHttpParams();

        // Timeout after 10 seconds (10000 ms)
        HttpConnectionParams.setConnectionTimeout(managerParams, 10000);
        _sharedConnectionManager = new ThreadSafeClientConnManager(managerParams, registry);
    }

    public static void setAlternativeTrustStore(KeyStore truststore) {
        if (truststore != null) {
            initConnectionManager(truststore);
        }
    }

    public MobileAPI(String module, String command, Map<String, String> parameters, MobileResponseHandler handler) {
        assert ((module != null) && (module.length() > 0));
        _module = module;
        _command = command;
        _parameters = (parameters == null) ? new HashMap<String, String>() : parameters;
        _responseHandler = handler;

        this.setUsesPOST(false);

        Uri.Builder uriBuilder = Uri.parse(this.getBaseServer()).buildUpon();

        uriBuilder.appendQueryParameter("module", module);

        if ((command != null) && (command.length() > 0)) {
            uriBuilder.appendQueryParameter("command", command);
        }

        _requestBaseURL = uriBuilder.build().toString();
    }

    public MobileAPI(String relativePath, Map<String, String> parameters, MobileResponseHandler handler) {
        _module = null;
        _command = null;
        _parameters = (parameters == null) ? new HashMap<String, String>() : parameters;
        _responseHandler = handler;

        this.setUsesPOST(false);

        Uri.Builder uriBuilder = Uri.parse(this.getBaseServer()).buildUpon();
        uriBuilder.appendPath(relativePath);
        _requestBaseURL = uriBuilder.build().toString();
    }

    public MobileAPI(Uri requestURL, Map<String, String> parameters, MobileResponseHandler handler) {
        _module = null;
        _command = null;
        _parameters = (parameters == null) ? new HashMap<String, String>() : parameters;
        _responseHandler = handler;
        _requestBaseURL = requestURL.toString();
        this.setUsesPOST(false);

    }

    private String getBaseServer() {
        return "http://mobile-dev.mit.edu/api/";
    }

    @Override
    protected Void doInBackground(IMobileAuthenticationHandler... arg0) {
        assert ((arg0 != null) && (arg0.length == 1));

        this.initTask(_responseHandler);
        _authenticationHandler = (IMobileAuthenticationHandler) (arg0[0]);

        do {
            if (_pendingRequest == null) {
                Thread.yield();
            } else {
                try {
                    _activeRequest = _pendingRequest;
                    _pendingRequest = null;

                    MobileResponse response = this.executeRequest(_activeRequest.request);

                    //If everything worked right, we should be halted at a 302
                    //  redirect back to the original domain here. If that is the
                    //  case, create a new request
                    if (_activeRequest.state == REQUEST_OK_AUTH) {
                    }

                    switch (_activeRequest.state) {
                    /*
                     * Steps for authentication. Loop until complete all of them.
                     */
                        case REQUEST_IDLE:
                            break;
                        case REQUEST_OK: {
                            this.handleOKResponse(_activeRequest.request, response);
                            break;
                        }
                        case REQUEST_AUTHENTICATE: {
                            this.handleAuthenticateResponse(_activeRequest.request, response);
                            break;
                        }
                        case REQUEST_OK_AUTH: {
                            this.enqueueRequest(REQUEST_OK, _originalRequest);
                        }
                        break;
                        case REQUEST_ERROR:
                            break;
                    }
                } catch (Throwable ex) {
                    // Catch any uncaught exceptions!
                    Log.e(this.getClass().getName(), ex.getMessage()
                            + "\n----\n"
                            + Log.getStackTraceString(ex));
                    this.cancelRequest();
                }

                _lastRedirectedUri = null;
            }
        } while (!(this.isCancelled() || _isFinished));
        return null;
    }

    @Override
    protected void onCancelled() {
        this.finishRequest();

        if (_responseHandler != null) {
            _responseHandler.onCanceled();
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        String responseString = _responseData;
        this.finishRequest();

        if (_responseHandler != null) {
            if (_error == null) {
                try {
					_responseHandler.onRequestCompleted(responseString, this._httpClient);
				} catch (PrinterClientException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } else {
                _responseHandler.onError(_error.getCode(), _error.getMessage());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MobileAPI other = (MobileAPI) obj;
        if ((this._module == null) ? (other._module != null) : !this._module.equals(other._module)) {
            return false;
        }
        if ((this._command == null) ? (other._command != null) : !this._command.equals(other._command)) {
            return false;
        }
        if (this._parameters != other._parameters && (this._parameters == null
                || !this._parameters.equals(other._parameters))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this._module != null ? this._module.hashCode() : 0);
        hash = 97 * hash + (this._command != null ? this._command.hashCode() : 0);
        hash = 97 * hash + (this._parameters != null ? this._parameters.hashCode() : 0);
        return hash;
    }

    public String getModule() {
        return _module;
    }

    public String getCommand() {
        return _command;
    }

    public Map<String, String> getParameters() {
        return new HashMap<String, String>(_parameters);
    }

    /*
     * Form initial request
     */
    private MobileRequest createRequest() {
        MobileRequest request = null;

        // Intentionally doing a shallow copy here, just in case
        // _parameters is changed on us while we are in the middle
        // of a request
        Map<String, String> parameters = new HashMap<String, String>(_parameters);

        if (this.getUsesPOST()) {
            request = new MobileRequest(Uri.parse(_requestBaseURL), MobileRequest.VERB_POST, parameters);
        } else {
            Uri.Builder builder = Uri.parse(this._requestBaseURL).buildUpon();

            for (Map.Entry<String, String> param : parameters.entrySet()) {
                builder.appendQueryParameter(param.getKey(), param.getValue());
            }

            request = new MobileRequest(builder.build(), MobileRequest.VERB_GET, null);
        }

        return request;
    }

    public RequestError getError() {
        return _error;
    }

    public final void setUsesPOST(boolean usePOST) {
        _usesPOST = usePOST;
    }

    public boolean getUsesPOST() {
        return _usesPOST;
    }

    public IMobileAuthenticationHandler getAuthenticationHandler() {
        return _authenticationHandler;
    }

    public void setAuthenticationHandler(IMobileAuthenticationHandler authenticationHandler) {
        this._authenticationHandler = authenticationHandler;
    }

    synchronized private void setPendingRequest(MobileRequestState newRequest) {
        _pendingRequest = newRequest;
    }

    /*
     * Instantiate parameters and headers for HTTP requests, as well as the HttpClient
     * class that will be used throughout the app.
     */
    private void initTask(MobileResponseHandler handler) {
        _activeRequest = null;
        _pendingRequest = null;
        _touchstoneUsername = null;
        _touchstonePassword = null;
        _responseData = null;

        _isFinished = false;
        _responseHandler = handler;
        HttpParams clientParams = new BasicHttpParams();
        HttpProtocolParams.setVersion(clientParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(clientParams, "utf-8");
        HttpClientParams.setRedirecting(clientParams, Boolean.TRUE);
        HttpClientParams.setAuthenticating(clientParams, Boolean.FALSE);
        HttpClientParams.setCookiePolicy(clientParams, CookiePolicy.BROWSER_COMPATIBILITY);
        clientParams.setBooleanParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);

        DefaultHttpClient client = new DefaultHttpClient(_sharedConnectionManager, clientParams);
        client.setCookieStore(_sharedCookieStore);
        client.setRedirectHandler(new DefaultRedirectHandler() {
            @Override
            public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
                int statusCode = response.getStatusLine().getStatusCode();

                boolean shouldRedirect = ((statusCode == 301)
                        || (statusCode == 302)
                        || (statusCode == 307))
                        && response.containsHeader("Location");

                URI redirectUri = null;

                if (shouldRedirect) {
                    try {
                        redirectUri = super.getLocationURI(response, context);
                    } catch (org.apache.http.ProtocolException ex) {
                        Log.i("MAPI:REDIRECT", ex.getMessage());
                    }
                }
                boolean redirectToOrigin = ((_activeRequest != null) && (_activeRequest.request.method.equalsIgnoreCase(MobileRequest.VERB_POST))
                        && (_activeRequest.state == REQUEST_OK_AUTH));

                if (shouldRedirect && redirectToOrigin) {
                    shouldRedirect = !(redirectUri.getHost().equalsIgnoreCase(_originalRequest.url.getHost()));
                }

                if (shouldRedirect)
                {
                    Log.i("MAPI:REDIRECT", "Redirecting to '"+redirectUri.toString()+"'");
                }
                return shouldRedirect && (redirectUri != null);
            }

            @Override
            public URI getLocationURI(HttpResponse response, HttpContext context) throws org.apache.http.ProtocolException {
                URI redirectUri = super.getLocationURI(response, context);
                _lastRedirectedUri = Uri.parse(redirectUri.toString());
                return redirectUri;
            }
        });

        _httpClient = client;

        _originalRequest = this.createRequest();
        this.enqueueRequest(REQUEST_OK, _originalRequest);
    }

    private void cancelRequest() {
        this.fatalError("The request was canceled", RequestError.REQUEST_CANCELED);
    }

    private void finishRequest() {
        _isFinished = true;

        _activeRequest = null;
        _pendingRequest = null;
        _touchstoneUsername = null;
        _touchstonePassword = null;
        _responseData = null;
    }

    /*
     * Add pending request
     */
    private void enqueueRequest(int state, MobileRequest request) {
        MobileRequestState nextRequest = new MobileRequestState(state, request);
        if (_pendingRequest != null) {
            Log.w(this.getClass().getName(), "Error: Overwriting pending request");
        } else {
            Log.i(this.getClass().getName(), "Queueing request to " + request.url.toString() + " for state "
                    + this.getStringForState(state));
        }

        this.setPendingRequest(nextRequest);
    }

    private void handleOKResponse(MobileRequest request, MobileResponse response) {
        final Uri targetUrl = request.url;
        final Uri responseUrl = (_lastRedirectedUri == null) ? targetUrl : _lastRedirectedUri;
        boolean sameHost = targetUrl.getHost().equalsIgnoreCase(responseUrl.getHost());

        if (sameHost) {
            _responseData = response.contentBody;
            _isFinished = true;
        } else {
            IAuthenticationCallback callback = new IAuthenticationCallback() {
                public void onCredentialLoad(String username, String password) {
                    _touchstoneUsername = username;
                    _touchstonePassword = password;

                    boolean mitIdp = (username.indexOf("@mit.edu") != -1) || (username.indexOf("@") == -1);
                    String idpUri = (mitIdp ? "https://idp.mit.edu/shibboleth" : "https://idp.touchstonenetwork.net/shibboleth-idp");

                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put("user_idp", idpUri);
                    parameters.put("duration", "none");

                    MobileRequest newRequest = new MobileRequest(responseUrl, MobileRequest.VERB_POST, parameters);
                    enqueueRequest(REQUEST_AUTHENTICATE, newRequest);
                }

                public void onCancel() {
                    cancelRequest();
                }
            };

            if (_authenticationHandler != null) {
                _authenticationHandler.onAuthenticationChallenge(this, callback);
            }
        }
    }

    private void handleAuthenticateResponse(MobileRequest request, MobileResponse response) {
    	Log.i(TAG, "handling authentication response");
        String responseData = response.contentBody;
        
        TouchstoneResponse tsResponse = new TouchstoneResponse(_lastRedirectedUri, responseData);

        if (tsResponse.isError() || (this._authenticationHandler == null)) {
        	Log.i(TAG, "touchstone error");
            boolean authError = (this._authenticationHandler == null)
                    || (tsResponse.getError().getCode() == RequestError.INVALID_CREDENTIALS);
            AuthenticationResult result = (authError ? AuthenticationResult.AUTHENTICATION_FAILED : AuthenticationResult.AUTHENTICATION_UNKNOWN);
            boolean retry = this.authenticationCompleted(result);

            if (retry) {
                this.enqueueRequest(REQUEST_OK, _originalRequest);
            } else {
                _error = new RequestError("Invalid or missing credentials", tsResponse.getError().getCode());
                this.cancelRequest();
            }
        } else if (tsResponse.isAuthenticationPrompt()) {
        	Log.i(TAG, "authentication prompt");
            Uri target = Uri.parse(tsResponse.getTargetUrl().toString());

            String username = _touchstoneUsername.replaceAll("(?i)@mit.edu", "");

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(tsResponse.getUserFieldName(), username);
            parameters.put(tsResponse.getPasswordFieldName(), _touchstonePassword);

            this.enqueueRequest(REQUEST_AUTHENTICATE, new MobileRequest(target, MobileRequest.VERB_POST, parameters));
        } else if (tsResponse.isSAMLAssertion()) {
        	Log.i(TAG, "saml assertion");
            MobileRequest samlRequest = new MobileRequest(Uri.parse(tsResponse.getTargetUrl().toString()), MobileRequest.VERB_POST, tsResponse.getSamlParameters());
            this.enqueueRequest(REQUEST_OK_AUTH, samlRequest);
        } else {
        	Log.i(TAG, "something went wrong here...");
        }
    }

    private boolean authenticationCompleted(AuthenticationResult result) {
        boolean retryAuth = false;

        if (_authenticationHandler != null) {
            _authenticationHandler.onAuthenticationCompleted(this, result);

            if (result != AuthenticationResult.AUTHENTICATION_SUCCESS) {
                retryAuth = _authenticationHandler.shouldRetryAuthentication(this, result);
            }
        }

        return retryAuth;
    }

    private MobileResponse executeRequest(MobileRequest request) throws UnsupportedEncodingException {
        HttpUriRequest httpRequest;
        HttpHost host = null;
        if (request.method.equalsIgnoreCase(MobileRequest.VERB_POST)) {
            HttpPost postRequest = new HttpPost(request.url.toString());

            List<NameValuePair> parameters = new ArrayList<NameValuePair>(request.parameters.size());

            for (Map.Entry<String, String> param : request.parameters.entrySet()) {
                parameters.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }

            HttpEntity postEntity = new UrlEncodedFormEntity(parameters);
            postRequest.setEntity(postEntity);
            httpRequest = postRequest;
        } else {
            httpRequest = new HttpGet(request.url.toString());
        }
        
        httpRequest.addHeader("User-Agent", MobileAPI.getDefaultUserAgent());
        
        HttpResponse response;
        try {
            if (host != null) {
                response = this._httpClient.execute(host, httpRequest);
            } else {
                response = this._httpClient.execute(httpRequest);
            }
        } catch (IOException ex) {
            Log.e("MAPI:Execute", "Execute failed!", ex);
            this.fatalError(ex.getMessage(), RequestError.UNKNOWN_ERROR);
            response = null;
        }

        if (response != null) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseData;

            try {
                responseData = EntityUtils.toString(response.getEntity());
            } catch (IOException ex) {
            	Log.i(TAG, "failed to read HTTP body: " + ex.getMessage());
                this.fatalError("Failed to read HTTP body: " + ex.getMessage(), RequestError.UNKNOWN_ERROR);
                return null;
            } catch (ParseException ex) {
            	Log.i(TAG, "Malformed HTTP response: " + ex.getMessage());
                this.fatalError("Malformed HTTP response: " + ex.getMessage(), RequestError.UNKNOWN_ERROR);
                return null;
            }


            Log.i("MAPI:Execute", "Execute finished with status code " + statusCode + ", read " + responseData.length() + " characters");
            return new MobileResponse(response, responseData);
        }

        return null;
    }

    private String getStringForState(int state) {
        switch (state) {
            case REQUEST_IDLE:
                return "REQUEST_IDLE";
            case REQUEST_OK:
                return "REQUEST_OK";
            case REQUEST_AUTHENTICATE:
                return "REQUEST_AUTHENTICATE";
            case REQUEST_ERROR:
                return "REQUEST_ERROR";
            case REQUEST_OK_AUTH:
                return "REQUEST_OK_AUTH";
            default:
                return "UNKNOWN_STATE";
        }
    }

    private boolean isRedirect(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();

        return ((status == HttpURLConnection.HTTP_MULT_CHOICE)
                || (status == HttpURLConnection.HTTP_MOVED_PERM)
                || (status == HttpURLConnection.HTTP_MOVED_TEMP));
    }

    private boolean isSecureRedirect(URL source, URL target) {
        boolean sourceIsSecure = source.getProtocol().equalsIgnoreCase("https");
        boolean targetIsSecure = target.getProtocol().equalsIgnoreCase("https");
        boolean sameHost = source.getHost().equalsIgnoreCase(target.getHost());

        return ((sourceIsSecure == false)
                && (targetIsSecure == true)
                && (sameHost == true));
    }

    private boolean isRedirectAllowed(URL source, URL target) {
        boolean sourceIsSecure = source.getProtocol().equalsIgnoreCase("https");
        boolean targetIsSecure = target.getProtocol().equalsIgnoreCase("https");

        // Allow redirects if we are either going from 'http' to 'https'
        //  on the same host or redirecting to a different host using the
        //  same scheme.
        return (this.isSecureRedirect(source, target)
                || (sourceIsSecure == targetIsSecure));
    }

    private void fatalError(String message, int errorCode, Exception ex) {
        if (_error == null) {
            _error = new RequestError(message, errorCode);
        }

        this.cancel(true);
    }

    private void fatalError(String message, int errorCode) {
        this.fatalError(message, errorCode, null);
    }
}