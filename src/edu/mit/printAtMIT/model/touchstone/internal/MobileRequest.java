/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mit.printAtMIT.model.touchstone.internal;

import android.net.Uri;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MobileRequest {
    public static final String VERB_GET = "GET";
    public static final String VERB_POST = "POST";
    
    public final Uri url;
    public final String method;
    public final Map<String, String> parameters;

    public MobileRequest(Uri url, String method, Map<String, String> parameters) {
        this.url = url;
        this.method = method;
        this.parameters = (parameters != null) ? parameters : new HashMap<String, String>();
    }

    public String getParameterString() {
        if (this.parameters == null) {
            return null;
        } else {
            StringBuilder builder = new StringBuilder();
            if (this.parameters.size() > 0) {
                for (String key : this.parameters.keySet()) {
                    String name = URLEncoder.encode(key);
                    String value = URLEncoder.encode(this.parameters.get(key));
                    builder.append(name).append('=').append(value).append('&');
                }

                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MobileRequest other = (MobileRequest) obj;
        if (this.url != other.url && (this.url == null || !this.url.equals(other.url))) {
            return false;
        }
        if ((this.method == null) ? (other.method != null) : !this.method.equals(other.method)) {
            return false;
        }
        if (this.parameters != other.parameters &&
            (this.parameters == null || !this.parameters.equals(other.parameters))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.url != null ? this.url.hashCode() : 0);
        hash = 67 * hash + (this.method != null ? this.method.hashCode() : 0);
        hash = 67 * hash + (this.parameters != null ? this.parameters.hashCode() : 0);
        return hash;
    }
}