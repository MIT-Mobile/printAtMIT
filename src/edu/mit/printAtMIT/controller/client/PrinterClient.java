package edu.mit.printAtMIT.controller.client;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.Html;
import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.parse.ParseObject;

import edu.mit.printAtMIT.model.printer.ListType;
import edu.mit.printAtMIT.model.printer.Printer;
import edu.mit.printAtMIT.model.printer.PrinterList;
import edu.mit.printAtMIT.model.printer.SortType;
import edu.mit.printAtMIT.view.list.Item;

public class PrinterClient {
    public static final String URL_STRING = "http://mobile-print-dev.mit.edu/printatmit/query_result/?sort=\"%s\"&latitude=%d&longitude=%d";
    public static final String NAME_SORT = "name";
    public static final String BUILDING_SORT = "building";
    public static final String DISTANCE_SORT = "distance";

    /**
     * Returns a list of printers of type ListType as Item objects, ready to be
     * added to the view
     * 
     * @param context
     *            , Context of the activity calling the method
     * @param type
     *            , type of printer list requested (all, campus, dorm)
     * @param objects
     *            , list of Parse Objects
     * @return ArrayList of Items
     */
    public static ArrayList<Item> getPrinterList(Context context,
            ListType type, List<ParseObject> objects) {
        PrinterList printerList = new PrinterList(context, type);
        return printerList.getList(objects);
    }

    public static List<Printer> getPrinterList(ListType listtype,
            SortType sorttype) throws PrinterClientException {
        List<Printer> printers = new ArrayList<Printer>();

        try {
            printers = requestPrinterList(listtype, sorttype, 0, 0);
        } catch (ClientProtocolException e) {
            e.printStackTrace();

            throw new PrinterClientException("ClientProtocolException");
        } catch (URISyntaxException e) {
            e.printStackTrace();

            throw new PrinterClientException("URISyntaxException");

        } catch (IOException e) {
            e.printStackTrace();
            throw new PrinterClientException("IOException");

        } catch (JSONException e) {
            e.printStackTrace();
            throw new PrinterClientException("JSONException");
        }

        return printers;
    }

    public static List<Printer> getPrinterList(ListType listtype,
            SortType sorttype, double latitude, double longitude) throws PrinterClientException {
        List<Printer> printers = new ArrayList<Printer>();

        try {
            printers = requestPrinterList(listtype, sorttype, latitude, longitude);
        } catch (ClientProtocolException e) {
            e.printStackTrace();

            throw new PrinterClientException("ClientProtocolException");
        } catch (URISyntaxException e) {
            e.printStackTrace();

            throw new PrinterClientException("URISyntaxException");

        } catch (IOException e) {
            e.printStackTrace();
            throw new PrinterClientException("IOException");

        } catch (JSONException e) {
            e.printStackTrace();
            throw new PrinterClientException("JSONException");
        }

        return printers;
    }

    private static List<Printer> requestPrinterList(ListType listtype,
            SortType sorttype, double latitude, double longitude)
            throws URISyntaxException, ClientProtocolException, IOException,
            JSONException {
        String uri = "";
        switch (sorttype) {
        case NAME:
            uri = String.format(URL_STRING, "name", 0, 0);
            break;
        case BUILDING:
            uri = String.format(URL_STRING, "building", 0, 0);
            break;
        case DISTANCE:
            uri = String.format(URL_STRING, "distance", latitude, longitude);
            break;
        default:
            Log.e("PrinterClient", "shouldn't reach here, yo");
            break;
        }
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        request.setURI(new URI(uri));
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String json = Html.fromHtml(EntityUtils.toString(entity)).toString();
        JSONTokener tokener = new JSONTokener(json);
        JSONArray jsonArray = new JSONArray(tokener);
        List<Printer> printers = new ArrayList<Printer>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject o = jsonArray.getJSONObject(i);
            Printer printer = new Printer(o.getString("name"),
                    o.getString("section_header"), o.getString("location"),
                    o.getString("building"), o.getBoolean("atResidence"),
                    o.getInt("status"), o.getDouble("distance"));
            printers.add(printer);
        }
        return printers;
    }

}
