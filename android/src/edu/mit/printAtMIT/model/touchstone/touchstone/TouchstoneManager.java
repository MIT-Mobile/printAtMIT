package edu.mit.printAtMIT.model.touchstone.touchstone;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Messenger;
import android.util.Log;
import edu.mit.mobile.api.internal.messaging.CancelMessage;
import edu.mit.mobile.api.internal.messaging.CredentialMessage;
import edu.mit.mobile.api.MobileTaskException.Code;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TouchstoneManager {
    private static final String TAG = TouchstoneManager.class.getSimpleName();
    private Handler mMyHandler;
    private Context mContext;
    private ConcurrentHashMap<UUID, Messenger> mRequests = new ConcurrentHashMap<UUID, Messenger>();
    private volatile UUID mActiveRequest = null;

    public TouchstoneManager(Context context) {
        if (context == null) {
            mMyHandler = null;
            mContext = null;

            throw new IllegalArgumentException("context is null");
        }

        mContext = context.getApplicationContext();
        mMyHandler = new Handler(Looper.getMainLooper());
    }

    protected Context getContext() {
        return this.mContext;
    }

    public void cancelAllRequests() {
        // Clean up the request that is currently
        // in-flight first so we can re-use it later
        UUID activeRequest = getActiveTask();
        if (activeRequest != null) {
            cancelRequest(activeRequest);
            mRequests.remove(activeRequest);
        }

        // Iterate through the rest of the pending
        // requests and report that they should be canceled.
        // By the end of this, the existing queue should be
        // purged
        for (UUID id : mRequests.keySet()) {
            cancelRequest(id);
        }
    }

    public void cancelRequest(UUID id) {
        this.cancelRequest(id,false);
    }

    public synchronized void cancelRequest(UUID id, boolean credentialFailure) {
        final UUID active = getActiveTask();
        Messenger messenger = mRequests.get(id);

        if (messenger != null) {
            if (id.equals(active)) {
                if (credentialFailure) {
                    postOnError(id, "request was canceled", Code.INVALID_CREDENTIALS);
                } else {
                    postOnError(id, "request was canceled", Code.REQUEST_CANCELED);
                }
                setActiveTask(null);
            }
            mRequests.remove(id);

            CancelMessage message = new CancelMessage(null, id);
            message.send(messenger);
        }
    }

    public boolean notifyCredentialsAvailable(UUID id, String username, String password) {
        boolean result = false;

        if (mRequests.containsKey(id) || id.equals(getActiveTask())) {
            Messenger messenger = this.mRequests.get(id);

            CredentialMessage message = new CredentialMessage(null, id, username, password);
            result = message.send(messenger);
        } else {
            Log.e(TAG, "received credentials for unrequested id '" + id + "'");
        }

        return result;
    }

    public UUID getActiveTask() {
        synchronized (this) {
            return mActiveRequest;
        }
    }

    private boolean setActiveTask(UUID newTask) {
        final UUID oldTask = mActiveRequest;

        boolean updateTask;

        if (oldTask == newTask) {
            return true;
        } else {
            if (oldTask != null) {
                updateTask = !oldTask.equals(newTask);
            } else {
                updateTask = true;
            }
        }

        if (updateTask) {
            synchronized (this) {
                mActiveRequest = newTask;

                if ((oldTask != null) && mRequests.containsKey(oldTask)) {
                    mRequests.remove(oldTask);
                }

                this.onActiveTaskChanged(oldTask, newTask);
            }
        }

        return updateTask;
    }

    public final void postOnChallenge(final UUID id, final Messenger replyTo) {
        this.mMyHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mRequests.containsKey(id)) {
                    mRequests.put(id, replyTo);
                }

                if (getActiveTask() == null) {
                    // Grab any UUID, we don't care which one. Thanks to the above
                    // we are guaranteed to have at least one available
                    setActiveTask(mRequests.keySet().iterator().next());
                    onChallenge(getActiveTask());
                }
            }
        });
    }

    public final void postOnSuccess(final UUID id) {
        this.mMyHandler.post(new Runnable() {
            @Override
            public void run() {
                UUID active = getActiveTask();

                if ((active != null) && (id.equals(active))) {
                    onSuccess(id);
                    setActiveTask(null);
                } else {
                    Log.e(TAG, "call to onSuccess out of order for id " + active);
                }
            }
        });
    }

    public final void postOnCredentialsIncorrect(final UUID id) {
        this.mMyHandler.post(new Runnable() {
            @Override
            public void run() {
                UUID active = getActiveTask();

                if ((active != null) && (active.equals(id))) {
                    if (!onCredentialsIncorrect(id)) {
                        cancelRequest(id);
                    }
                } else {
                    Log.e(TAG, "call to onError out of order for id " + active);
                }
            }
        });
    }

    public final void postOnError(final UUID id, String message, Code code) {
        this.mMyHandler.post(new Runnable() {
            @Override
            public void run() {
                UUID active = getActiveTask();

                if ((active != null) && (active.equals(id))) {
                    onError(id);
                    setActiveTask(null);
                } else {
                    Log.e(TAG, "call to onError out of order for id " + active);
                }
            }
        });
    }

    protected abstract void onActiveTaskChanged(UUID oldTask, UUID newTask);

    public abstract void onChallenge(UUID id);

    public abstract void onSuccess(UUID id);

    public abstract boolean onCredentialsIncorrect(UUID id);

    public abstract void onError(UUID id);
}
