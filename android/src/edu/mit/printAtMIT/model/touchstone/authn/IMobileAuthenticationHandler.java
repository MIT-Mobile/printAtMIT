package edu.mit.printAtMIT.model.touchstone.authn;

import edu.mit.printAtMIT.model.touchstone.MobileAPI;

public interface IMobileAuthenticationHandler {
    public void onAuthenticationChallenge(MobileAPI operation, IAuthenticationCallback callback);
    public void onAuthenticationCompleted(MobileAPI operation, AuthenticationResult result);
    public boolean shouldRetryAuthentication(MobileAPI operation, AuthenticationResult result);
}
