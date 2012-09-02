package edu.mit.printAtMIT.controller.client;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import edu.mit.printAtMIT.PrintAtMITActivity;
import edu.mit.printAtMIT.model.printer.ListType;
import edu.mit.printAtMIT.model.printer.Printer;
import edu.mit.printAtMIT.model.printer.PrintersDbAdapter;
import edu.mit.printAtMIT.model.printer.SortType;
import edu.mit.printAtMIT.view.list.Item;
import edu.mit.printAtMIT.view.list.PrinterEntryItem;
import edu.mit.printAtMIT.view.list.SectionItem;

public class PrinterClient {
    public static final String TAG = "PRINTERCLIENT";
    public static final String ALL_URL = "https://mobile-print-dev.mit.edu/printatmit/query_result/?sort=%s&latitude=%f&longitude=%f";
    public static final String PRINTER_QUERY_URL = "https://mobile-print-dev.mit.edu/printatmit/query_result/?printer_query=%s";
    public static final String NAME_SORT = "name";
    public static final String BUILDING_SORT = "building";
    public static final String DISTANCE_SORT = "distance";


    /**
     * @param context
     *      context where list is to be rendered
     * @param type
     * @param objects
     *      printer objects retrieved from the server
     * @return
     *      a list of Item to be used for setting list layout
     */
    public static List<Item> getPrinterItemList(Context context, ListType type,
            List<Printer> objects) {
        HashMap<String, PrinterEntryItem> curr_map = new HashMap<String, PrinterEntryItem>();
        HashMap<String, PrinterEntryItem> all_map = new HashMap<String, PrinterEntryItem>();
        ArrayList<Item> itemsList = new ArrayList<Item>();
        if (objects != null && objects.size() > 0) {
            curr_map = new HashMap<String, PrinterEntryItem>();
            all_map = new HashMap<String, PrinterEntryItem>();
            String currSectHeader = "";
            for (Printer o : objects) {
                Log.i(TAG, o.getSectionHeader());
                // COMMON NAME
                StringBuilder location = new StringBuilder(o.getLocation());
                if (o.getBuilding() != null && o.getBuilding().length() != 0) {
                    location.append("#" + o.getBuilding());
                }
                all_map.put(o.getName(), new PrinterEntryItem(o.getName(),
                        location.toString(), o.getStatus()));

                if (type == ListType.ALL) {
                    PrinterEntryItem item = new PrinterEntryItem(o.getName(),
                            location.toString(), o.getStatus());
                    if (!currSectHeader.equals(o.getSectionHeader())) {

                        // TODO
                        // different section header ui?
                        itemsList.add(new SectionItem(o.getSectionHeader()));
                        currSectHeader = o.getSectionHeader();
                    }
                    itemsList.add(item);
                    curr_map.put(o.getName(), item);
                } else if (type == ListType.DORM) {
                    if (o.atResidence()) {
                        PrinterEntryItem item = new PrinterEntryItem(
                                o.getName(), location.toString(), o.getStatus());
                        if (!currSectHeader.equals(o.getSectionHeader())) {

                            // TODO
                            // different section header ui?
                            itemsList
                                    .add(new SectionItem(o.getSectionHeader()));
                            currSectHeader = o.getSectionHeader();
                        }
                        itemsList.add(item);

                        curr_map.put(o.getName(), item);
                    }
                } else if (type == ListType.CAMPUS) {
                    if (!o.atResidence()) {
                        PrinterEntryItem item = new PrinterEntryItem(
                                o.getName(), location.toString(), o.getStatus());

                        if (!currSectHeader.equals(o.getSectionHeader())) {

                            // TODO
                            // different section header ui?
                            itemsList
                                    .add(new SectionItem(o.getSectionHeader()));
                            currSectHeader = o.getSectionHeader();
                        }
                        itemsList.add(item);

                        curr_map.put(o.getName(), item);
                    }
                } else {
                    PrinterEntryItem item = new PrinterEntryItem(o.getName(),
                            location.toString(), o.getStatus());
                    if (!currSectHeader.equals(o.getSectionHeader())) {

                        // TODO
                        // different section header ui?
                        itemsList.add(new SectionItem(o.getSectionHeader()));
                        currSectHeader = o.getSectionHeader();
                    }
                    itemsList.add(item);

                    curr_map.put(o.getName(), item);
                }
            }
        } else {
            // handle error
            curr_map = new HashMap<String, PrinterEntryItem>();
        }

        final ArrayList<Item> items = new ArrayList<Item>();
        ArrayList<Item> printers = null;
        if (type == ListType.FAVORITE) {
            PrintersDbAdapter mDbAdapter = new PrintersDbAdapter(context);
            mDbAdapter.open();
            List<String> ids = mDbAdapter.getFavorites();
            printers = new ArrayList<Item>();
            for (String id : ids) {
                if (all_map.containsKey(id)) {
                    printers.add(all_map.get(id));
                }
            }
            mDbAdapter.close();

        } else {
            // printers = new ArrayList<PrinterEntryItem>(curr_map.values());
            printers = itemsList;

        }

        switch (type) {
        case ALL:
            items.add(new SectionItem("All Printers"));
            break;
        case FAVORITE:
            items.add(new SectionItem("Favorites"));
            break;
        case CAMPUS:
            items.add(new SectionItem("Campus Printers"));
            break;
        case DORM:
            items.add(new SectionItem("Dorm Printers"));
            break;
        default:
            items.add(new SectionItem("All Printers"));
            break;
        }

        if (printers.size() == 0) {
            if (type == ListType.FAVORITE) {
                items.clear();
            } else {
                items.clear();
                items.add(new SectionItem("Check internet connection"));
            }
        }

        for (Item item : printers) {
            items.add(item);
        }

        return items;
    }

