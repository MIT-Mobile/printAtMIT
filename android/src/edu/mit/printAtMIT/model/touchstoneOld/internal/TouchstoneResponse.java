package edu.mit.printAtMIT.model.touchstoneOld.internal;

import android.net.Uri;
import android.text.Html;
import android.util.Log;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.htmlcleaner.*;

import edu.mit.printAtMIT.model.touchstoneOld.RequestError;

public class TouchstoneResponse {

    static final CleanerProperties _cleanerProperties;
    static final HtmlCleaner _htmlCleaner;
    private static final String[] _touchstoneLoginActionPaths;
    private static final String[] _touchstoneUsernamePaths;
    private static final String[] _touchstonePasswordPaths;
    private static final String[] _touchstoneErrorPaths;
    private static final String kShibbolethAssertActionPath = "//body/form";
    private static final String kShibbolethAssertInputsPath = "//body/form/div/input";
    private RequestError _error;
    private Uri _targetUrl;
    private String _userFieldName;
    private String _passwordFieldName;
    private Map<String, String> _samlParameters;
    private Uri _sourceUrl;
    private String _httpBody;

    static {
        _cleanerProperties = new CleanerProperties();
        _htmlCleaner = new HtmlCleaner(_cleanerProperties);

        _touchstoneLoginActionPaths = new String[]{
            "//div[@id='loginbox']/form[@id='loginform']",	// Desktop
            "//div[@id='container']/form[@id='kform']"		// Mobile
        };

        _touchstoneUsernamePaths = new String[]{
            "//div[@id='loginbox']/form[@id='loginform']/fieldset/label/input[@type='text']/@name",	// Desktop
            "//div[@id='container']/form[@id='kform']//input[@id='username']/@name" // Mobile
        };

        _touchstonePasswordPaths = new String[]{
            "//div[@id='loginbox']/form[@id='loginform']/fieldset/label/input[@type='text']/@name", // Desktop
            "//div[@id='container']/form[@id='kform']//input[@id='pwd']/@name" // Mobile
        };
		
		_touchstoneErrorPaths = new String[]{
			"//div[@id='loginbox']/div[@class='error']/p", // Desktop
			"//div[@id='container']/div[@class='alertbox warning']", // Mobile
		};
    }

    public TouchstoneResponse(Uri requestUri, String httpBody) {
        _sourceUrl = requestUri;
        _httpBody = httpBody;
        _samlParameters = new HashMap<String, String>();

        this.processResponse();
    }

    public String getPasswordFieldName() {
        return _passwordFieldName;
    }

    public Map<String, String> getSamlParameters() {
        return _samlParameters;
    }

    public URL getTargetUrl() {
        try {
            URL target = new URL(_targetUrl.toString());
            return target;
        } catch (MalformedURLException ex) {
            Log.e("MAPI:TSR", null, ex);
            return null;
        }
    }

    public Uri getTargetUri() {
        return Uri.parse(this.getTargetUrl().toString());
    }

    public String getUserFieldName() {
        return _userFieldName;
    }

    public RequestError getError() {
        return _error;
    }

    public boolean isError() {
        return (_error != null);
    }

    public boolean isSAMLAssertion() {
        return ((this.isError() == false)
                && (_samlParameters != null)
                && (_targetUrl != null));
    }

    public boolean isAuthenticationPrompt() {
        return ((this.isError() == false)
                && (_userFieldName != null)
                && (_passwordFieldName != null)
                && (_targetUrl != null));
    }

    private void processResponse() {
        TagNode docRoot = _htmlCleaner.clean(_httpBody);

        if (this.errorWithNode(docRoot)) {
            return;
        }

        if (this.authnRequestWithNode(docRoot)) {
            return;
        }

        if (this.samlAttributesWithNode(docRoot)) {
            return;
        }

        _error = new RequestError("Touchstone response was malformed", RequestError.UNKNOWN_ERROR);
    }

    private boolean errorWithNode(TagNode rootNode) {
        Object[] nodes = new Object[]{};
        RequestError error = null;

        for (String path : _touchstoneErrorPaths) {
            try {
                nodes = rootNode.evaluateXPath(path);
            } catch (XPatherException ex) {
                Log.e(TouchstoneResponse.class.getName(), ex.toString());
                _error = new RequestError(ex.getLocalizedMessage(), RequestError.UNKNOWN_ERROR);
                return false;
            }
            
            if (nodes.length > 0)
            {
                break;
            }
        }

        if (nodes.length > 0) {
            TagNode node = (TagNode) (nodes[0]);
            String nodeText = node.getText().toString().trim();

            if (nodeText.toLowerCase().contains("password")) {
                error = new RequestError("Invalid username or password entered", RequestError.INVALID_CREDENTIALS);
            } else {
                error = new RequestError(nodeText, RequestError.TOUCHSTONE_ERROR);
            }
        }

        _error = error;
        return this.isError();
    }

