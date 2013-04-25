package edu.mit.printAtMIT.model.touchstone.touchstone;

import android.content.*;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;

import java.util.UUID;

public class InteractiveTouchstoneManager extends TouchstoneManager {
    public static class Constants {
        public static final String TAG = Constants.class.getSimpleName();
        public static final String BROADCAST_INTENT_TOUCHSTONE = TAG + ".LocalBroadcast.Touchstone";

        public static final String NOTICE_CREDENTIALS = TAG + ".notice.Credentials";
        public static final String FLAG_ALLOW_SAVE = TAG + "flag.can-save";
        public static final String EXTRA_USERNAME = TAG + ".extra.username";
        public static final String EXTRA_PASSWORD = TAG + ".extra.password";

        public static final String NOTICE_DESTROYED = TAG + ".notice.OnDestroy";
        public static final String FLAG_USER_CANCELED = TAG + ".flag.Canceled-By-User";

        public static final String NOTICE_STATUS = TAG + ".notice.Request-Result";
        public static final String FLAG_SUCCESS = TAG + ".flag.Success";
        public static final String FLAG_ABORT = TAG + ".flag.Error";
        public static final String FLAG_ERROR = TAG + ".flag.Error";
        public static final String FLAG_ERROR_CREDENTIALS = TAG + ".flag.error.Bad-Credentials";

        private Constants() {}
    }

    public static final String TAG = InteractiveTouchstoneManager.class.getSimpleName();
    public static final String INTENT_TOUCHSTONE = "edu.mit.mobile.api.Intent.Touchstone";

    private static final String PREFERENCES_TOUCHSTONE = "edu.mit.mobile.api.Touchstone";
    private static final String PREFERENCES_KEY_USER = "edu.mit.mobile.api.Touchstone.key.user";
    private static final String PREFERENCES_KEY_PASSWORD = "edu.mit.mobile.api.Touchstone.key.pass";


    private volatile Pair<String, String> mTrialCredentials;
    private volatile boolean mPersistCredentials = false;
    private volatile UUID mPromptRequest = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final UUID id = mPromptRequest;

            if (intent.hasExtra(Constants.NOTICE_CREDENTIALS)) {
                String username = intent.getStringExtra(Constants.EXTRA_USERNAME);
                String password = intent.getStringExtra(Constants.EXTRA_PASSWORD);

                mPersistCredentials = intent.hasExtra(Constants.FLAG_ALLOW_SAVE);
                mTrialCredentials = Pair.create(username, password);
                notifyCredentialsAvailable(id, username, password);
            } else {
                if (intent.hasExtra(Constants.NOTICE_DESTROYED)) {
                    mPromptRequest = null;

                    if (intent.hasExtra(Constants.FLAG_USER_CANCELED)) {
                        cancelAllRequests();
                    }

                    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
                }
            }
        }
    };

    public InteractiveTouchstoneManager(Context context) {
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
        Pair<String, String> credentials = BasicTouchstoneManager.getTouchstoneCredentials(this.getContext());
        mPersistCredentials = false;

        boolean validCredentials = (((credentials.first != null) &&
                                     (credentials.first.length() > 0)) ||
                                    ((credentials.second != null) &&
                                     (credentials.second.length() > 0)));

        if (validCredentials) {
            this.notifyCredentialsAvailable(id, credentials.first, credentials.second);
        } else {
            if (mPromptRequest == null) {
                mPromptRequest = id;

                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this.getContext());
                manager.registerReceiver(this.mReceiver, new IntentFilter(Constants.BROADCAST_INTENT_TOUCHSTONE));

                Intent intent = new Intent(INTENT_TOUCHSTONE);
                intent = Intent.createChooser(intent, "Touchstone Prompt");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.getContext().startActivity(intent);
            }
        }
    }

    @Override
    public void onSuccess(UUID id) {
        Log.w(TAG, "request '" + id + "' successfully authenticated");

        if (id.equals(mPromptRequest)) {
            Intent intent = new Intent(Constants.BROADCAST_INTENT_TOUCHSTONE);
            intent.putExtra(Constants.NOTICE_STATUS, true);
            intent.putExtra(Constants.FLAG_SUCCESS, true);

            LocalBroadcastManager.getInstance(this.getContext()).sendBroadcast(intent);

            if (this.mPersistCredentials) {
                BasicTouchstoneManager.setTouchstoneCredentials(this.getContext(), mTrialCredentials.first, mTrialCredentials.second);
            }

            mTrialCredentials = null;
            mPromptRequest = null;
        }
    }

    @Override
    public boolean onCredentialsIncorrect(UUID id) {
        Log.w(TAG, "request '" + id + "' - provided incorrect credentials");

        if (mPromptRequest.equals(id)) {
            Intent intent = new Intent(Constants.BROADCAST_INTENT_TOUCHSTONE);
            intent.putExtra(Constants.NOTICE_STATUS, true);
            intent.putExtra(Constants.FLAG_ERROR, true);
            intent.putExtra(Constants.FLAG_ERROR_CREDENTIALS, true);

            LocalBroadcastManager.getInstance(this.getContext()).sendBroadcast(intent);

            // TODO: Make sure this is the right behavior...
            // On incorrect credentials, the activity *should* clear the
            // saved credentials if what was last tested and what is currently
            // saved match.
            if (mTrialCredentials != null) {
                Pair<String,String> savedCredentials = BasicTouchstoneManager.getTouchstoneCredentials(getContext());

                if (mTrialCredentials.equals(savedCredentials)) {
                    BasicTouchstoneManager.setTouchstoneCredentials(getContext(),null,null);
                }
            }

            mTrialCredentials = null;
        }

        return true;
    }

    @Override
    public void onError(UUID id) {
        Log.w(TAG, "request '" + id + "' - encountered an error");

        if (id.equals(mPromptRequest)) {
            Intent intent = new Intent(Constants.BROADCAST_INTENT_TOUCHSTONE);
            intent.putExtra(Constants.NOTICE_STATUS, true);
            intent.putExtra(Constants.FLAG_ERROR, true);
            intent.putExtra(Constants.FLAG_ERROR_CREDENTIALS, true);

            LocalBroadcastManager.getInstance(this.getContext()).sendBroadcast(intent);
            mTrialCredentials = null;
            mPromptRequest = null;
        }
    }
}
