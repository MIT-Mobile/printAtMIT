package edu.mit.printAtMIT.model.printer;

import java.util.Comparator;

import edu.mit.printAtMIT.view.list.PrinterEntryItem;

// Comparator to sort printers alphabetically
public class PrinterComparator implements Comparator<PrinterEntryItem> {

	public int compare(PrinterEntryItem item1, PrinterEntryItem item2) {
		return item1.printerName.compareTo(item2.printerName);
	}

}
