package edu.mit.printAtMIT.controller.client;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.parse.ParseObject;

import edu.mit.printAtMIT.model.printer.ListType;
import edu.mit.printAtMIT.model.printer.PrinterList;
import edu.mit.printAtMIT.view.list.Item;

public class PrinterClient {
	
	/**
	 * Returns a list of printers of type ListType as Item objects, ready to be added to the view
	 * 
	 * @param context, Context of the activity calling the method
	 * @param type, type of printer list requested (all, campus, dorm)
	 * @param objects, list of Parse Objects
	 * @return ArrayList of Items
	 */
	public static ArrayList<Item> getPrinterList(Context context, ListType type, List<ParseObject> objects) {
		PrinterList printerList = new PrinterList(context, type);
		return printerList.getList(objects);
	}
}
