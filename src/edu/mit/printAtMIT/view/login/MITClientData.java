package edu.mit.printAtMIT.view.login;

import java.net.URI;
import android.app.Activity;

public class MITClientData {

	private URI targetUri;
	private String touchstoneState;
	
	public String getTouchstoneState() {
		return touchstoneState;
	}
	public void setTouchstoneState(String touchstoneState) {
		this.touchstoneState = touchstoneState;
	}
	public URI getTargetUri() {
		return targetUri;
	}
	public void setTargetUri(URI targetUri) {
		this.targetUri = targetUri;
	}

	
}
