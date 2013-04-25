package edu.mit.printAtMIT.model.touchstone.internal.messaging;

import android.os.*;

import java.util.UUID;

public class CancelMessage {
    public final Messenger localMessenger;
    public final Messenger remoteMessenger;
    public final UUID uuid;

    public CancelMessage(Messenger localMessenger, UUID id) {
        this.localMessenger = localMessenger;
        this.remoteMessenger = null;
        this.uuid = id;
    }

    public CancelMessage(Message inMessage) {
        if (inMessage.what == MessageConstants.MSG_TOUCHSTONE) {
            Bundle data = inMessage.getData();
            ParcelUuid parcelId = data.getParcelable(MessageConstants.KEY_UUID);

            if (parcelId != null) {
                this.uuid = parcelId.getUuid();
            } else {
                this.uuid = null;
            }

            this.remoteMessenger = inMessage.replyTo;
            this.localMessenger = null;
        } else {
            this.localMessenger = null;
            this.remoteMessenger = inMessage.replyTo;
            this.uuid = null;
        }
    }

    public Message toMessage() {
        Message message = Message.obtain();

        Bundle data = new Bundle();
        if (this.uuid != null) {
            data.putParcelable(MessageConstants.KEY_UUID,new ParcelUuid(this.uuid));
        }
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
