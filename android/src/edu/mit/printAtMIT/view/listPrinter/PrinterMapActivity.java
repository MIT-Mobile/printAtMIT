package edu.mit.printAtMIT.view.listPrinter;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import edu.mit.printAtMIT.R;
import edu.mit.printAtMIT.controller.client.PrinterClient;
import edu.mit.printAtMIT.controller.client.PrinterClientException;
import edu.mit.printAtMIT.model.printer.Printer;
import edu.mit.printAtMIT.model.printer.SortType;
import edu.mit.printAtMIT.model.printer.StatusType;
import edu.mit.printAtMIT.view.main.SettingsActivity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Shows map of printers, color coded by status
 * 
 * Menu Items: Home About Refresh
 */
public class PrinterMapActivity extends MapActivity {
    public static final String TAG = "PrinterMapActivity";
    public static final int MIT_CENTER_LAT = 42359425;
    public static final int MIT_CENTER_LONG = -71094735;
    LinearLayout linearLayout;
    MapView mapView;
    List<Overlay> mapOverlays;
    Drawable drawable;
    PrinterItemizedOverlay itemizedOverlay;
    FixedMyLocationOverlay myLocationOverlay;
    public boolean tapped_overlay = false;
    private boolean allView;
    private int centerLat;
    private int centerLong;
    private Bundle extras;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        extras = getIntent().getExtras();
        allView = extras.getBoolean("allPrinterView", false);

        centerLat = MIT_CENTER_LAT;
        centerLong = MIT_CENTER_LONG;

        RefreshTask task = new RefreshTask();
        if (allView) {
            task.execute((String) null);
        } else {
            task.execute(extras.getString("id"));
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent intent;
        switch (item.getItemId()) {
        case R.id.refresh:
            RefreshTask task = new RefreshTask();
            if (allView) {
                task.execute((String) null);
            } else {
                task.execute(extras.getString("id"));
            }
            return true;
        case R.id.home:
            intent = new Intent(
                    findViewById(android.R.id.content).getContext(),
                    MainMenuActivity.class);
            intent.putExtra("activity", "home_button");

            startActivity(intent);
            return true;
        case R.id.setting:
            intent = new Intent(
                    findViewById(android.R.id.content).getContext(),
                    SettingsActivity.class);
            startActivity(intent);
            return true;
        case R.id.about:
            showAboutDialog();
            super.onOptionsItemSelected(item);
            return true;
        case R.id.list:
            intent = new Intent(
                    findViewById(android.R.id.content).getContext(),
                    PrinterListActivity.class);
            intent.putExtra(PrintListMenuActivity.LIST_TYPE,
                    PrintListMenuActivity.LIST_ALL);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showAboutDialog() {
        showDialog(0);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.about_dialog);
        dialog.setTitle("About");
        TextView tv = (TextView) dialog.findViewById(R.id.about_text);
        Linkify.addLinks(tv, Linkify.ALL);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        return dialog;
    }

    private void refresh(List<Printer> objects) {
        // if (allView) {
        // setPrinterList(query, null);
        // } else {
        // String id = extras.getString("id");
        // ParseObject printer;
        // try {
        // printer = query.get(id);
        // centerLat = Integer.parseInt(printer.getString("latitude"));
        // centerLong = Integer.parseInt(printer.getString("longitude"));
        // mPrinters.add(printer);
        // } catch (ParseException e) {
        // Log.e("PrinterMapActivity", "Parse query.get(id) FAIL");
        // e.printStackTrace();
        // }
        // }
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.map_green_pin);

        myLocationOverlay = new FixedMyLocationOverlay(this, mapView);
        mapOverlays.add(myLocationOverlay);
        mapView.postInvalidate();

        itemizedOverlay = new PrinterItemizedOverlay(drawable, this, mapView);

        MapController controller = mapView.getController();

        // make mapview start at MIT if allView, else animate to selected
        // printer loc
        if (allView) {
            if (myLocationOverlay.getMyLocation() != null) {
                centerLat = myLocationOverlay.getMyLocation().getLatitudeE6();
                centerLong = myLocationOverlay.getMyLocation().getLongitudeE6();
            }
            controller.setCenter(new GeoPoint(centerLat, centerLong));

        } else {
            if (objects != null && objects.size() > 0) {
                centerLat = objects.get(0).getLatitude();
                centerLong = objects.get(0).getLongitude();
            }
        }
        controller.setZoom(17);
        controller.animateTo(new GeoPoint(centerLat, centerLong));

        Collections.sort(objects, new Comparator<Printer>() {

            // public int compare(ParseObject obj1, ParseObject obj2) {
            // return Integer.parseInt(obj1.getString("status"))
            // - Integer.parseInt(obj2.getString("status"));
            // }

            @Override
            public int compare(Printer lhs, Printer rhs) {
                // TODO Auto-generated method stub
                return lhs.getStatus() - rhs.getStatus();
            }

        });
        // add printer overlayitems to map
        for (Printer printer : objects) {
            // GeoPoint point = new GeoPoint(Integer.parseInt(printer
            // .getString("latitude")), Integer.parseInt(printer
            // .getString("longitude")));
            GeoPoint point = new GeoPoint(printer.getLatitude(),
                    printer.getLongitude());
            PrinterOverlayItem item = new PrinterOverlayItem(point,
                    printer.getName() + " (" + printer.getLocation() + ")",
                    "Status: " + getStatusString(printer.getStatus()),
                    printer.getName());
            StatusType status = getStatus(printer.getStatus());
            if (status.equals(StatusType.BUSY)) {
                drawable = this.getResources().getDrawable(
                        R.drawable.map_yellow_pin);
            } else if (status.equals(StatusType.ERROR)) {
                drawable = this.getResources().getDrawable(
                        R.drawable.map_red_pin);
            } else {
                drawable = this.getResources().getDrawable(
                        R.drawable.map_green_pin);
            }
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight());
            item.setMarker(drawable);
            itemizedOverlay.addOverlay(item);
        }

