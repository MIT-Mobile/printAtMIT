package edu.mit.printAtMIT.model.touchstoneOld.authn;

public interface IAuthenticationCallback {
    public void onCredentialLoad(String username, String password);
    public void onCancel();
}
