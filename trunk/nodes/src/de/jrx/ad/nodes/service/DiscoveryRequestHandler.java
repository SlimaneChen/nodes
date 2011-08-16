package de.jrx.ad.nodes.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import de.jrx.ad.coap.CoapNodes;
import de.jrx.ad.coap.CoapPacket;
import de.jrx.ad.nodes.Nodes;

public class DiscoveryRequestHandler implements HttpRequestHandler {
	
	public DiscoveryRequestHandler(){
		
	}	
	
	
	private final Object lock = new Object();
	
	AtomicBoolean isRunning=new AtomicBoolean(false);
	AtomicBoolean receivedError=new AtomicBoolean(false);
	AtomicBoolean receivedData=new AtomicBoolean(false);
	int receivedId = 0;
	
	HttpRequest request; 
	HttpResponse response;
	HttpContext httpContext;	
	
	CoapPacket packet = new CoapPacket();
	private List<CoapNodes> nodes=new ArrayList<CoapNodes>();
	
	public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
				
		this.request = request;
		this.response = response;
		this.httpContext = httpContext;
		
		isRunning.set(false);
		receivedError.set(false);
		receivedData.set(false);
		receivedId = 0;

		Log.i("server", "new connection");
		
		/* Handle request payload */ 
        byte payload[] = null;	
		
        /* Prepare arguments for libcoap */
        
        
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(Nodes.app);
        final String req_uri = "coap://[" + prefs.getString("coapmulticast", "ff02::1") +"%" + prefs.getString("coapinterface", "eth0") + "]/";
        
        final String req_method = "GET";
        getData(req_method, req_uri, payload);
        
        synchronized (lock) {
            try {
				lock.wait(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
          }

		String contentType = "text/html";
        
    	/* Error Handling */
    	if (receivedError.get()) {
    		
    		isRunning.set(false);
        	response.setStatusCode(502);
 
    	} else if (receivedId > 0 && receivedData.get()) {

    		isRunning.set(false);
        	Log.i("»»ad-rest-service««", "receivedData");

    	}
    	StringBuilder buf = new StringBuilder("");;
    	for (CoapNodes node : nodes) {			
				
			buf.append("<coap://[" + node.getIPv6() + "]"+ prefs.getString("coapcorelink", "/.well-known/core") +">;ct=40,");
			
			//	buf.append("<strong>[" + node.getIPv6() + "]:"+ node.getPort() + "</strong>");
			buf.append("\n");

		}
        final String foutput = buf.toString();

        nodes.clear();
        
        /* Build reponse content */
		HttpEntity entity = new EntityTemplate(new ContentProducer() {
    		public void writeTo(final OutputStream outstream) throws IOException {
    			OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
            
        		writer.write(foutput);
        		
    			writer.flush();
    			Log.i("»»ad-rest-service««", "Respond.");
    	
    			
    		}
    	});
		
		((EntityTemplate)entity).setContentType(contentType);
		
		response.setEntity(entity);	
        
	}
	
	/** Send response to client **/
	private void generateResponse() {
	
        
        /*
        ** Wait until a CoAP-Message is received
        ** TODO: Implement Timeout 
        ** TODO: req_id == packet.id ? --> 502
        ** TODO:Connection still alive?
        */ 
    	
    	/* Error Handling */
    	if (receivedError.get()) {
    		
    		isRunning.set(false);

    	} else if (receivedId > 0 && receivedData.get()) {

    		isRunning.set(false);

    	}        
    	
        /* Check if necessary data is received */
    	if (!isRunning.get()) {
    		
    	       synchronized (lock) {
    	            lock.notify();
    	          }
    	       
	
    	}
	}
	
	/** Send arguments to libcoap **/
	
	private int getData(String method, String strURL, final byte payload[]) {
		
		String opt_method= " -m " + method;		// -m [GET,PUT,POST,DELETE]
		String opt_file= "";					// -f -
		
        
        // Handle Payload
        if (payload != null && !(new String(payload).equals(""))) {
        	opt_file =" -f -";
        }
        
        
       	final String argv = "./coap-client"+ opt_method + opt_file + " " + strURL ;        
    	final int argc = argv.replaceAll("[^" + " " + "]","").length() + 1; //Leerzeichen zählen

        
    	Log.i("--ad-coap-client--", "uri: " + argv + " argc:" + argc);

		if (!strURL.equals("")) {
			Log.w(getClass().getName(), "jni start!");
			
		   	Thread background=new Thread(new Runnable() {
	    		public void run() {
	    			if(isRunning.get()) {
	    				
		    	    	Looper.prepare();
		    	    	
		    	    	Message returnMsg = new Message();
	    				returnMsg.arg1 = getNeighbors(argc, argv, payload);

	    				Log.i("--ad-coap-client--", "Java - Return of JNI:" + returnMsg);
	    				
		    	    	handler.sendMessage(returnMsg);
		    	    	
		    	    	
	    			}
	    		}
	    	});
		  
			isRunning.set(true);
		   	background.start();
		}
		
		return 0;
	}
	
	
	/** Receive id from libcoap **/
	Handler handler=new Handler() {
		@Override
			public void handleMessage(Message msg) {
			int id = msg.arg1;
			
			if (id < 0) {
			
	        	receivedError.set(true);

			} else {
				receivedId = id;
			}
			
			generateResponse();
			
			}
		};
		
	/** Receive data from libcoap **/
	
	public void JNIgetPacket(int id, int code, int type, int optcnt,int optcontenttype, int optmaxage, byte optlocationpath[], byte data[]) {

		/* Header */
		packet.setID(id);
		packet.setCode(code);
		packet.setType(type);
		packet.setOptionCount(optcnt);
		
		/* Options */
		packet.setOptContentType(optcontenttype);
		packet.setOptMaxAge(optmaxage);
		if (optlocationpath != null) {	packet.setOptLocationPath(new String(optlocationpath)); }
		
		/* Payload */
		packet.setData(data);
		
		Log.i("getPacket", "" + id);	
		receivedData.set(true);
		generateResponse();
		
	}
	
	/** Receive neighbors from libcoap **/
	
	public void JNIgetNeighbor(String addr, int port) {
		
		CoapNodes node=new CoapNodes();
		
		nodes.add(node);
			
		/* Node */
		node.setIPv6(addr);
		node.setPort(port);

		receivedData.set(true);
			
	}
	
	/** Called from JNI to check if the request is still active **/
	public int JNIisRunning() {
		if (isRunning.get()) {
			return 1;
		} else {
			return 0;
		}
	}	
	
	
	/** JNI **/

	public native int getNeighbors(int argc, String argv, byte payload[]);
    
    static {
        System.loadLibrary("coap-client");
    }
       
}

