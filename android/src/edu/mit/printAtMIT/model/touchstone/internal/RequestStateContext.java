package edu.mit.printAtMIT.model.touchstone.internal;

import android.util.Log;
import android.util.Pair;
import edu.mit.mobile.api.MobileRequest;
import edu.mit.mobile.api.MobileTaskException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;

public class RequestStateContext {

    public static enum State {
        INVALID,
        IDLE,
        OK,
        CREDENTIALS_REQUESTED,
        TS_AUTHENTICATE,
        SAML_REPLY,
        OK_AUTH,
        COMPLETE
    }

    public static final String TAG = RequestStateContext.class.getSimpleName();
    public final UUID uuid;

    public MobileRequest initialRequest;
    public URL lastRedirect;
    public URL sourceURL;
    public Pair<String, String> touchstoneCredentials;
    public ConnectionResult result;
    public ConnectionResult response;

    private boolean mIsCanceled = false;
    private boolean mDispatched = false;
    private boolean mIsFinished = false;
    private MobileRequest mActiveRequest;
    private State mState = State.INVALID;

    protected static State getInitialState() {
        return State.IDLE;
    }

    public RequestStateContext(MobileRequest request) {
        this.initialRequest = request;
        uuid = request.uuid;
        this.setState(getInitialState(), null);
    }

    public MobileRequest getActiveRequest() {
        return mActiveRequest;
    }

    public void setDispatched() {
        mDispatched = true;
    }

    public boolean wasDispatched() {
        return mDispatched;
    }


    public State getState() {
        return mState;
    }

    public void reset() {
        this.result = null;
        this.response = null;
        this.lastRedirect = null;
        this.response = null;
        this.mDispatched = false;
        this.mIsFinished = false;
        this.mIsCanceled = false;
        this.setState(getInitialState(), new MobileRequest(this.initialRequest));
    }

    public void cancel() {
        this.mIsCanceled = true;
    }

    public boolean isCanceled() {
        return mIsCanceled;
    }

    public final boolean setState(State aState) {
        return this.setState(aState, null);
    }

    public final boolean setState(State aState, URL aURL, Map<String, String> aParameters) {
        MobileRequest request = new MobileRequest(aURL, aParameters);
        return this.setState(aState, request);
    }

    public final boolean setState(State aState, MobileRequest request) {
        // If we are finished, stop processing any more state change
        // requests and signal the caller that the state change failed
        if (mIsFinished || aState == null) {
            mIsFinished = true;
            return false;
        }

        mState = aState;
        mDispatched = false;
        lastRedirect = null;
        response = null;
        mActiveRequest = request;

        return true;
    }

    public final boolean wasRedirectedToSource() {
        boolean result = false;

        if (this.sourceURL != null) {
            String sourceHost = this.sourceURL.getHost();
            if (this.lastRedirect == null) {
                result = sourceHost.equalsIgnoreCase(initialRequest.toURL().getHost());
            } else {
                result = sourceHost.equalsIgnoreCase(lastRedirect.getHost());
            }
        }

        return result;
    }

    public void setFinished(boolean finished) {
        if (mIsFinished == false) {
            mIsFinished = finished;
        }
    }

    public boolean isFinished() {
        return mIsFinished;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        RequestStateContext newRequest = new RequestStateContext(this.initialRequest);
        newRequest.mDispatched = this.mDispatched;
        newRequest.mState = this.mState;
        newRequest.sourceURL = this.sourceURL;
        newRequest.lastRedirect = this.lastRedirect;
        newRequest.mActiveRequest = this.mActiveRequest;

        return newRequest;
    }

