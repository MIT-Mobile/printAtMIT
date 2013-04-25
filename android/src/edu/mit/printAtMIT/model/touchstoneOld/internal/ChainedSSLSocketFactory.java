/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mit.printAtMIT.model.touchstoneOld.internal;

import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import javax.net.ssl.SSLSocket;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.BasicHttpParams;

/**
 *
 * @author bskinner
 */
public class ChainedSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory _defaultFactory = SSLSocketFactory.getSocketFactory();

    public ChainedSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        super(truststore);
        _defaultFactory.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        Socket socket = null;
        IOException exception = null;

        try {
            socket = _defaultFactory.createSocket(s, host, port, autoClose);
        } catch (IOException ioe) {
            exception = ioe;
        }

        if (socket == null) {
            try {
                SSLSocket sslsock = (SSLSocket) super.createSocket();
                Log.i("CSSF:createSocket[4]", TextUtils.join(", ", sslsock.getSupportedProtocols()));
                sslsock.setEnabledProtocols(new String[]{"TLSv1"});
                socket = super.connectSocket(sslsock, host, port, null, 0, new BasicHttpParams());
            } catch (IOException ioe) {
                Log.e("CSSF:createSocket[4]", "Failed to create secure socket", ioe);
            }
        }

        if ((socket == null) && (exception != null)) {
            throw exception;
        } else if ((socket != null) && (socket instanceof SSLSocket)) {
            SSLSocket sslsock = (SSLSocket) socket;
            Log.i("CSSF:createSocket[4]", TextUtils.join(", ", sslsock.getSupportedProtocols()));
            sslsock.setEnabledProtocols(new String[]{"TLSv1"});
            sslsock.setNeedClientAuth(false);
            sslsock.setWantClientAuth(false);
            sslsock.setReuseAddress(false);
        }


        return socket;
    }

    @Override
    public Socket createSocket() throws IOException {
        Socket socket = null;
        IOException exception = null;

        try {
            socket = _defaultFactory.createSocket();
        } catch (IOException ioe) {
            exception = ioe;
        }

        if (socket == null) {
            try {
                socket = super.createSocket();
            } catch (IOException ioe) {
                Log.e("CSSF:createSocket[0]", "Failed to create secure socket", ioe);
            }
        }

        if ((socket == null) && (exception != null)) {
            throw exception;
        }

        return socket;
    }

    /*
     @Override
     public Socket connectSocket(Socket sock, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException {
     int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
     int soTimeout = HttpConnectionParams.getSoTimeout(params);
     InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
     SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());

     if ((localAddress != null) || (localPort > 0)) {
     // we need to bind explicitly
     if (localPort < 0) {
     localPort = 0; // indicates "any"
     }
     InetSocketAddress isa = new InetSocketAddress(localAddress, localPort);
     sslsock.bind(isa);
     }

     sslsock.connect(remoteAddress, connTimeout);
     sslsock.setSoTimeout(soTimeout);
     return sslsock;
     }
     */
}
