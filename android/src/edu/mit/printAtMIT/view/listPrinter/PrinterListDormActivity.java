package edu.mit.printAtMIT.view.listPrinter;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import edu.mit.printAtMIT.R;
import edu.mit.printAtMIT.controller.client.PrinterClient;
import edu.mit.printAtMIT.controller.client.PrinterClientException;
import edu.mit.printAtMIT.model.printer.ListType;
import edu.mit.printAtMIT.model.printer.Printer;
import edu.mit.printAtMIT.model.printer.SortType;
import edu.mit.printAtMIT.view.list.EntryAdapter;
import edu.mit.printAtMIT.view.list.Item;
import edu.mit.printAtMIT.view.list.PrinterEntryItem;
import edu.mit.printAtMIT.view.listPrinter.PrinterListCampusActivity.RefreshListTask;

/**
 * Lists all the printers from database. Shows name, location, status from each
 * printer List of favorite printers on top, then list of all printers
 * 
 * Menu Item: Settings About Home Refresh
 * 
 * Context Menu Items: Favorite, Info, MapView
 */

public class PrinterListDormActivity extends ListActivity implements LocationListener {
    public static final String TAG = "PrinterListActivity";
//    private static final String REFRESH_ERROR = "Error connecting to network, please try again later";
//    private static final int REFRESH_ID = Menu.FIRST;

    private ProgressDialog mProgressDialog;
    private LocationManager mlocationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.printer_list);
        mlocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (mlocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mlocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 20 * 1000, 10, this);
        }
        if (mlocationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mlocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 2*1000, 10, this);
        }
        
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMessage("Loading Printer Data");
        RefreshListTask task = new RefreshListTask();
        if (isConnected(this)) {
            task.execute(SortType.NAME);

        } else {
            Toast.makeText(this, "Internet Error", Toast.LENGTH_SHORT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20*1000, 10, this);
        mlocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2*1000, 10, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mlocationManager.removeUpdates(this);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.printlist_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh:
            RefreshListTask task = new RefreshListTask();
            if (isConnected(this)) {
                // uncomment for setting location when sorting by distance
                // task.setLocation(latitude, longitude)
                mProgressDialog.setMessage("Refreshing Printer Data");
                task.execute(SortType.NAME);

            } else {
                Toast.makeText(this, "Internet Error", Toast.LENGTH_SHORT);
            }
            return true;
		case R.id.about:
			showAboutDialog();
			super.onOptionsItemSelected(item);
			return true;
		case R.id.name_sort:
            RefreshListTask task1 = new RefreshListTask();
            if (isConnected(this)) {
                mProgressDialog.setMessage("Refreshing Printer Data");
                task1.execute(SortType.NAME);

            } else {
                Toast.makeText(this, "Internet Error", Toast.LENGTH_SHORT);
            }           
            return true;
        case R.id.building_sort:
            RefreshListTask task2 = new RefreshListTask();
            if (isConnected(this)) {
                mProgressDialog.setMessage("Refreshing Printer Data");
                task2.execute(SortType.BUILDING);

            } else {
                Toast.makeText(this, "Internet Error", Toast.LENGTH_SHORT);
            }
            return true;
        case R.id.location_sort:
            Log.i(TAG, "getting location");
            RefreshListTask task3 = new RefreshListTask();
            if (isConnected(this)) {
                mProgressDialog.setMessage("Refreshing Printer Data");
                task3.execute(SortType.DISTANCE);

            } else {
                Toast.makeText(this, "Internet Error", Toast.LENGTH_SHORT);
            }
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
    /**
     * Sets Views Should be called in UI thread
     */
    private void setListViewData(List<Printer> objects) {
    	final List<Item> items = PrinterClient.getPrinterItemList(this, ListType.DORM, objects);
        EntryAdapter adapter = new EntryAdapter(this, (ArrayList<Item>) items);
        setListAdapter(adapter);

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Intent intent = new Intent(view.getContext(),
                        PrinterInfoActivity.class);

                if (!items.get(position).isSection()) {
                    intent.putExtra("id",
                            ((PrinterEntryItem) items.get(position)).printerName);
                }

                startActivity(intent);
            }

        });
        Log.i(TAG, "end of fillListData()");
    }

    /**
     * Background task that refreshes the hashmap of printers. Modifies map.
     */
    public class RefreshListTask extends
            AsyncTask<SortType, byte[], List<Printer>> {
        private double latitude;
        private double longitude;
        
        @Override
        protected void onPreExecute() {
            Log.i(TAG, "RefreshTask onPreExecute");
            mProgressDialog.show();

        }

        @Override
        protected List<Printer> doInBackground(SortType... arg0) {
            // happens in background thread
            
            List<Printer> objects = null;
            try {
                //get gps coords
                if (arg0[0].equals(SortType.DISTANCE)) {
                    Location loc = mlocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (loc == null) {
                        loc = mlocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                    Log.i("COORDINATES ", latitude + ", " + longitude);
                }
                objects = PrinterClient.getAllPrinterObjects(arg0[0],
                        this.latitude, this.longitude);
            } catch (PrinterClientException e) {
                // e.printStackTrace();
                Log.e(TAG, "PrinterClient exception in refresh list task");
            }
            return objects;
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG, "RefreshTask Cancelled.");
        }

        @Override
        protected void onPostExecute(List<Printer> objects) { // happens in
                                                                  // UI thread
            // Bad practice, but meh, it'd be better if java had tuples
            if (objects == null) {
                Toast.makeText(getApplicationContext(),
                        "Error getting data, please try again later",
                        Toast.LENGTH_SHORT).show();
                Log.i(TAG,
                        "RefreshHashMapTask onPostExecute: Completed with an Error.");
            }
            setListViewData(objects);

            mProgressDialog.dismiss();
        }
        
        /**
         * Called when sorting by distance
         * 
         * @param latitude
         * @param longitude
         */
//        protected void setLocation(double latitude, double longitude) {
//            this.latitude = latitude;
//            this.longitude = longitude;
//        }
    }

    /**
     * Checks to see if user is connected to wifi or 3g
     * 
     * @return
     */
    private boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
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

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        
    }
}