    /**
     * Retrieves list of sorted printers from database
     * 
     * @param sorttype
     * @param latitude
     * @param longitude
     * @return
     * @throws PrinterClientException
     */
    public static List<Printer> getAllPrinterObjects(SortType sorttype,
            double latitude, double longitude) throws PrinterClientException {
        List<Printer> printers = new ArrayList<Printer>();

        try {
            printers = requestPrinterList(sorttype, latitude, longitude);
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

    private static List<Printer> requestPrinterList(SortType sorttype,
            double latitude, double longitude) throws URISyntaxException,
            ClientProtocolException, IOException, JSONException {
        String uri = "";

        switch (sorttype) {
        case NAME:
            uri = String.format(ALL_URL, "name", (float)0, (float)0);
            break;
        case BUILDING:
            uri = String.format(ALL_URL, "building", (float)0, (float)0);
            break;
        case DISTANCE:
            uri = String.format(ALL_URL, "distance", latitude, longitude);
            break;
        default:
            Log.e("PrinterClient", "shouldn't reach here, yo");
            break;
        }
        HttpGet request = new HttpGet();
        request.setURI(new URI(uri));
        HttpResponse response = (PrintAtMITActivity.HTTP_CLIENT).execute(request);
        HttpEntity entity = response.getEntity();
        String json = Html.fromHtml(EntityUtils.toString(entity)).toString();
        JSONTokener tokener = new JSONTokener(json);
        JSONArray jsonArray = new JSONArray(tokener);
        List<Printer> printers = new ArrayList<Printer>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject o = jsonArray.getJSONObject(i);
            Printer printer = new Printer(o.getString("name"),
                    o.getString("section_header"), o.getString("location"),
                    o.getString("building_name"), o.getBoolean("atResidence"),
                    o.getInt("status"), o.getInt("latitude"),
                    o.getInt("longitude"), o.getDouble("distance"));
            printers.add(printer);
        }
        return printers;
    }

    /**
     * Retrieves information about one printer from backend
     * 
     * @param name
     * @return
     * @throws PrinterClientException
     */
    public static Printer getPrinterObject(String name)
            throws PrinterClientException {
        Printer printer = null;
        String uri = String.format(PRINTER_QUERY_URL, name);
        HttpGet request = new HttpGet();
        try {
            request.setURI(new URI(uri));
            HttpResponse response = (PrintAtMITActivity.HTTP_CLIENT).execute(request);
            HttpEntity entity = response.getEntity();
            String json = Html.fromHtml(EntityUtils.toString(entity))
                    .toString();
            JSONTokener tokener = new JSONTokener(json);
            JSONArray jsonArray = new JSONArray(tokener);
            if (jsonArray.length() > 0) {
                JSONObject object = jsonArray.getJSONObject(0);
                printer = new Printer(object.getString("name"),
                        object.getString("section_header"),
                        object.getString("location"),
                        object.getString("building_name"),
                        object.getBoolean("atResidence"),
                        object.getInt("status"), object.getInt("latitude"),
                        object.getInt("longitude"),
                        object.getDouble("distance"));
            } else {
                Log.e("PrinterClientActivity", "json array length is 0");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new PrinterClientException("URISyntaxException");

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            throw new PrinterClientException("ClientProtocolException");

        } catch (IOException e) {
            e.printStackTrace();
            throw new PrinterClientException("IOException");

        } catch (JSONException e) {
            e.printStackTrace();
            throw new PrinterClientException("JSONException");

        }
        return printer;

    }

}
