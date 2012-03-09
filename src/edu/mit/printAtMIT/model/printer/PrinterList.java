package edu.mit.printAtMIT.model.printer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.content.Context;

import com.parse.Parse;
import com.parse.ParseObject;

import edu.mit.printAtMIT.view.list.Item;
import edu.mit.printAtMIT.view.list.PrinterEntryItem;
import edu.mit.printAtMIT.view.list.SectionItem;

/**
 * Class for grabbing lists of printers of certain types, such as all printers, campus, or dorm printers.
 * 
 * @author Steph
 *
 */
public class PrinterList {

	private final ListType type;
	private Context context;
	
	public PrinterList(Context context, ListType type) {
		this.type = type;
		this.context = context;
	}
	
	/**
	 * Returns an ArrayList of printers as Item objects, ready to be displayed in the View.
	 * 
	 * @param objects
	 * @return ArrayList of printers
	 */
	public ArrayList<Item> getList(List<ParseObject> objects) {
		Parse.initialize(this.context, "KIb9mNtPKDtkDk7FJ9W6b7MiAr925a10vNuCPRer",
                "dSFuQYQXSvslh9UdznzzS9Vb0kDgcKnfzgglLUHT");
		
		HashMap<String, PrinterEntryItem> curr_map = new HashMap<String, PrinterEntryItem>();
		HashMap<String, PrinterEntryItem> all_map = new HashMap<String, PrinterEntryItem>();
		
		if (objects != null) {
            curr_map = new HashMap<String, PrinterEntryItem>();
            all_map = new HashMap<String, PrinterEntryItem>();
            for (ParseObject o : objects) {
                //COMMON NAME
                StringBuilder location = new StringBuilder(o.getString("location"));
                if (o.getString("commonName") != null && o.getString("commonName").length() != 0) {
                    location.append("#" + o.getString("commonName"));
                }
                all_map.put(o.getObjectId(), new PrinterEntryItem(o.getObjectId(),
                            o.getString("printerName"), location.toString(),
                            Integer.parseInt(o.getString("status"))));
                if (this.type == ListType.ALL){ 
            		PrinterEntryItem item = new PrinterEntryItem(o.getObjectId(),
                            o.getString("printerName"), location.toString(),
                            Integer.parseInt(o.getString("status")));
                    curr_map.put(o.getObjectId(), item);
            	}
                else if (this.type == ListType.DORM) {
                	if (o.getBoolean("residence")) {
            			PrinterEntryItem item = new PrinterEntryItem(o.getObjectId(),
                                o.getString("printerName"), location.toString(),
                                Integer.parseInt(o.getString("status")));
                        curr_map.put(o.getObjectId(), item);
            		}
            	}
                else if (this.type == ListType.CAMPUS) {
                	if (!o.getBoolean("residence")) {
            			PrinterEntryItem item = new PrinterEntryItem(o.getObjectId(),
                                o.getString("printerName"), location.toString(),
                                Integer.parseInt(o.getString("status")));
                        curr_map.put(o.getObjectId(), item);
            		}
            	}
            	else {
            		PrinterEntryItem item = new PrinterEntryItem(o.getObjectId(),
                            o.getString("printerName"), location.toString(),
                            Integer.parseInt(o.getString("status")));
                    curr_map.put(o.getObjectId(), item);
            	}
            }
        } else {
            //handle error
            curr_map = new HashMap<String, PrinterEntryItem>();
        }

        final ArrayList<Item> items = new ArrayList<Item>();
        ArrayList<PrinterEntryItem> printers = null;
        if (this.type == ListType.FAVORITE) {
        	PrintersDbAdapter mDbAdapter = new PrintersDbAdapter(this.context);
        	mDbAdapter.open();
            List<String> ids = mDbAdapter.getFavorites();
            printers = new ArrayList<PrinterEntryItem>();
            for (String id : ids) {
                if (all_map.containsKey(id)) {
                    printers.add(all_map.get(id));
                }
            }
            mDbAdapter.close();

        }
        else {
            printers = new ArrayList<PrinterEntryItem>(curr_map.values());

        }
        
        Collections.sort(printers, new PrinterComparator());

        if (printers.size() == 0) {
        	if (this.type == ListType.FAVORITE) {
        		items.add(new SectionItem("No favorites to display"));
        	}
        	else {
        		items.add(new SectionItem("Check internet connection"));
        	}
        }
        for (PrinterEntryItem item : printers) {
            items.add(item);
        }
        
        return items;
	}
}