    private boolean samlAttributesWithNode(TagNode rootNode) {
        Object[] nodes;
        RequestError error = null;
        Uri targetUrl = null;

        try {
            nodes = rootNode.evaluateXPath(kShibbolethAssertActionPath);
        } catch (XPatherException ex) {
            Log.e(TouchstoneResponse.class.getName(), ex.toString());
            _error = new RequestError(ex.getLocalizedMessage(), RequestError.UNKNOWN_ERROR);
            return false;
        }

        if (nodes.length > 0) {
            TagNode node = (TagNode) (nodes[0]);
            String action = Html.fromHtml(node.getAttributeByName("action")).toString();

            if (action.startsWith("/")) {
                Uri.Builder builder = _sourceUrl.buildUpon();
                builder.encodedPath(action);
                targetUrl = builder.build();
            } else {
                targetUrl = Uri.parse(action);
            }
        }

        if (targetUrl == null) {
            _error = new RequestError("Invalid or missing Touchstone response URI", RequestError.TOUCHSTONE_ERROR);
            return false;
        }

        try {
            nodes = rootNode.evaluateXPath(kShibbolethAssertInputsPath);
        } catch (XPatherException ex) {
            Log.e(TouchstoneResponse.class.getName(), ex.toString());
            _error = new RequestError(ex.getLocalizedMessage(), RequestError.UNKNOWN_ERROR);
            return false;
        }

        for (Object obj : nodes) {
            TagNode node = (TagNode) (obj);
            String name = Html.fromHtml(node.getAttributeByName("name")).toString();
            String value = node.getAttributeByName("value");

            // Check to see if the string contains Base64
            // encoded data. If it does, don't mess with it!
            if (value.endsWith("==") == false) {
                value = Html.fromHtml(value).toString();
            }

            _samlParameters.put(name, value);
        }

        _targetUrl = targetUrl;
        _error = null;
        return this.isSAMLAssertion();
    }

    private boolean authnRequestWithNode(TagNode rootNode) {
        Object[] nodes = new Object[]{};
        Uri targetUrl = null;

        for (String path : _touchstoneLoginActionPaths) {
            try {
                nodes = rootNode.evaluateXPath(path);
            } catch (XPatherException ex) {
                Log.e(TouchstoneResponse.class.getName(), ex.toString());
                _error = new RequestError(ex.getLocalizedMessage(), RequestError.UNKNOWN_ERROR);
                return false;
            }
            
            if (nodes.length > 0)
            {
                break;
            }
        }

        if (nodes.length > 0) {
            TagNode node = (TagNode) (nodes[0]);
            String action = node.getAttributeByName("action");

            if (action.startsWith("/")) {
                Uri.Builder builder = _sourceUrl.buildUpon();
                builder.encodedPath(action);
                targetUrl = builder.build();
            } else {
                targetUrl = Uri.parse(action);
            }
        }

        if (targetUrl == null) {
            _error = new RequestError("Invalid or missing Touchstone response URI", RequestError.TOUCHSTONE_ERROR);
            return false;
        }

        String userFieldName;
        nodes = new Object[]{}; // Sanity check!
        // Grab the name of the username field
        for (String path : _touchstoneUsernamePaths) {
            try {
                nodes = rootNode.evaluateXPath(path);
            } catch (XPatherException ex) {
                Log.e(TouchstoneResponse.class.getName(), ex.toString());
                _error = new RequestError(ex.getLocalizedMessage(), RequestError.UNKNOWN_ERROR);
                return false;
            }
            
            if (nodes.length > 0)
            {
                break;
            }
        }

        if (nodes.length > 0) {
            userFieldName = (String) (nodes[0]);
        } else {
            _error = new RequestError("Invalid or missing Touchstone username input", RequestError.TOUCHSTONE_ERROR);
            return false;
        }


        String passwordFieldName;
        // Grab the name of the password field
        nodes = new Object[]{}; // Sanity check!
        // Grab the name of the username field
        for (String path : _touchstonePasswordPaths) {
            try {
                nodes = rootNode.evaluateXPath(path);
            } catch (XPatherException ex) {
                Log.e(TouchstoneResponse.class.getName(), ex.toString());
                _error = new RequestError(ex.getLocalizedMessage(), RequestError.UNKNOWN_ERROR);
                return false;
            }
            
            if (nodes.length > 0)
            {
                break;
            }
        }
        
        if (nodes.length > 0) {
            passwordFieldName = (String) (nodes[0]);
        } else {
            _error = new RequestError("Invalid or missing Touchstone password input", RequestError.TOUCHSTONE_ERROR);
            return false;
        }


        _targetUrl = targetUrl;
        _userFieldName = userFieldName;
        _passwordFieldName = passwordFieldName;
        _error = null;
        return this.isAuthenticationPrompt();
    }
}
