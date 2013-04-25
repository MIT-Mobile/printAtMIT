package edu.mit.printAtMIT.model.touchstone.internal.messaging;

import android.os.*;
import android.util.Pair;

import java.util.UUID;

public class CredentialMessage {
    private final String KEY_USER = "key.user";
    private final String KEY_PASS = "key.pass";
    public final Messenger localMessenger;
    public final Messenger remoteMessenger;
    public final UUID requestId;
    public final Pair<String,String> credentials;


    public CredentialMessage(Messenger localMessenger, String username, String password) {
        this.localMessenger = localMessenger;
        this.remoteMessenger = null;
        this.credentials = new Pair<String,String>(username,password);
        this.requestId = null;
    }

    public CredentialMessage(Messenger localMessenger, UUID id, String username, String password) {
        this.localMessenger = localMessenger;
        this.remoteMessenger = null;
        this.credentials = new Pair<String,String>(username,password);
        this.requestId = id;
    }

    public CredentialMessage(Message inMessage) {
        if (inMessage.what == MessageConstants.MSG_TOUCHSTONE) {
            Bundle data = inMessage.getData();
            ParcelUuid parcelId = data.getParcelable(MessageConstants.KEY_UUID);
            String username = data.getString(KEY_USER);
            String password = data.getString(KEY_PASS);

            if (parcelId != null) {
                this.requestId = parcelId.getUuid();
            } else {
                this.requestId = null;
            }

            this.remoteMessenger = inMessage.replyTo;
            this.localMessenger = null;
            this.credentials = new Pair<String, String>(username,password);
        } else {
            this.localMessenger = null;
            this.remoteMessenger = inMessage.replyTo;
            this.credentials = null;
            this.requestId = null;
        }
    }

    public Message toMessage() {
        Message message = Message.obtain();

        Bundle data = new Bundle();

        if (this.requestId != null) {
            data.putParcelable(MessageConstants.KEY_UUID,new ParcelUuid(this.requestId));
        }

        if (this.credentials != null) {
            data.putString(KEY_USER,this.credentials.first);
            data.putString(KEY_PASS,this.credentials.second);
        }

        message.setData(data);

        message.what = MessageConstants.MSG_TOUCHSTONE;
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
