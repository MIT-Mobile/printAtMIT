package edu.mit.printAtMIT.view.listPrinter;

import java.util.ArrayList;

import edu.mit.printAtMIT.R;
import edu.mit.printAtMIT.view.list.EntryAdapter;
import edu.mit.printAtMIT.view.list.EntryItem;
import edu.mit.printAtMIT.view.list.Item;
import edu.mit.printAtMIT.view.list.SectionItem;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

public class TestActivity extends ListActivity {
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.printer_list);
        
        final ArrayList<Item> items = new ArrayList<Item>();
        
        items.add(new SectionItem("Testing header"));
        items.add(new EntryItem("TESTONE", "subtitle", 1));
        items.add(new EntryItem("TESTTWO", "subtitle", 2));
        items.add(new EntryItem("TESTTHREE", "subtitle", 3));
        
        EntryAdapter adapter = new EntryAdapter(this, (ArrayList<Item>) items);
        setListAdapter(adapter);

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
    }
}
