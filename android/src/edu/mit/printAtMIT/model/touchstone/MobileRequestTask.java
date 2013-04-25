package edu.mit.printAtMIT.model.touchstone;

import android.os.*;
import android.util.Log;
import edu.mit.mobile.api.internal.ConnectionResult;
import edu.mit.mobile.api.internal.TouchstoneResponse;
import edu.mit.mobile.api.internal.messaging.CancelMessage;
import edu.mit.mobile.api.internal.messaging.CredentialMessage;
import edu.mit.mobile.api.internal.messaging.MessageConstants;
import edu.mit.mobile.api.MobileTaskException.Code;
import edu.mit.mobile.api.internal.RequestStateContext;
import edu.mit.mobile.api.internal.RequestStateContext.State;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MobileRequestTask extends AsyncTask<MobileRequest, Void, Void> {
    public static final String IDENTIFIER = "edu.mit.mobile.api.MobileService";
    public static final String TAG = MobileRequestTask.class.getSimpleName();

    private final Object blockObject = new Object();

    private ConcurrentLinkedQueue<UUID> mCredentialRequests = new ConcurrentLinkedQueue<UUID>();
    private ConcurrentMap<UUID, RequestStateContext> mRequests = new ConcurrentHashMap<UUID, RequestStateContext>();
    private ConnectionListener mConnectionListener;

    private Handler mMyHandler;
    private Messenger mMyMessenger;
    private final Handler.Callback mMessengerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MessageConstants.MSG_TOUCHSTONE: {
                    CredentialMessage result = new CredentialMessage(message);
                    if (mCredentialRequests.contains(result.requestId)) {
                        RequestStateContext context = getRequestContext(result.requestId);

                        if (context != null) {
                            context.touchstoneCredentials = result.credentials;
                        } else {
                            // We can let the touchstone manager know there was an error but, other than that
                            // there isn't much we can do without a valid state context.
                            MobileTaskException error = new MobileTaskException(context.uuid, "received message for request not in queue", MobileTaskException.Code.UNKNOWN_ERROR);
                            notifyAuthenticateError(context.uuid, error);
                        }

                        synchronized (blockObject) {
                            blockObject.notifyAll();
                        }
                    }
                    return true;
                }

                case MessageConstants.MSG_TOUCHSTONE_CANCEL: {
                    CancelMessage cancelMessage = new CancelMessage(message);
                    if (mCredentialRequests.contains(cancelMessage.uuid)) {
                        RequestStateContext context = mRequests.get(cancelMessage.uuid);

                        if (context != null) {
                            cancelRequest(context.uuid);
                        }

                        MobileTaskException error = new MobileTaskException(context.uuid, MobileTaskException.Code.REQUEST_CANCELED);
                        notifyAuthenticateError(context.uuid, error);

                        synchronized (blockObject) {
                            blockObject.notifyAll();
                        }
                    }
                    return true;
                }
            }

            return false;
        }
    };

    public MobileRequestTask(ConnectionListener listener) {
        super();

        setConnectionListener(listener);
    }

    public void setConnectionListener(ConnectionListener aListener) {
        this.mConnectionListener = aListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Looper looper = Looper.myLooper();
        mMyHandler = new Handler(looper, this.mMessengerCallback);
        mMyMessenger = new Messenger(mMyHandler);
    }

    @Override
    protected void onPostExecute(Void voids) {

    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        for (UUID id : this.mRequests.keySet()) {
            this.cancelRequest(id);
        }
    }

    @Override
    protected Void doInBackground(MobileRequest... argv) {
        for (MobileRequest request : argv) {
            Log.i(TAG, "processing request '" + request + "'");
            RequestStateContext context = new RequestStateContext(request);
            this.mRequests.put(context.uuid, context);
        }

        for (UUID id : mRequests.keySet()) {
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            RequestStateContext stateContext = this.mRequests.get(id);
            final UUID uuid = stateContext.uuid;

            try {
                do {
                    handleRequest(uuid);
                } while (!stateContext.isFinished());

                // We managed to exit out

                if (mConnectionListener != null) {
                    final ConnectionResult result = stateContext.result;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            MobileResult svcResult = new MobileResult(result.contentBody, result.mimeType, result.responseCode);
                            mConnectionListener.onConnectionFinished(uuid, svcResult);
                        }
                    });
                }

                if (this.mCredentialRequests.contains(uuid)) {
                    final MobileTaskException mte = new MobileTaskException(uuid, MobileTaskException.Code.REQUEST_CANCELED);
                    notifyAuthenticateError(uuid, mte);
                    mCredentialRequests.remove(uuid);

                    if (mConnectionListener != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mConnectionListener.onConnectionError(uuid, mte);
                            }
                        });
                    }
                }
            } catch (final MobileTaskException mte) {
                if (mConnectionListener != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mConnectionListener.onConnectionError(uuid, mte);
                        }
                    });
                }
            } catch (final Exception ex) {
                if (mConnectionListener != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mConnectionListener.onConnectionError(uuid, new MobileTaskException(uuid,((Throwable)ex), MobileTaskException.Code.UNKNOWN_ERROR));
                        }
                    });
                }
            } finally {
                mRequests.remove(uuid);
            }

            if (stateContext.isCanceled()) {

            }

            this.mRequests.remove(stateContext.uuid);
        }

        return null;
    }

    private boolean handleRequest(final UUID id) throws MobileTaskException {
        RequestStateContext svcRequest = this.getRequestContext(id, true);

        if (!svcRequest.isFinished()) {
            if (svcRequest.getActiveRequest() != null) {
                try {
                    svcRequest.execute();
                } catch (Exception ex) {
                    throw new MobileTaskException(id, ex, MobileTaskException.Code.REQUEST_CANCELED);
                }
            }

            try {
                State requestState = svcRequest.getState();

                switch (requestState) {
                    case IDLE: {
                        handleStateIdle(svcRequest);
                    }
                    break;

                    case OK: {
                        handleStateOK(svcRequest);
                    }
                    break;

                    case OK_AUTH: {
                        handleAuthenticateSuccess(svcRequest);
                    }
                    break;

                    case CREDENTIALS_REQUESTED: {
                        handleStateChallenge(svcRequest);
                    }
                    break;

                    case TS_AUTHENTICATE: {
                        handleStateAuthenticate(svcRequest);
                    }
                    break;

                    case SAML_REPLY: {
                        handleStateSamlHandshake(svcRequest);
                    }
                    break;

                    default:
                    case COMPLETE: {
                        handleStateCompleted(svcRequest);
                    }
                }
            } catch (Exception ex) {
                throw new MobileTaskException(id, ex, MobileTaskException.Code.REQUEST_CANCELED);
            }
        }

        return svcRequest.isFinished();
    }

    private void cancelRequest(UUID id) {
        RequestStateContext context = this.getRequestContext(id);
        if (context != null) {
            boolean result = this.notifyAuthenticateError(context.uuid, new MobileTaskException(id, MobileTaskException.Code.REQUEST_CANCELED));
            if (!result) {
                Log.e(TAG, "failed to cancel request " + id);
            }
        }
    }

    private RequestStateContext getRequestContext(final UUID id) {
        return this.mRequests.get(id);
    }

    private RequestStateContext getRequestContext(final UUID id, boolean isCritical) throws MobileTaskException {
        RequestStateContext context = this.getRequestContext(id);

        if (isCritical && (context == null)) {
            throw new MobileTaskException(id, "failed to find request for '" + id + "'", MobileTaskException.Code.REQUEST_CANCELED);
        }

        return context;
    }

    private boolean notifyCredentialsIncorrect(final UUID id) {
        return this.notifyAuthenticateError(id, new MobileTaskException(id, MobileTaskException.Code.INVALID_CREDENTIALS));
    }

    private boolean notifyAuthenticateError(final UUID id, MobileTaskException error) {
        // At this point, just report that there was an error but it was not
        // related to the credentials.
        boolean hasManager = (MobileRequest.getTouchstoneManager() != null);

        if (hasManager && mCredentialRequests.contains(id)) {
            mCredentialRequests.remove(id);

            if (error.code == MobileTaskException.Code.INVALID_CREDENTIALS) {
                MobileRequest.getTouchstoneManager().postOnCredentialsIncorrect(id);
            } else {
                MobileRequest.getTouchstoneManager().postOnError(id,error.getMessage(),error.code);
            }

            return true;
        } else {
            if (this.mCredentialRequests.contains(id)) {
                Log.e(TAG, "fatal: request was added to credential queue without a manager present");
            }
        }

        return false;
    }

    private boolean notifyAuthenticateSuccess(final UUID id) {
        boolean hasManager = (MobileRequest.getTouchstoneManager() != null);

        if (this.mCredentialRequests.contains(id) && hasManager) {
            mCredentialRequests.remove(id);
            MobileRequest.getTouchstoneManager().postOnSuccess(id);
            return true;
        } else {
            if (this.mCredentialRequests.contains(id)) {
                Log.e(TAG, "fatal: request was added to credential queue without a manager present");
            }
        }

        return false;
    }

    private boolean notifyRequestCredentials(final UUID id) {
        if (MobileRequest.getTouchstoneManager() == null) {
            return false;
        } else {
            if (!this.mCredentialRequests.contains(id)) {
                mCredentialRequests.add(id);
            }

            MobileRequest.getTouchstoneManager().postOnChallenge(id, this.mMyMessenger);

            boolean interrupted;
            do {
                try {
                    synchronized (blockObject) {
                        blockObject.wait();
                    }
                    interrupted = false;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            } while (interrupted);

            return true;
        }
    }

    private final void handleStateIdle(RequestStateContext context) throws MobileTaskException {
        Log.d(TAG, "processing response for state 'IDLE'");

        if (context.initialRequest == null) {
            throw new MobileTaskException(context.uuid, Code.UNKNOWN_ERROR);
        } else {
            context.setState(RequestStateContext.State.OK, context.initialRequest);
        }
    }

    private final void handleStateOK(RequestStateContext context) {
        Log.d(TAG, "processing response for state 'OK'");

        ConnectionResult content = context.response;
        if (context.wasRedirectedToSource()) {
            context.result = content;
            context.setState(RequestStateContext.State.COMPLETE);
        } else {
            // Looks like we are not authenticated and most likely ended up at the WAYF or the
            // Touchstone generic error page. Just cycle around
            context.setState(RequestStateContext.State.CREDENTIALS_REQUESTED, context.getActiveRequest());
        }
    }

    private final void handleStateChallenge(RequestStateContext context) throws MobileTaskException {
        Log.d(TAG, "processing response for state 'CREDENTIAL_REQUEST'");

        if (MobileRequest.getTouchstoneManager() == null) {
            throw new MobileTaskException(context.uuid, MobileTaskException.Code.TOUCHSTONE_UNAVAILABLE);
        } else {
            if (context.touchstoneCredentials == null) {
                if (notifyRequestCredentials(context.uuid) == false) {
                    throw new MobileTaskException(context.uuid, Code.TOUCHSTONE_ERROR);
                }
            } else {
                String username = context.touchstoneCredentials.first;
                String password = context.touchstoneCredentials.second;

                boolean validCredentials = ((username != null) &&
                                            (username.length() > 0) &&
                                            (password != null) &&
                                            (password.length() > 0));

                if (validCredentials) {
                    boolean mitIdp = ((username.indexOf("@mit.edu") != -1) ||
                                      (username.indexOf("@") == -1));
                    String idpUri = (mitIdp ? "https://idp.mit.edu/shibboleth" : "https://idp.touchstonenetwork.net/shibboleth-idp");

                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put("user_idp", idpUri);
                    parameters.put("duration", "none");

                    MobileRequest request = new MobileRequest(context.lastRedirect, parameters);
                    request.usePOST = true;

                    context.setState(RequestStateContext.State.TS_AUTHENTICATE, request);
                } else {
                    context.touchstoneCredentials = null;
                    notifyCredentialsIncorrect(context.uuid);
                }
            }
        }
    }

    private final void handleStateAuthenticate(RequestStateContext context) throws MobileTaskException {
        Log.d(TAG, "processing response for state 'TS_AUTHENTICATE'");

        if (MobileRequest.getTouchstoneManager() == null) {
            throw new MobileTaskException(context.uuid, MobileTaskException.Code.TOUCHSTONE_UNAVAILABLE);
        }

        ConnectionResult content = context.response;
        String responseData = content.contentBody;
        TouchstoneResponse tsResponse;

        try {
            tsResponse = new TouchstoneResponse(context.lastRedirect, responseData);
        } catch (TouchstoneResponse.TouchstoneErrorException e) {
            tsResponse = null;
            context.touchstoneCredentials = null;

            if (e.incorrectCredentials) {
                if (notifyCredentialsIncorrect(context.uuid)) {
                    context.setState(RequestStateContext.State.CREDENTIALS_REQUESTED);
                } else {
                    throw new MobileTaskException(context.uuid, MobileTaskException.Code.TOUCHSTONE_ERROR);
                }
            } else {
                MobileTaskException error = new MobileTaskException(context.uuid, e, Code.TOUCHSTONE_ERROR);
                notifyAuthenticateError(context.uuid, error);
                throw error;
            }
        }

        if ((tsResponse != null) && tsResponse.isCredentialPrompt()) {
            URL target = tsResponse.getTargetUrl();
            String username = context.touchstoneCredentials.first.replaceAll("(?i)@mit.edu", "");

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(tsResponse.getUserFieldName(), username);
            parameters.put(tsResponse.getPasswordFieldName(), context.touchstoneCredentials.second);

            MobileRequest request = new MobileRequest(target, parameters);
            request.usePOST = true;

            context.setState(State.SAML_REPLY, request);
        } else {
            MobileTaskException error = new MobileTaskException(context.uuid, MobileTaskException.Code.TOUCHSTONE_UNAVAILABLE);
            notifyAuthenticateError(context.uuid, error);
            throw error;
        }
    }

    private final void handleStateSamlHandshake(RequestStateContext context) throws MobileTaskException {
        Log.d(TAG, "processing response for state 'TS_AUTHENTICATE'");

        if (MobileRequest.getTouchstoneManager() == null) {
            throw new MobileTaskException(context.uuid, MobileTaskException.Code.TOUCHSTONE_UNAVAILABLE);
        }

        ConnectionResult content = context.response;
        String responseData = content.contentBody;
        TouchstoneResponse tsResponse = null;

        try {
            tsResponse = new TouchstoneResponse(context.lastRedirect, responseData);
        } catch (TouchstoneResponse.TouchstoneErrorException e) {
            context.touchstoneCredentials = null;

            if (e.incorrectCredentials) {
                notifyCredentialsIncorrect(context.uuid);
                context.setState(RequestStateContext.State.CREDENTIALS_REQUESTED);
            } else {
                MobileTaskException exception = new MobileTaskException(context.uuid, e, MobileTaskException.Code.TOUCHSTONE_ERROR);
                notifyAuthenticateError(context.uuid, exception);
                throw exception;
            }

        }

        if (tsResponse.isSAMLAssertion()) {
            MobileRequest request = new MobileRequest(tsResponse.getTargetUrl(), tsResponse.getSamlParameters());
            request.usePOST = true;

            context.setState(RequestStateContext.State.OK_AUTH, request);
        } else {
            MobileTaskException exception = new MobileTaskException(context.uuid, MobileTaskException.Code.TOUCHSTONE_ERROR);
            notifyAuthenticateError(context.uuid, exception);
            throw exception;
        }
    }

    private final void handleAuthenticateSuccess(RequestStateContext context) throws MobileTaskException {
        Log.d(TAG, "processing response for state 'OK_AUTHENTICATED'");

        if (context.lastRedirect == null) {
            // Uh oh. Looks like authentication failed for some really bad reason. Ideally, we should
            // never reach this state since, if authentication fails, the request should just circle
            // around in the AUTHENTICATE state and there will always be a 'lastRedirect'.
            // Reaching this state usually implies a seriously malformed request or Touchstone barfing
            throw new MobileTaskException(context.uuid, "unexpected redirect from Touchstone", MobileTaskException.Code.TOUCHSTONE_ERROR);
        } else {
            if (context.wasRedirectedToSource()) {
                notifyAuthenticateSuccess(context.uuid);
            }
        }

        context.setState(RequestStateContext.State.OK, context.initialRequest);
    }

    private final void handleStateCompleted(RequestStateContext context) {
        Log.d(TAG, "processing response for state 'COMPLETE'");
        context.setFinished(true);
    }
}
