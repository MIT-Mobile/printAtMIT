package edu.mit.printAtMIT.model.touchstone.touchstone;

import android.content.*;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.util.UUID;

public class BasicTouchstoneManager extends TouchstoneManager {
    public static final String TAG = InteractiveTouchstoneManager.class.getSimpleName();
    public static final String INTENT_TOUCHSTONE = "edu.mit.mobile.api.Intent.Touchstone";

    private static final String PREFERENCES_TOUCHSTONE = "edu.mit.mobile.api.Touchstone";
    private static final String PREFERENCES_KEY_USER = "edu.mit.mobile.api.Touchstone.key.user";
    private static final String PREFERENCES_KEY_PASSWORD = "edu.mit.mobile.api.Touchstone.key.pass";


    private volatile Pair<String, String> mTrialCredentials;
    private volatile boolean mPersistCredentials = false;
    private volatile UUID mPromptRequest = null;



    public synchronized static void setTouchstoneCredentials(Context context, String username, String password) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_TOUCHSTONE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();

        // If there is no username, clear the currently saved credentials
        if ((username == null) || (username.length() == 0)) {
            editor.remove(PREFERENCES_KEY_USER);
            editor.remove(PREFERENCES_KEY_PASSWORD);
        } else {
            editor.putString(PREFERENCES_KEY_USER, username);

            if ((password == null) || (password.length() == 0)) {
                editor.remove(PREFERENCES_KEY_PASSWORD);
            } else {
                editor.putString(PREFERENCES_KEY_PASSWORD, password);
            }
        }

        editor.apply();
    }

    public synchronized static Pair<String, String> getTouchstoneCredentials(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_TOUCHSTONE, Context.MODE_PRIVATE);

        return Pair.create(preferences.getString(PREFERENCES_KEY_USER, null),
                preferences.getString(PREFERENCES_KEY_PASSWORD, null));

    }

    public BasicTouchstoneManager(Context context) {
        super(context);
    }

    @Override
    protected void onActiveTaskChanged(UUID oldTask, UUID newTask) {
        if ((mPromptRequest != null) && mPromptRequest.equals(oldTask)) {
            mPromptRequest = newTask;
        }
    }

    @Override
    public void onChallenge(UUID id) {
        Pair<String, String> credentials = getTouchstoneCredentials(this.getContext());

        boolean validCredentials = (((credentials.first != null) &&
                                     (credentials.first.length() > 0)) ||
                                    ((credentials.second != null) &&
                                     (credentials.second.length() > 0)));

        if (validCredentials) {
            this.notifyCredentialsAvailable(id, credentials.first, credentials.second);
        } else {
            cancelRequest(id);
        }
    }

    @Override
    public void onSuccess(UUID id) {
        Log.w(TAG, "request '" + id + "' successfully authenticated");
        Toast.makeText(getContext(),"Touchstone login succeeded", 2).show();
    }

    @Override
    public boolean onCredentialsIncorrect(UUID id) {
        Log.w(TAG, "request '" + id + "' - provided incorrect credentials");
        Toast.makeText(getContext(),"Touchstone username or password is invalid", 2).show();

        return false;
    }

    @Override
    public void onError(UUID id) {
        Log.w(TAG, "request '" + id + "' - encountered an error");
        Toast.makeText(getContext(),"The Touchstone service is unavailable", 2).show();
    }
}
