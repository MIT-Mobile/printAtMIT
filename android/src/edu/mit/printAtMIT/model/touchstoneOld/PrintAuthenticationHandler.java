package edu.mit.printAtMIT.model.touchstoneOld;

import android.util.Log;
import edu.mit.printAtMIT.model.touchstoneOld.authn.AuthenticationResult;
import edu.mit.printAtMIT.model.touchstoneOld.authn.IAuthenticationCallback;
import edu.mit.printAtMIT.model.touchstoneOld.authn.IMobileAuthenticationHandler;


public class PrintAuthenticationHandler implements IMobileAuthenticationHandler {
    private String _username;
    private String _password;
    private static final String TAG = "Authentication Handler";
    public PrintAuthenticationHandler(String username, String password)
    {
        assert ((username != null) && (password != null));
        
        _username = username;
        _password = password;
    }

    public void onAuthenticationChallenge(MobileAPI operation, IAuthenticationCallback callback) {
        Log.i(TAG, "authentication challenged");
        if (callback != null) {
            callback.onCredentialLoad(_username, _password);
        }
    }

    public void onAuthenticationCompleted(MobileAPI operation, AuthenticationResult result) {
        /* Do Nothing */
    }

    public boolean shouldRetryAuthentication(MobileAPI operation, AuthenticationResult result) {
        return false;
    }
}
