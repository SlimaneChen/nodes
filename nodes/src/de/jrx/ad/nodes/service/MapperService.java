package de.jrx.ad.nodes.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import de.jrx.ad.nodes.Nodes;
import de.jrx.ad.nodes.R;


public class MapperService extends Service {
	public static final String EXTRA_PORT="EXTRA_PORT";
	
	private int port = 0;
	private BasicHttpProcessor httpproc = null;
	private BasicHttpContext httpContext = null;
	private HttpService httpService = null;
	private HttpRequestHandlerRegistry registry = null;

	AtomicBoolean isRunning=new AtomicBoolean(false);

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		port=intent.getIntExtra(EXTRA_PORT, 8080);
		httpproc = new BasicHttpProcessor();
		httpContext = new BasicHttpContext();
		
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        httpService = new HttpService(httpproc, 
        		new DefaultConnectionReuseStrategy(),
        		new DefaultHttpResponseFactory());
        
        registry = new HttpRequestHandlerRegistry();
        registry.register("*", new FallbackRequestHandler());

        registry.register("coap://*", new CoapRequestHandler());
        registry.register("/coap://*", new CoapRequestHandler());

        
        registry.register("/.well-known/core*", new DiscoveryRequestHandler());

        httpService.setHandlerResolver(registry);  
        
		start(port);			
		return(START_NOT_STICKY);
	}

	
	@Override
	public void onDestroy() {
		stop();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return(null);
	}

	private void start(final int port) {
				
		// HTTP Client, workaround to  flush the HTTP Server
		HttpClient client = new DefaultHttpClient();
	    HttpGet request = new HttpGet("http://127.0.0.1:" + port);
	        try{
	            client.execute(request);
	        }catch(Exception ex){
	        	Log.i(getClass().getName(),"HTTP Client failed");
	        }
	        
		//stopService(new Intent(this, MapperService.class));
		Log.w(getClass().getName(), "FORCE STOP!");

		
		if (!isRunning.get()) {
			Log.w(getClass().getName(), "Got to start()!");

		   	Thread background=new Thread(new Runnable() {

				public void run() {
	    			    			
					try {
						ServerSocket serverSocket = new ServerSocket(port);
		    			serverSocket.setReuseAddress(true);
		    			

		    			while (isRunning.get()) {
		    			
		    				
		    				try {
		    					
		    					
		    					final Socket socket = serverSocket.accept();
		    					DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();
		    					serverConnection.bind(socket, new BasicHttpParams());
		    					
		    					httpService.handleRequest(serverConnection, httpContext);

		    					serverConnection.shutdown();
		    					
		    				} catch (IOException e) {
		    					e.printStackTrace();
		    					//stopService(new Intent(this, MapperService.class));
		    				} catch (HttpException e) {
		    					e.printStackTrace();
		    				}
		    				
		    				
		    			}
		    			
		    			serverSocket.close();
		    			stopForeground(true);
		    			sendBroadcast(new Intent("de.jrx.ad.nodes.service.STOP"));

		    							
					} catch (IOException e) {
						e.printStackTrace();
						isRunning.set(false);
						stopForeground(true);
						sendBroadcast(new Intent("de.jrx.ad.nodes.service.SOCKET_FAILED"));
					}

	    			
	    		}
	    	});
			
						
			/* Notify */
			
			Notification note=new Notification(R.drawable.stat_notify_server,
				"HTTP-Server started.",
				System.currentTimeMillis());
			
			Intent i=new Intent(this, Nodes.class);
			
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
				Intent.FLAG_ACTIVITY_SINGLE_TOP);
			
			PendingIntent pi=PendingIntent.getActivity(this, 0,
				i, 0);
			
			note.setLatestEventInfo(this, "HTTP-CoAP Mapper",
				"Running.",
				pi);
			
			note.flags|=Notification.FLAG_NO_CLEAR;
		
			startForeground(1337, note);

			sendBroadcast(new Intent("de.jrx.ad.nodes.service.START"));
			isRunning.set(true);
	    	background.start();
	    	
		}
		
	}

	private void stop() {
		if (isRunning.get()) {
			
			Log.w(getClass().getName(), "Got to stop()!");
			isRunning.set(false);

			// HTTP Client, workaround to  flush the HTTP Server
			HttpClient client = new DefaultHttpClient();
		    HttpGet request = new HttpGet("http://127.0.0.1:" + port);
		        try{
		            client.execute(request);
		        }catch(Exception ex){
		        	Log.i(getClass().getName(),"HTTP Client failed");
		        }
		        
			//stopService(new Intent(this, MapperService.class));
			Log.w(getClass().getName(), "FORCE STOP!");
			
		}

	}
}
