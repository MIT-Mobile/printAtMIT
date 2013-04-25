package edu.mit.printAtMIT.model.touchstone;

import android.content.Context;
import android.net.Uri;
import android.os.*;
import edu.mit.mobile.api.touchstone.InteractiveTouchstoneManager;
import edu.mit.mobile.api.touchstone.TouchstoneManager;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MobileRequest implements Parcelable {
    private static CookieManager mCookieManager;
    private static TouchstoneManager mTouchstoneManager;

    private static final String KEY_URL = "key.url";
    private static final String KEY_PATH = "key.relative-path";
    private static final String KEY_COMMAND = "key.command";
    private static final String KEY_MODULE = "key.module";
    private static final String KEY_POST = "key.post";
    private static final String KEY_UUID = "key.uuid";
    private static final String KEY_PARAMETERS = "key.parameters";

    public final UUID uuid;
    public final URL url;
    public final String path;
    public final String command;
    public final String module;
    public boolean usePOST;

    public Map<String, String> parameters;

    public static final Parcelable.Creator<MobileRequest> CREATOR
            = new Parcelable.Creator<MobileRequest>() {
        public MobileRequest createFromParcel(Parcel in) {
            try {
                return new MobileRequest(in);
            } catch (MalformedURLException ex) {
                return null;
            }
        }

        public MobileRequest[] newArray(int size) {
            return new MobileRequest[size];
        }
    };

    static {
        clearTouchstoneToken();
    }

    public static final synchronized void clearTouchstoneToken() {
        mCookieManager = new CookieManager();
        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(mCookieManager);
    }

    public static final synchronized void setTouchstoneManager(TouchstoneManager aManager) {
        if (mTouchstoneManager != null) {
            mTouchstoneManager.cancelAllRequests();
        }

        mTouchstoneManager = aManager;
    }

    public static final synchronized TouchstoneManager getTouchstoneManager() {
        return mTouchstoneManager;
    }

    /**
     * Creates and executes a MobileRequestTask object. If there is no Touchstone manager installed
     * and the task receives a Touchstone challenge the request will be canceled and signal an error.
     *
     * @param request the request you wish to execute
     * @param listener the callback that will receive the success and error notices
     * @see TouchstoneManager
     * @see edu.mit.mobile.api.touchstone.BasicTouchstoneManager
     * @see edu.mit.mobile.api.touchstone.InteractiveTouchstoneManager
     * @return
     */
    public static MobileRequestTask execute(final MobileRequest request, final ConnectionListener listener) {
        return execute(null, new MobileRequest[]{request}, listener);
    }

    /**
     * Creates and executes a MobileRequestTask object. If there is no Touchstone manager installed
     * and the task receives a Touchstone challenge the request will be canceled and signal an error.
     *
     * @param requests an array of requests that you wish to execute
     * @param listener the callback that will receive the success and error notices
     * @see TouchstoneManager
     * @return
     */
    public static MobileRequestTask execute(final MobileRequest[] requests, final ConnectionListener listener) {
        return execute(null,requests,listener);
    }

    /**
     * Creates and executes a MobileRequestTask object. If there is no Touchstone manager installed,
     * this method will create one by default (currently the InteractiveTouchstoneManager).
     *
     * @param request the request you wish to execute
     * @param listener the callback that will receive the success and error notices
     * @see TouchstoneManager
     * @return
     */
    public static MobileRequestTask execute(final Context aContext, final MobileRequest request, final ConnectionListener listener) {
        return execute(aContext, new MobileRequest[]{request}, listener);
    }

    /**
     *
     * @param aContext
     * @param requests
     * @param listener
     * @return
     */
    public static MobileRequestTask execute(final Context aContext, final MobileRequest[] requests, final ConnectionListener listener) {
        final MobileRequestTask task = new MobileRequestTask(listener);

        if ((aContext != null) && (getTouchstoneManager() == null)) {
            setTouchstoneManager(new InteractiveTouchstoneManager(aContext));
        }

        task.execute(requests);
        return task;
    }

    public MobileRequest(Parcel parcel) throws MalformedURLException {
        Bundle bundle = parcel.readBundle();

        this.uuid = (UUID) bundle.getSerializable(KEY_UUID);

        if (bundle.containsKey(KEY_URL)) {
            this.url = new URL(bundle.getString(KEY_URL));
        } else {
            this.url = null;
        }

        if (bundle.containsKey(KEY_PATH)) {
            this.path = bundle.getString(KEY_PATH);
        } else {
            this.path = null;
        }

        if (bundle.containsKey(KEY_COMMAND)) {
            this.command = bundle.getString(KEY_COMMAND);
        } else {
            this.command = null;
        }

        if (bundle.containsKey(KEY_MODULE)) {
            this.module = bundle.getString(KEY_MODULE);
        } else {
            this.module = null;
        }

        this.usePOST = bundle.getBoolean(KEY_POST, false);

        Map<String, String> paramMap = new HashMap<String, String>();
        if (bundle.containsKey(KEY_PARAMETERS)) {
            Bundle pBundle = bundle.getBundle(KEY_PARAMETERS);

            for (String key : pBundle.keySet()) {
                String data = pBundle.getString(key);
                if (data != null) {
                    paramMap.put(key, data);
                }
            }

        }
        this.parameters = paramMap;
    }

    public MobileRequest(MobileRequest request) {
        this.uuid = request.uuid;
        this.url = request.url;
        this.path = request.path;
        this.command = request.command;
        this.module = request.module;
        this.usePOST = request.usePOST;
        this.parameters = request.parameters;
    }

    public MobileRequest(String aModule, String aCommand, Map<String, String> aParameters) {
        uuid = UUID.randomUUID();
        url = null;
        path = null;

        module = aModule;
        command = aCommand;
        parameters = aParameters;
        usePOST = false;
    }

    public MobileRequest(String relativePath, Map<String, String> aParameters) {
        uuid = UUID.randomUUID();
        url = null;
        module = null;
        command = null;

        path = relativePath;
        parameters = aParameters;
        usePOST = false;
    }

    public MobileRequest(URL aURL, Map<String, String> aParameters) {
        uuid = UUID.randomUUID();
        path = null;
        module = null;
        command = null;

        url = aURL;
        parameters = aParameters;
        usePOST = false;
    }

    public Map<String, String> getParameters() {
        if (this.parameters == null) {
            return new HashMap<String, String>();
        }
        return parameters;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        Bundle parcelBundle = new Bundle();

        parcelBundle.putSerializable(KEY_UUID, this.uuid);

        if (this.url != null) {
            parcelBundle.putString(KEY_URL, this.url.toString());
        }

        if (this.path != null) {
            parcelBundle.putString(KEY_PATH, this.path);
        }

        if (this.module != null) {
            parcelBundle.putString(KEY_MODULE, this.module);
        }

        if (this.command != null) {
            parcelBundle.putString(KEY_COMMAND, this.command);
        }

        if (this.parameters.isEmpty() == false) {
            Bundle mapBundle = new Bundle();
            for (String key : this.parameters.keySet()) {
                mapBundle.putString(key, this.parameters.get(key).toString());
            }

            parcelBundle.putBundle(KEY_PARAMETERS, mapBundle);
        }
    }

    private String getDefaultAPIServer() {
        return "http://mobile-dev.mit.edu/api/";
    }

    public String getParameterString(boolean asPostBody) {
        if (asPostBody) {
            StringBuilder builder = new StringBuilder();

            for (String key : this.getParameters().keySet()) {
                builder.append(URLEncoder.encode(key)).append("=").append(URLEncoder.encode(this.getParameters().get(key).toString()));
                builder.append("&");
            }

            builder.deleteCharAt(builder.lastIndexOf("&"));

            return builder.toString();
        } else {
            Uri uri = this.toURI();
            return uri.getEncodedQuery();
        }
    }

    public URL toURL() {
        try {
            return new URL(this.toURI().toString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public Uri toURI() {
        String baseString;

        if (this.url != null) {
            baseString = this.url.toString();
        } else {
            baseString = this.getDefaultAPIServer();
        }

        Uri.Builder builder = Uri.parse(baseString).buildUpon();

        // Only add on the "command" and "module" parameters
        // if we are communicating with the MIT api server
        if (this.url == null) {
            if (this.module != null && (this.module.length() > 0)) {
                builder.appendQueryParameter("module", this.module);
            }

            if (this.command != null && (this.command.length() > 0)) {
                builder.appendQueryParameter("command", this.command);
            }
        }

        if ((this.usePOST == false) && !this.getParameters().isEmpty()) {
            for (String key : this.getParameters().keySet()) {
                builder.appendQueryParameter(key, this.getParameters().get(key).toString());
            }
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return this.toURI().toString();
    }
}
