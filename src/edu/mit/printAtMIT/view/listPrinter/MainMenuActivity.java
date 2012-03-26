package edu.mit.printAtMIT.view.listPrinter;

import com.parse.Parse;

import android.app.Activity;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import edu.mit.printAtMIT.R;
import edu.mit.printAtMIT.view.main.SettingsActivity;
import edu.mit.printAtMIT.view.print.PrintMenuActivity;

/**
 * Show "print" and "view printers" buttons.
 * Menu buttons:
 *      Setting
 *      About
 */
public class MainMenuActivity extends TabActivity{
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        Log.i("PrinterListActivity", "Calling onCreate()");

        Parse.initialize(this, "KIb9mNtPKDtkDk7FJ9W6b7MiAr925a10vNuCPRer",
                "dSFuQYQXSvslh9UdznzzS9Vb0kDgcKnfzgglLUHT");

        setContentView(R.layout.home_screen);
    	
        TabHost tabHost = getTabHost();  // The activity TabHost
        
        addTab(tabHost, PrinterListActivity.ALL_PRINTERS, R.drawable.all_tab);
        addTab(tabHost, PrinterListActivity.CAMPUS_PRINTERS, R.drawable.campus_tab);
        addTab(tabHost, PrinterListActivity.DORM_PRINTERS, R.drawable.dorm_tab);

        tabHost.setCurrentTab(0);
        
    	Button settingsButton = (Button) findViewById(R.id.settings_icon);
    	Button printButton = (Button) findViewById(R.id.printer_icon);
    	Button listButton = (Button) findViewById(R.id.list_icon);
    	
    	settingsButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(),
						SettingsActivity.class);
				startActivity(intent);
			}
		});
    	
    	printButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(),
						PrintMenuActivity.class);
				startActivity(intent);
			}
		});
    	
    	listButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				TabHost tabHost = getTabHost();
				tabHost.setCurrentTab(0);
			}
		});
    }
    
    /**
     * Sets the tab view
     * @param tabHost
     * @param label
     * @param drawableId
     */
    private void addTab(TabHost tabHost, int type, int drawableId) {
    	Intent intent;
    	switch (type) {
    	case PrinterListActivity.ALL_PRINTERS: intent = new Intent(this, PrinterListActivity.class); break;
    	case PrinterListActivity.CAMPUS_PRINTERS: intent = new Intent(this, PrinterListCampusActivity.class); break;
    	case PrinterListActivity.DORM_PRINTERS: intent = new Intent(this, PrinterListDormActivity.class); break;
    	default: intent = new Intent(this, PrinterListActivity.class); break;
    	}

    	TabHost.TabSpec spec = tabHost.newTabSpec(""+type);

    	View tabIndicator = LayoutInflater.from(this).inflate(R.layout.tab_indicator, getTabWidget(), false);

    	ImageView icon = (ImageView) tabIndicator.findViewById(R.id.icon);
    	icon.setImageResource(drawableId);

    	spec.setIndicator(tabIndicator);
    	spec.setContent(intent);

    	tabHost.addTab(spec);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.setting:
        	Intent intent = new Intent(findViewById(android.R.id.content).getContext(), SettingsActivity.class);
        	startActivity(intent);
            return true;
        case R.id.about:
	    	Dialog dialog = new Dialog(this);
	    	dialog.setContentView(R.layout.about_dialog);
	    	dialog.setTitle("About");
	    	dialog.show();
	    		
	    	TextView tv = (TextView) dialog.findViewById(R.id.about_text);
	    	Linkify.addLinks(tv, Linkify.ALL);
	    	tv.setMovementMethod(LinkMovementMethod.getInstance());
	    	
            super.onOptionsItemSelected(item);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
