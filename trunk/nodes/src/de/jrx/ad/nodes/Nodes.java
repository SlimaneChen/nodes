package de.jrx.ad.nodes;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import de.jrx.ad.nodes.service.MapperService;

public class Nodes extends ActivityGroup {
	
	public static final int MENU_QUIT = Menu.FIRST+1;
	public static final int MENU_INFO = Menu.FIRST+2;
	public static final int MENU_SETTINGS = Menu.FIRST+3;
	public static Application app;
	
	private MyReceiver receiver;

	 TabHost mTabHost;
	private ImageView imgServer;
	
	private void setupTabHost() {
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup(this.getLocalActivityManager());
	}
	
	/** Initialize all Instances **/
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.nodes);
	    
	    //Share Context for the Preferences
	    app = getApplication();
	    
	    setupTabHost();
	    mTabHost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);
	    
	    Intent intent;

	    intent = new Intent().setClass(this, NodesBookmarks.class);
		setupTab("Bookmarks", intent);

	    intent = new Intent().setClass(this, NodesDiscovery.class);
		setupTab("Discover", intent);

		imgServer = (ImageView)findViewById(R.id.server); 
		isMapperServiceRunning();
		displayEULA();
		restoreMe(savedInstanceState);
	}
	
	/** Save and restore the Activity State **/
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mTabHost!=null) {
			outState.putString("currenttab", mTabHost.getCurrentTabTag());
		}
	}

	private void restoreMe(Bundle state) {

		if (state!=null) {
			String currenttab=state.getString("currenttab");

			if (mTabHost!=null) {
				mTabHost.setCurrentTabByTag(currenttab);
			}
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
//		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
//		TODO: Restart Background Service
		
		if (isMapperServiceRunning() == true) {
			imgServer.setImageResource(R.drawable.title_server_on);
		} else {
			imgServer.setImageResource(R.drawable.title_server_off);
		}
		
	}
	
	
	/** Check if the user accepted the EULA **/
	public void displayEULA() {
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(app);
		if (prefs.getBoolean("eula", false) == false) {

	    	 new AlertDialog.Builder(this)
				.setTitle(R.string.eula)
				.setMessage(R.string.eula_content)
				.setPositiveButton(R.string.accept,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
						int whichButton) {
							SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(app);
							SharedPreferences.Editor editor = prefs.edit();
							editor.putBoolean("eula", true);
							editor.commit();
						}
				})
				.setNegativeButton(R.string.decline,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
						int whichButton) {
							finish();
						}
				})
				.show();
		}
	}
	
	/** Display the Info Screen **/
	public void displayInfo() {

	    	 new AlertDialog.Builder(this)
				.setTitle(R.string.info)
				.setMessage(R.string.info_content)
				.setIcon(R.drawable.icon)
				.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
						int whichButton) {

					}
				})
				.show();
	}
	
	private void setupTab(final String tag, Intent intent) {
		View tabview = createTabView(mTabHost.getContext(), tag);
		
		TabSpec setContent = mTabHost.newTabSpec(tag).setIndicator(tabview).setContent(intent);		
		mTabHost.addTab(setContent);

	}

	private static View createTabView(final Context context, final String text) {
		View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
		TextView tv = (TextView) view.findViewById(R.id.tabsText);
		tv.setText(text);
		return view;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, "Settings");
		menu.add(Menu.NONE, MENU_INFO, Menu.NONE, "Info");
		menu.add(Menu.NONE, MENU_QUIT, Menu.NONE, "Quit");

		return(super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_SETTINGS:
				startActivity(new Intent(this, Settings.class));
				return(true);
			case MENU_INFO:
				displayInfo();
				return(true);
			case MENU_QUIT:
				Intent i=new Intent(this, MapperService.class);
				stopService(i);
				finish();
				return(true);
		}

		return(super.onOptionsItemSelected(item));
	}
	
	/** Buttons **/
	public void visitNodesList(View v) {
		startActivity(new Intent(this, Nodes.class));
	}
	public void visitBrowser(View v) {
		startActivity(new Intent(this, Browser.class));
	}
	
	/** Broadcast Receiver **/
	public void startMapperService(View v) {
		Intent i=new Intent(this, MapperService.class);
		if (isMapperServiceRunning() == false) {
			
			SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(app);
			i.putExtra(MapperService.EXTRA_PORT, new Integer(prefs.getString("port", "8080")).intValue());
			startService(i);
			
		} else {
			stopService(i);
		}
	}
	
	private boolean isMapperServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("de.jrx.ad.nodes.service.MapperService".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	// Setup a BroadcastReceiver for the MapperService
	protected void onPause(){
		super.onPause();
		unregisterReceiver(receiver);
	}

	protected void onStart(){
		super.onStart();
		receiver = new MyReceiver(this);
		registerReceiver(receiver, new IntentFilter("de.jrx.ad.nodes.service.START"));
		registerReceiver(receiver, new IntentFilter("de.jrx.ad.nodes.service.STOP"));
		registerReceiver(receiver, new IntentFilter("de.jrx.ad.nodes.service.SOCKET_FAILED"));
	}
	
	public class MyReceiver extends BroadcastReceiver {

	    private Activity activity;
	
	    public MyReceiver(Activity activity) {
	        this.activity = activity;
	    }

	    public void onReceive(Context context, Intent intent) {
	    	
	    	if (intent.getAction().equalsIgnoreCase("de.jrx.ad.nodes.service.START")) {
				imgServer.setImageResource(R.drawable.title_server_on);	    		
	    	}
	    	if (intent.getAction().equalsIgnoreCase("de.jrx.ad.nodes.service.STOP")) {
				imgServer.setImageResource(R.drawable.title_server_off);	    		
				stopService(new Intent(this.activity, MapperService.class));
	    	}
	    	if (intent.getAction().equalsIgnoreCase("de.jrx.ad.nodes.service.SOCKET_FAILED")) {
				imgServer.setImageResource(R.drawable.title_server_off);	    	
				
		    	Toast
		    		.makeText(this.activity, "The socket is already in use.", Toast.LENGTH_LONG)
		    		.show();

	    	}

	    	
	    	Log.i("test",intent.getAction());;
	        if(this.activity == null);
	    }
	    
	}
}
