package edu.mit.printAtMIT.model.touchstone;

import java.util.UUID;

public interface ConnectionListener {
    public void onConnectionError(UUID id, MobileTaskException exception);
    public void onConnectionFinished(UUID id, MobileResult result);
}