    public void execute() throws IOException, NoSuchAlgorithmException, MobileTaskException {
        MobileRequest request = this.getActiveRequest();

        if (request == null) {
            return;
        } else {
            if (this.wasDispatched()) {
                return;
            }
        }

        if (System.getProperty("http.keepAlive", "true").equalsIgnoreCase("true")) {
            System.setProperty("http.keepAlive", "false");
        }

        URL requestUrl = request.toURL();
        HttpURLConnection pendingConnection = null;
        String responseBody = null;

        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        final URL startingURL = connection.getURL();
        URL contextSource = startingURL;
        if ((this.sourceURL == null) && requestUrl.equals(this.initialRequest.toURL())) {
            contextSource = startingURL;
        }


        this.setDispatched();

        boolean redirect = false;
        do {
            if (redirect) {
                Log.v(TAG, "\t--> '" + connection.getURL().toString() + "'");
            } else {
                Log.v(TAG, "Executing: '" + connection.getURL().toString() + "'");
            }
            try {

                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection sslConn = (HttpsURLConnection) connection;
                    /* Do nothing for the time being */
                    // FIXME: Need to add support for external keystore
                }
                connection.setInstanceFollowRedirects(true);
                connection.setDoInput(true);

                if ((redirect == false) && request.usePOST) {
                    byte[] queryData = request.getParameterString(true).getBytes();

                    connection.setDoOutput(true);
                    connection.setFixedLengthStreamingMode(queryData.length);
                    BufferedOutputStream bos = new BufferedOutputStream(connection.getOutputStream());
                    bos.write(queryData);
                    bos.flush();
                    bos.close();
                }

                InputStream in;

                try {
                    InputStream connectionInputStream = connection.getInputStream();
                    in = new BufferedInputStream(connectionInputStream);
                } catch (Throwable ioe) {
                    Log.e(TAG, String.valueOf(ioe));
                    Log.e(TAG, Log.getStackTraceString(ioe));
                    throw new MobileTaskException(this.uuid, ioe, MobileTaskException.Code.UNKNOWN_ERROR);
                }

                int responseCode = connection.getResponseCode();
                redirect = ((connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) ||
                            (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP));

                if (redirect) {
                    URL base = connection.getURL();
                    String location = connection.getHeaderField("Location");
                    URL target = null;

                    if (location != null) {
                        target = new URL(base, location);
                    }

                    boolean secureRedirect = ((target != null) &&
                                              (requestUrl.getProtocol().equals("http") == true) &&
                                              (target.getProtocol().equals("https") == true));
                    if (secureRedirect) {
                        pendingConnection = (HttpURLConnection) target.openConnection();

                        // Save the new URL we were redirected to if
                        // the hosts match. This will be used later to
                        // determine if we are back at the original URL
                        // after the Touchstone handshake
                        URL securedURL = pendingConnection.getURL();

                        // Make sure we're still on the original request
                        if (requestUrl.equals(initialRequest.toURL())) {
                            if ((this.sourceURL == null) && securedURL.getHost().equals(initialRequest.toURL().getHost())) {
                                this.sourceURL = pendingConnection.getURL();
                            }
                        }
                    } else {
                        throw new MobileTaskException(this.uuid, "insecure redirection to '" + target.getHost() + "'", MobileTaskException.Code.UNKNOWN_ERROR);
                    }
                } else {
                    if ((responseCode < HttpURLConnection.HTTP_OK) ||
                        (responseCode > HttpURLConnection.HTTP_NO_CONTENT)) {
                        throw new MobileTaskException(this.uuid, "unexpected http response '" + responseCode + "'", MobileTaskException.Code.UNKNOWN_ERROR);
                    } else {
                        if ((in != null) && (responseCode != HttpURLConnection.HTTP_NO_CONTENT)) {
                            byte[] buf = new byte[128];
                            int read;
                            StringBuilder response = new StringBuilder();

                            while ((read = in.read(buf)) != -1) {
                                response.append(new String(buf, 0, read));
                            }

                            responseBody = response.toString();
                        }
                    }
                }

            } catch (Exception ex) {
                Log.w(TAG, "Woah...");
            } finally {
                connection.disconnect();

                if (pendingConnection != null) {
                    connection = pendingConnection;
                    pendingConnection = null;
                } else {
                    if (startingURL.equals(connection.getURL()) == false) {
                        // Looks like we followed a redirect at some point!
                        this.lastRedirect = connection.getURL();
                    }
                }
            }
        } while (redirect);

        if (this.sourceURL == null) {
            this.sourceURL = contextSource;
        }

        this.response = new ConnectionResult(connection.getHeaderField("Content-Type"),
                responseBody,
                connection.getResponseCode(),
                connection.getResponseMessage());

        Log.v(TAG, "*****");
    }
}
