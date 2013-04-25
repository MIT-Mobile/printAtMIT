package edu.mit.printAtMIT.model.touchstone.internal;

import android.net.Uri;
import android.text.Html;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.htmlcleaner.*;

public class TouchstoneResponse {

    static final CleanerProperties _cleanerProperties;
    static final HtmlCleaner _htmlCleaner;
    private static final String[] _touchstoneLoginActionPaths;
    private static final String[] _touchstoneUsernamePaths;
    private static final String[] _touchstonePasswordPaths;
    private static final String[] _touchstoneErrorPaths;
    private static final String kShibbolethAssertActionPath = "//body/form";
    private static final String kShibbolethAssertInputsPath = "//body/form/div/input";
    private boolean isSAMLAssertion = false;
    private boolean isCredentialPrompt = false;
    private URL _targetUrl;
    private String _userFieldName;
    private String _passwordFieldName;
    private Map<String, String> _samlParameters;
    private URL _sourceUrl;
    private String _httpBody;

    public class TouchstoneErrorException extends Exception {
        public final boolean incorrectCredentials;

        public TouchstoneErrorException(Exception cause, boolean incorrectCredentials) {
            super(cause);
            this.incorrectCredentials = false;
        }

        public TouchstoneErrorException(String message, boolean incorrectCredentials) {
            super(message);
            this.incorrectCredentials = false;
        }
    }

    static {
        _cleanerProperties = new CleanerProperties();
        _htmlCleaner = new HtmlCleaner(_cleanerProperties);

        _touchstoneLoginActionPaths = new String[]{
                "//div[@id='loginbox']/form[@id='loginform']",    // Desktop
                "//div[@id='container']/form[@id='kform']"        // Mobile
        };

        _touchstoneUsernamePaths = new String[]{
                "//div[@id='loginbox']/form[@id='loginform']/fieldset/label/input[@type='text']/@name",    // Desktop
                "//div[@id='container']/form[@id='kform']//input[@id='username']/@name" // Mobile
        };

        _touchstonePasswordPaths = new String[]{
                "//div[@id='loginbox']/form[@id='loginform']/fieldset/label/input[@type='text']/@name", // Desktop
                "//div[@id='container']/form[@id='kform']//input[@id='pwd']/@name" // Mobile
        };

        _touchstoneErrorPaths = new String[]{
                "//div[@id='loginbox']/div[@class='error']/p/text()", // Desktop
                "//div[@id='container']/div[@class='alertbox warning']/text()", // Mobile
        };
    }

    public TouchstoneResponse(URL requestUri, String httpBody) throws TouchstoneErrorException {
        _sourceUrl = requestUri;
        _httpBody = httpBody;
        _samlParameters = new HashMap<String, String>();

        this.processResponse();
    }

    public boolean isSAMLAssertion() {
        return isSAMLAssertion;
    }

