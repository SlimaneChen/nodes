package de.jrx.ad.nodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;

import de.jrx.ad.nodes.service.MapperService;

public class Browser extends Activity {
	
	public static final int MENU_CLOSE = Menu.FIRST+1;
	private static final String[] methods={"GET", "PUT", "POST", "DELETE"};

	private MyReceiver receiver;

	private WebView browser;
	private EditText browserbar;
	private ImageView imgServer;

	AtomicBoolean isRunning=new AtomicBoolean(false);
	
	/** Initialize all Instances **/
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.browser);
        
	    browser=(WebView)findViewById(R.id.webkit);
		browserbar=(EditText)findViewById(R.id.browserbar);
		browserbar.setOnEditorActionListener(processMethod);

		/* Handle Intents */
		if (getIntent().getAction() != null) {
			browserbar.setText(getIntent().getData().toString());
		}
		
		imgServer = (ImageView)findViewById(R.id.server); 
		
		if (isMapperServiceRunning() == true) {
			imgServer.setImageResource(R.drawable.title_server_on);
		} else {
			imgServer.setImageResource(R.drawable.title_server_off);
		}
		
	    generatePage("..");
	}
	
	@Override
	public void onResume() {
		super.onResume();

		if (isMapperServiceRunning() == true) {
			imgServer.setImageResource(R.drawable.title_server_on);
		} else {
			imgServer.setImageResource(R.drawable.title_server_off);
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_CLOSE, Menu.NONE, "Close");

		return(super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_CLOSE:
				finish();
				return(true);
		}

		return(super.onOptionsItemSelected(item));
	}
	
	/** Accept the Enter/Go-Button from the keyboard **/
	private TextView.OnEditorActionListener processMethod=
		new TextView.OnEditorActionListener()
	{
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

	    	connect(browserbar.getText().toString(), "GET", "".getBytes());	    	
			return false;
		}
			
	};
	
	 public void connect(final String url, final String method, final byte[] payload)
	    {	 
		 	// Hide the keyboard
	    	InputMethodManager mgr=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
	    	mgr.hideSoftInputFromWindow(browserbar.getWindowToken(), 0);

		 	HttpClient httpclient = BrowserHttpClientFactory.getThreadSafeClient();

		 	
	    	if (!isRunning.get()) {
	    		
	    		// Try the Connection
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

			    		        /* Send Request */
			    	        	
			    	        	HttpUriRequest request = new HttpGet(url);
			    	        	
		    	        		StringEntity se = new StringEntity(new String(payload),HTTP.UTF_8);
			    	        	se.setContentType("text/plain"); 

			    	        	if (method.equalsIgnoreCase("POST")) {
			    	        		request = new HttpPost(url);			   
				    	        	((HttpPost)request).setEntity(se);
			    	        	} else if (method.equalsIgnoreCase("PUT")) {
			    	        		request = new HttpPut(url);
			    	        		((HttpPut)request).setEntity(se);

			    	        	} else if (method.equalsIgnoreCase("DELETE")) {
			    	        		request = new HttpDelete(url);
			    	        	}
			    	        	
		    	        		request.addHeader("Nodes-Flags", "debug");


			    		        /* Receive Response */
			    			 	
			    			 	HttpResponse response;
			    				StringBuilder buf=new StringBuilder("");

			    				Log.i("nodes", "connections opened");
			    	        	response = client.execute(request);
			    	        	
			    	            HttpEntity entity = response.getEntity();
			    	 
			    	            if (entity != null) {
			    	 
			    	                InputStream instream = entity.getContent();
			    	                String 	result = Tools.convertStreamToString(instream);
			    	                //		result = Tools.stringToHTMLString(result);
			    	                buf.append(result);	                
			    	                instream.close();
			    	            }
			    	            
			                    generatePage(buf.toString());
			    	 
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
	    		httpclient = BrowserHttpClientFactory.getThreadSafeClient();
	    		Log.i("nodes", "connections closed");
	    	}

	        
	    }
	 
	
	public void generatePage(String data) {
		isRunning.set(false);
		StringBuilder buf=new StringBuilder(data);
		browser.loadDataWithBaseURL(null, buf.toString(), "text/html", "UTF-8", null);
	}
	
	/** Dialog **/
	public void showMethodDialog(View v) {
		LayoutInflater inflater=LayoutInflater.from(this);
		View addView=inflater.inflate(R.layout.browser_methods, null);
		final DialogMethodWrapper wrapper=new DialogMethodWrapper(addView, this);
		
		new AlertDialog.Builder(this)
		.setTitle(R.string.title_dialog_method)
		.setView(addView)
		.setPositiveButton(R.string.button_ok,
			new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
				int whichButton) {
				 	// Hide the keyboard
					InputMethodManager mgr=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
			    	mgr.hideSoftInputFromWindow(wrapper.getPayloadField().getWindowToken(), 0);
					processMethodDialog(wrapper);
				}
		})
		.setNegativeButton(R.string.button_cancel,
			new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
				int whichButton) {
					InputMethodManager mgr=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
					mgr.hideSoftInputFromWindow(wrapper.getPayloadField().getWindowToken(), 0);
					// ignore, just dismiss
				}
		})
		.show();
	}
	
	private void processMethodDialog(DialogMethodWrapper wrapper) {

		connect(browserbar.getText().toString(),wrapper.getMethod(), wrapper.getBytePayload());

	}
	
	
	class DialogMethodWrapper {
		Spinner methodSpinner=null;
		EditText payloadField=null;
		View base=null;
		byte data[]=null;
		
		DialogMethodWrapper(View base, Context ctx) {

			this.base=base;

			methodSpinner=(Spinner)base.findViewById(R.id.method);

			ArrayAdapter<String> aa=new ArrayAdapter<String>(
					ctx,
					android.R.layout.simple_spinner_item,
					methods
					);
			
			aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

			methodSpinner.setAdapter(aa);
			
			payloadField=(EditText)base.findViewById(R.id.payload);
		}
	
		String getMethod() {
			
			return methods[getMethodSpinner().getSelectedItemPosition()];
		}

		String getPayload() {
			return(getPayloadField().getText().toString());
		}
		
		byte[] getBytePayload() {
			return (getPayloadField().getText().toString().getBytes());			
		}
		
		private Spinner getMethodSpinner() {
			if (methodSpinner==null) {
				methodSpinner=(Spinner)base.findViewById(R.id.method);
			}

			return(methodSpinner);
		}

		private EditText getPayloadField() {
			if (payloadField==null) {
				payloadField=(EditText)base.findViewById(R.id.payload);
			}

			return(payloadField);
		}
	}
	
	/** Buttons **/
	public void retransmitData(View v) {
		connect(browserbar.getText().toString(), "GET", "".getBytes());
	}
	
	public void visitNodes(View v) {
		startActivity(new Intent(this, Nodes.class));
	}
	
	/** Broadcast Receiver **/
	public void startMapperService(View v) {
		Intent i=new Intent(this, MapperService.class);
		if (isMapperServiceRunning() == false) {
			
			SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
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
