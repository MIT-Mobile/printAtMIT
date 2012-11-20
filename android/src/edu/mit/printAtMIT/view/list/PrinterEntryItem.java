package edu.mit.printAtMIT.view.list;

import edu.mit.printAtMIT.model.printer.StatusType;
import android.util.Log;

/**
 * Represents a printer list item.
 * Contains printer name, location, status
 */
public class PrinterEntryItem implements Item {

    public final String printerName;
    public final String location;
    public int status;
    
    public static final String BUSY = "Busy";
    public static final String READY = "Available";
    public static final String ERROR = "Error";
    public static final String UNKNOWN = "Unknown";
    
    public PrinterEntryItem(String printerName, String location, int status) {
        this.printerName = printerName;
        this.location = location;
        this.status = status;
        
    }

   // @Override
    public boolean isSection() {
        return false;
    }


    //@Override
    public boolean isButton() {
        return false;
    }
    

   // @Override
    public boolean isPrinterEntry() {
        // TODO Auto-generated method stub
        return true;
    }

    public String getStatusString(StatusType type) {
        String string = "";
        switch(type) {
            case READY: string = PrinterEntryItem.READY; break; //green
            case BUSY: string = PrinterEntryItem.BUSY; break; //yellow
            case ERROR: string = PrinterEntryItem.ERROR; break; //error
            case UNKNOWN: string = PrinterEntryItem.UNKNOWN; break; //grey
            default: Log.e("PrinterEntryItem", "Invalid printer status, thou hast problems"); break;
        }
        return string;
    }
    
    public StatusType getStatus() {
    	int status = this.status;
    	switch(status) {
    	case 0: return StatusType.READY; 
    	case 1: return StatusType.BUSY;
    	case 2: return StatusType.ERROR;
    	case 3: return StatusType.UNKNOWN;
    	default: Log.e("PrinterEntryItem", "Invalid printer status, thou hast problems"); break;
    	}
    	return null;
    }
}
