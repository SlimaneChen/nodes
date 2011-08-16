package de.jrx.ad.nodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import de.jrx.ad.coap.CoreLink;
import de.jrx.ad.coap.CoreLinkParser;
import de.jrx.ad.nodes.service.MapperService;

public class NodesDiscovery extends ListActivity {
	private DatabaseHelper db=null;
	AtomicBoolean isRunning=new AtomicBoolean(false);
	private ListView listview;
	
	ArrayList<String> items=new ArrayList<String>();

	private static final int ADD_ID = Menu.FIRST+1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
        setContentView(R.layout.viewlist_discovery);
                
		listview=getListView();
		listview.setVisibility(android.view.View.INVISIBLE);

		setListAdapter(new ArrayAdapter<String>(this,
				R.layout.row_discovery, R.id.title,
				items));
		
		registerForContextMenu(listview);
		
	}
	
	Handler handler=new Handler() {
		@Override
			public void handleMessage(Message msg) {

		    	List<CoreLink> links=new ArrayList<CoreLink>();
	
		    //	String data = "</data-sink>;ct=40;n=\"POSTed data is stored here (read-only)\",</filestorage>;n=\"a single file, you can PUT things here\",</time>;n=\"server's local time and date (read-only)\",</lipsum>;ct=0;n=\"some large text to test buffer sizes (<EOT> marks its end) (read-only)\",</.well-known/core>;ct=40, bllajskdjasjk";
		    	String data = msg.obj.toString();
		    	links = CoreLinkParser.parse(data);
		    	
		    	for (CoreLink link : links) {
		    		
		    		items.add(link.getURI());
		    		
		    	}
	    	
				Log.i("nodes", msg.obj.toString() );

				listview.setAdapter(getListAdapter());
				
			}
		};

	public void discover(View v) {		

		// Hide the discover button
		//Button btnDiscover=(Button)v.findViewById(R.id.discover);
		//btnDiscover.setVisibility(android.view.View.VISIBLE);

		// Show and clear the list
		items.clear();
		listview.setVisibility(android.view.View.VISIBLE);		
		
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
		final String uri = "http://localhost:"+ prefs.getString("port", "8080") +"/.well-known/core";
		
	 	HttpClient httpclient = new DefaultHttpClient();
	 	
    	if (!isRunning.get()) {
			
    		Log.i("nodes", "try new connections");
			final HttpClient client = httpclient;
		   	Thread background=new Thread(new Runnable() {
	    		public void run() {
	    			if(isRunning.get()) {
	    				
	    				// Check if the proxy is running. If not, start it.
	    				if (isMapperServiceRunning() == false) {
	    					
	    		    		Intent i=new Intent(Nodes.app, MapperService.class);
	    					SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(Nodes.app);
	    					i.putExtra(MapperService.EXTRA_PORT, new Integer(prefs.getString("port", "8080")).intValue());
	    					startService(i);	
	    					try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
	    				}
	    				
		    	    	Looper.prepare();
		    	    	
		    	        try {
		    	   		 
		    				StringBuilder buf=new StringBuilder("");
		    			 	HttpGet httpmethod = new HttpGet(uri);
		    		        HttpResponse response;
		    	    		Log.i("nodes", "connections opened");
		    	        	response = client.execute(httpmethod);
		    	        	
		    	        	if (response.getStatusLine().getStatusCode() == 200) {
			    	            
			    	            HttpEntity entity = response.getEntity();
			    	 
			    	            if (entity != null) {
			    	 
			    	                InputStream instream = entity.getContent();
			    	                String result= Tools.convertStreamToString(instream);
			    	                buf.append(result);	                
			    	                instream.close();
			    	            }

				    	    	Message msg = new Message();
				    	    	msg.obj = buf;
				    	    	handler.sendMessage(msg);
				    	    	
		    	        	}
		    	 
		    	        } catch (ClientProtocolException e) {
		    	            e.printStackTrace();
		    	        } catch (IllegalStateException e) {
		    	            e.printStackTrace();
		    	        } catch (NullPointerException e) {
		    	            e.printStackTrace();
		    	        } catch (IOException e) {
		    	            e.printStackTrace();
		    	        }
	    				
		
	    			}
	    			isRunning.set(false);
	    		}
	    	});
		  
			isRunning.set(true);
		   	background.start();
		  
		   	
		   	
    	} else {
    		isRunning.set(false);	    		
    		httpclient = new DefaultHttpClient();
    		Log.i("nodes", "connections closed");
    	}

		
	}
		
	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		
		TextView uri_label=(TextView)v.findViewById(R.id.title);
		
		Uri uri=Uri.parse(uri_label.getText().toString());
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
		ContextMenu.ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, ADD_ID, Menu.NONE, "Bookmark")
				.setAlphabeticShortcut('a');
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case ADD_ID:
				AdapterView.AdapterContextMenuInfo info=
					(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				
				addNode(info.id, info.targetView);
				return(true);
		}

		return(super.onOptionsItemSelected(item));
	}
	
	public void addNode(final long rowId, View v) {
		LayoutInflater inflater=LayoutInflater.from(this);
		View addView=inflater.inflate(R.layout.list_add, null);

		if (rowId >= 0) {
			EditText uri_dest = (EditText)addView.findViewById(R.id.uri);
			TextView uri_src = (TextView)v.findViewById(R.id.title);
			uri_dest.setText(uri_src.getText().toString());
		}
		
		final DialogWrapper wrapper=new DialogWrapper(addView);
		

		
		new AlertDialog.Builder(this)
		.setTitle(R.string.list_add_name)
		.setView(addView)
		.setPositiveButton(R.string.button_ok,
			new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
				int whichButton) {
					processAdd(wrapper);
				}
		})
		.setNegativeButton(R.string.button_cancel,
			new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
				int whichButton) {
					// ignore, just dismiss
				}
		})
		.show();
	}
	
	private void processAdd(DialogWrapper wrapper) {
		ContentValues values=new ContentValues(2);

		values.put(DatabaseHelper.NAME, wrapper.getName());
		values.put(DatabaseHelper.URI, wrapper.getUri());
		
		db = new DatabaseHelper(this);
		db.getWritableDatabase().insert("bookmarks", DatabaseHelper.NAME, values);
		db.close();

		NodesBookmarks.bookmarksCursor.requery();
	}	
	
	class DialogWrapper {
		EditText nameField=null;
		EditText uriField=null;
		View base=null;

	
		DialogWrapper(View base) {
			this.base=base;
			uriField=(EditText)base.findViewById(R.id.uri);
		}

		String getName() {
			return(getNameField().getText().toString());
		}

		String getUri() {
			return(getUriField().getText().toString());
		}

		private EditText getNameField() {
			if (nameField==null) {
				nameField=(EditText)base.findViewById(R.id.name);
			}

			return(nameField);
		}

		private EditText getUriField() {
			if (uriField==null) {
				uriField=(EditText)base.findViewById(R.id.uri);
			}

			return(uriField);
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
}

