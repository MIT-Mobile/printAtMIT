package edu.mit.printAtMIT.model.touchstone;

import java.util.List;

import org.apache.http.client.HttpClient;

import edu.mit.printAtMIT.controller.client.PrinterClientException;

/*
 * Interface for handling response with MobileAPI
 */
public interface MobileResponseHandler {
    public void onRequestCompleted(String result, HttpClient _httpClient) throws PrinterClientException;
    public void onCanceled();
    public void onError(int code, String message);
}
