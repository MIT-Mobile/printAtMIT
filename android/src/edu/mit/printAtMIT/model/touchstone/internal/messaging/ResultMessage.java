package edu.mit.printAtMIT.model.touchstone.internal.messaging;

import android.os.*;

import java.util.UUID;

public class ResultMessage {
    private final String KEY_SUCCESS = "key.touchstone-success";
    private final String KEY_BADPASS = "key.bad-credentials";

    public final Messenger localMessenger;
    public final Messenger remoteMessenger;
    public final UUID requestId;
    public final boolean isSuccess;
    public final boolean isCredentialFailure;

    public ResultMessage(Messenger localMessenger, boolean isSuccess, boolean credentialFailure) {
        this.localMessenger = localMessenger;
        this.remoteMessenger = null;
        this.isSuccess = isSuccess;
        this.isCredentialFailure = credentialFailure;
        this.requestId = null;
    }

    public ResultMessage(Messenger localMessenger, UUID id, boolean isSuccess, boolean credentialFailure) {
        this.localMessenger = localMessenger;
        this.remoteMessenger = null;
        this.isSuccess = isSuccess;
        this.isCredentialFailure = credentialFailure;
        this.requestId = id;
    }

    public ResultMessage(Message inMessage) {
        if (inMessage.what == MessageConstants.MSG_TOUCHSTONE) {
            Bundle data = inMessage.getData();
            ParcelUuid parcelId = data.getParcelable(MessageConstants.KEY_UUID);

            if (parcelId != null) {
                this.requestId = parcelId.getUuid();
            } else {
                this.requestId = null;
            }

            this.remoteMessenger = inMessage.replyTo;
            this.localMessenger = null;
            this.isSuccess = data.getBoolean(KEY_SUCCESS, false);
            this.isCredentialFailure = data.getBoolean(KEY_BADPASS, false);
        } else {
            this.localMessenger = null;
            this.remoteMessenger = inMessage.replyTo;
            this.requestId = null;
            this.isSuccess = false;
            this.isCredentialFailure = false;
        }
    }

    public Message toMessage() {
        Message message = Message.obtain();

        Bundle data = new Bundle();
        if (this.requestId != null) {
            data.putParcelable(MessageConstants.KEY_UUID,new ParcelUuid(this.requestId));
        }

        data.putBoolean(KEY_SUCCESS, this.isSuccess);
        data.putBoolean(KEY_BADPASS, this.isCredentialFailure);
        message.setData(data);

        message.what = MessageConstants.MSG_TOUCHSTONE_RESULT;
        message.replyTo = this.localMessenger;
        return message;
    }

    public boolean send(Messenger remoteMessenger) {
        Message message = this.toMessage();
        boolean result;

        try {
            remoteMessenger.send(message);
            result = true;
        } catch (RemoteException e) {
            result = false;
        }

        return result;
    }
}
