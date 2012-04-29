package edu.mit.printAtMIT.view.login;

import android.os.Build;

public class UserAgent {
	
	public static String get() {
		return "Print At MIT/v1.0/Android/2.3.6";
//		return "MIT Mobile/" + BuildSettings.VERSION_NAME + 
//			" (" + BuildSettings.BUILD_GIT_DESCRIBE + ";)" +
//			" Android/" + Build.VERSION.RELEASE + 
//				" (" + Build.CPU_ABI  + "; " +  Build.MANUFACTURER + " " + Build.MODEL + ";)";
	}
}