    public boolean isCredentialPrompt() {
        return isCredentialPrompt;
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

    private void processResponse() throws TouchstoneErrorException {
        TagNode docRoot = _htmlCleaner.clean(_httpBody);

        if (this.checkForErrorWithNode(docRoot)) {
            // We should never reach here since checkForErrorWithNode never returns true; it either will
            // throw an exception or return false
            throw new TouchstoneErrorException("unknown error processing the response from the Touchstone service", false);
        } else if (this.authnRequestWithNode(docRoot)) {
            this.isCredentialPrompt = true;
            this.isSAMLAssertion = false;
        } else if (this.samlAttributesWithNode(docRoot)) {
            this.isCredentialPrompt = false;
            this.isSAMLAssertion = true;
        } else {
            throw new TouchstoneErrorException("invalid response from the Touchstone service", false);
        }
    }

    // This method is a bit of an oddity. If any error is encountered, it will throw a
    // TouchstoneErrorException but, in the case of no errors, it will return false.
    private boolean checkForErrorWithNode(TagNode rootNode) throws TouchstoneErrorException {
        Object[] nodes = this.evaluatePathsForNode(rootNode, _touchstoneErrorPaths);

        if (nodes.length > 0) {
            String nodeText = nodes[0].toString().trim();

            if (nodeText.toLowerCase().contains("password")) {
                throw new TouchstoneErrorException("username or password is incorrect", true);
            } else {
                throw new TouchstoneErrorException(nodeText, false);
            }
        }

        return false;
    }

    private boolean samlAttributesWithNode(TagNode rootNode) throws TouchstoneErrorException {
        Uri targetUrl;
        Object[] nodes = this.evaluatePathsForNode(rootNode, new String[]{kShibbolethAssertActionPath});

        if (nodes.length == 0) {
            // This page doesn't look like anything remotely like what we were expecting.
            // Report that we won't handle it (by returning false) to give the class
            // another chance to parse it as something else. Any further errors should result
            // in an exception being thrown
            return false;
        }

        TagNode assertNode;
        Object nodeObject = nodes[0];
        if (nodeObject instanceof TagNode) {
            assertNode = (TagNode) nodeObject;
        } else {
            throw new TouchstoneErrorException("unexpected node type encountered while processing response", false);
        }

        String action = Html.fromHtml(assertNode.getAttributeByName("action")).toString();

        if (action.startsWith("/")) {
            Uri.Builder builder = Uri.parse(_sourceUrl.toString()).buildUpon();
            builder.encodedPath(action);
            targetUrl = builder.build();
        } else {
            targetUrl = Uri.parse(action);
        }

        if (targetUrl == null) {
            throw new TouchstoneErrorException("invalid Touchstone response", false);
        }

        // If we get to this point, don't return true/false on error. Any error should result in
        // a TouchstoneErrorException indicating a malformed response
        nodes = this.evaluatePathsForNode(rootNode, new String[]{kShibbolethAssertInputsPath});
        if (nodes.length == 0) {
            throw new TouchstoneErrorException("invalid Touchstone response", false);
        }

        for (Object obj : nodes) {
            TagNode myNode = (TagNode) (obj);
            String name = Html.fromHtml(myNode.getAttributeByName("name")).toString();
            String value = myNode.getAttributeByName("value");

            // Check to see if the string contains Base64
            // encoded data. If it does, don't mess with it!
            if (value.endsWith("==") == false) {
                value = Html.fromHtml(value).toString();
            }

            _samlParameters.put(name, value);
        }

        try {
            _targetUrl = new URL(targetUrl.toString());
        } catch (MalformedURLException e) {
            throw new TouchstoneErrorException(e, false);
        }

        return true;
    }

    private boolean authnRequestWithNode(TagNode rootNode) throws TouchstoneErrorException {
        Uri targetUrl;
        Object[] nodes = this.evaluatePathsForNode(rootNode, _touchstoneLoginActionPaths);

        if (nodes.length > 0) {
            TagNode node = (TagNode) (nodes[0]);
            String action = node.getAttributeByName("action");

            if (action.startsWith("/")) {
                Uri.Builder builder = Uri.parse(_sourceUrl.toString()).buildUpon();
                builder.encodedPath(action);
                targetUrl = builder.build();
            } else {
                targetUrl = Uri.parse(action);
            }
        } else {
            return false;
        }

        if (targetUrl == null) {
            throw new TouchstoneErrorException("invalid Touchstone target uri", false);
        }


        String userFieldName;
        nodes = this.evaluatePathsForNode(rootNode, _touchstoneUsernamePaths);
        if (nodes.length > 0) {
            userFieldName = (String) (nodes[0]);
        } else {
            throw new TouchstoneErrorException("malformed Touchstone response", false);
        }


        String passwordFieldName;
        nodes = this.evaluatePathsForNode(rootNode, _touchstonePasswordPaths);
        if (nodes.length > 0) {
            passwordFieldName = (String) (nodes[0]);
        } else {
            throw new TouchstoneErrorException("malformed Touchstone response", false);
        }


        try {
            _targetUrl = new URL(targetUrl.toString());
        } catch (MalformedURLException e) {
            throw new TouchstoneErrorException(e, false);
        }

        _userFieldName = userFieldName;
        _passwordFieldName = passwordFieldName;

        return true;
    }

    private Object[] evaluatePathsForNode(final TagNode rootNode, String[] paths) {
        try {
            Object[] nodes;
            for (String path : paths) {
                nodes = rootNode.evaluateXPath(path);

                if ((nodes != null) && (nodes.length > 0)) {
                    return nodes;
                }
            }
        } catch (XPatherException e) {
            return null;
        }

        return new Object[0];
    }
}