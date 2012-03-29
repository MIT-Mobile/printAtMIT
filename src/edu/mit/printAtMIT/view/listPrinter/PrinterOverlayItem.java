package edu.mit.printAtMIT.view.listPrinter;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class PrinterOverlayItem extends OverlayItem {

    private String printerName;
    
    public PrinterOverlayItem(GeoPoint point, String title, String snippet, String printerName) {
        super(point, title, snippet);
        this.printerName = printerName;
    }
    
    public String getParseId() {
        return printerName;
    }

}