        mapOverlays.add(itemizedOverlay);
    }

    /**
     * Makes request to Parse to retrieve list of all printers. Sets mPrinters
     * to that list.
     * 
     * @param query
     *            : ParseQuery id: ParseId of printer, null if viewing all
     *            printers
     */
    private List<Printer> getData(String id) {
        List<Printer> objects = new ArrayList<Printer>();
        if (id == null) {
            try {
                objects = PrinterClient.getAllPrinterObjects(SortType.NAME, 0,
                        0);
            } catch (PrinterClientException e) {
                Log.e(TAG, "getData(id==null) fail");
                e.printStackTrace();
            }

        } else {
            try {
                Printer printer = PrinterClient.getPrinterObject(id);
                objects.add(printer);
            } catch (PrinterClientException e) {
                Log.e(TAG, "getData(id) fail");
                e.printStackTrace();
            }
        }
        return objects;

    }

    private StatusType getStatus(int code) {
        switch (code) {
        case 0:
            return StatusType.READY;
        case 1:
            return StatusType.BUSY;
        case 2:
            return StatusType.ERROR;
        default:
            Log.e(TAG, "shouldn't get here, yo");
            break;
        }
        return null;
    }
    
    private String getStatusString(int x) {
        if (x == 0) {
            return "Ready";
        } else if (x == 1) {
            return "Busy";
        } else {
            return "Error";
        }
    }

    /**
     * Checks to see if user is connected to wifi or 3g
     * 
     * @return
     */
    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) PrinterMapActivity.this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null) {

            networkInfo = connectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (!networkInfo.isAvailable()) {
                networkInfo = connectivityManager
                        .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            }
        }
        return networkInfo == null ? false : networkInfo.isConnected();
    }

    /**
     * Background task for refreshing parse data.
     */
    public class RefreshTask extends AsyncTask<String, byte[], List<Printer>> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "RefreshTask onPreExecute");
            dialog = ProgressDialog.show(PrinterMapActivity.this, "",
                    "Refreshing Data", true);
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG, "RefreshTask Cancelled.");
        }

        @Override
        protected List<Printer> doInBackground(String... params) {
            if (isConnected()) {
                return getData(params[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Printer> result) {
            if (result == null) {
                result = new ArrayList<Printer>();
            }
            if (result.size() == 0) {
                Toast.makeText(getApplicationContext(),
                        "Error getting data, please try again later",
                        Toast.LENGTH_SHORT).show();
                Log.i(TAG,
                        "RefreshTask onPostExecute: Completed with an Error.");
            }
            refresh(result);
            dialog.dismiss();

        }

    }
}
