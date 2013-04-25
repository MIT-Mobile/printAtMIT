package edu.mit.printAtMIT.model.touchstone;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author bskinner
 */
public class MobileResult {
    private static final String TAG = MobileResult.class.getCanonicalName();
    private JSONObject mCachedJsonObject;
    private JSONArray mCachedJsonArray;
    private final String _contentBody;
    private final String _mimeType;
    private final int _httpStatusCode;
    private boolean mIsJson;
    
    public MobileResult(String contentBody, String mimeType, int statusCode) {
        _contentBody = contentBody;
        _mimeType = mimeType;
        _httpStatusCode = statusCode;
        
        JSONArray array = null;
        if (mCachedJsonObject == null) {
            try {
                array = new JSONArray(contentBody);
            } catch (JSONException jse) {
                Log.d(TAG, "failed to create JSON array, content does not appear to be JSON content", jse);
            }
        }
        mCachedJsonArray = array;
    }

    public JSONObject  getJSONObject() {
        if ((mCachedJsonObject == null) && (mCachedJsonArray == null)) {
            JSONObject object = null;

            try {
                object = new JSONObject(this.getContentBody());
            } catch ( JSONException jse ) {
                Log.d(TAG, "failed to create JSON object, attempt to parse as array", jse);
            }
            
            mCachedJsonObject = object;
        }

        return this.mCachedJsonObject;
    }
    
    public JSONArray getJSONArray() {
        if ((mCachedJsonArray == null) && (mCachedJsonObject == null)) {
            JSONArray object = null;

            try {
                object = new JSONArray(this.getContentBody());
            } catch ( JSONException jse ) {
                Log.d(TAG, "failed to create JSON object, attempt to parse as array", jse);
            }

            mCachedJsonArray = object;
        }

        return this.mCachedJsonArray;
    }
    
    public String getMimeType() {
        return this._mimeType;
    }
    
    public String getContentBody() {
        return this._contentBody;
    }

    public int getResponseCode() {
        return _httpStatusCode;
    }
}
