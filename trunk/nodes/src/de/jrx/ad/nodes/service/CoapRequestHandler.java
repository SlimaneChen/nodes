package de.jrx.ad.nodes.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import de.jrx.ad.coap.CoapData;
import de.jrx.ad.coap.CoapPacket;
import de.jrx.ad.nodes.Tools;

public class CoapRequestHandler implements HttpRequestHandler {
	
	public CoapRequestHandler(){
		
	}	
	
	private final Object lock = new Object();
	
	AtomicBoolean isRunning=new AtomicBoolean(false);
	AtomicBoolean notImplemented=new AtomicBoolean(false);
	AtomicBoolean receivedError=new AtomicBoolean(false);
	AtomicBoolean receivedData=new AtomicBoolean(false);
	int receivedId = 0;
	
	HttpRequest request; 
	HttpResponse response;
	HttpContext httpContext;	
	
	CoapPacket packet = new CoapPacket();

	
	public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
				
		this.request = request;
		this.response = response;
		this.httpContext = httpContext;
		
		isRunning.set(false);
		notImplemented.set(false);
		receivedError.set(false);
		receivedData.set(false);
		receivedId = 0;

		Log.i("server", "new connection");
		
		/* Switch between Standard Proxy and Nodes Browser View */
		AtomicBoolean isDebugging=new AtomicBoolean(false);
        if (request.containsHeader("Nodes-Flags")) {
	        String nodesflags  = request.getFirstHeader("Nodes-Flags").getValue().toString(); 
	        
	        if (nodesflags.contains("debug")) {
	        	isDebugging.set(true);
	        	Log.i ("Request-Nodes-Flags:", new String(nodesflags));
	        }
        }
        
		/* Handle request payload */ 
        byte payload[] = null;
        if (request.containsHeader("Content-Length")) {
	        int length = new Integer(request.getFirstHeader("Content-Length").getValue()).intValue(); 

			InputStream input = ((HttpEntityEnclosingRequest) request).getEntity().getContent();
		    
			payload = Tools.convertStreamToBytes(input, length);
			Log.i ("Request-Payload:", new String(payload));
        }		
		
        /* Prepare arguments for libcoap */
        
        final String req_uri = Tools.decodeURL(request.getRequestLine().getUri());
        final String req_method = request.getRequestLine().getMethod();
        
        /* Check if Request-Method is supported by CoAP */
        if (		req_method.equalsIgnoreCase("GET")
        		||	req_method.equalsIgnoreCase("PUT")
        		||	req_method.equalsIgnoreCase("POST")
        		||	req_method.equalsIgnoreCase("DELETE")	) {
        	
	        getData(req_method, req_uri, payload);
	        
	        synchronized (lock) {
	            try {
					lock.wait(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	          }
        
        } else {
    		notImplemented.set(true);
        }
        
		String contentType = "text/plain";
        String output = "";
                        
        /* Build reponse content */

        isRunning.set(false);
        
        /* Standard Proxy */
        if (!isDebugging.get()) {

    		/* Error Received: 502 (Bad Gateway) */
        	if (receivedError.get()) {
        		response.setStatusCode(502);
        		output = "Bad Gateway";
        		
    		/* Error Request Method not supported: 501 (Not Implemented) */
        	} else if (notImplemented.get()) {
        		response.setStatusCode(501);
            	output = "Not Implemented";
            	
            /* Received Data Handling */ 	
        	} else if (receivedId > 0 && receivedData.get()) {
        		
        		response.setStatusCode(CoapData.getStatusInteger(packet.getCode()));
        		contentType = CoapData.getContentTypeString(packet.getOptContentType());
     		
        		
            	output = packet.getData();
            
        	/* Error Time Out: 504 (Gateway Timeout) */	
        	} else {
        		response.setStatusCode(504);
            	output = "Gateway Timeout";        		
        	}
        	
        /* Nodes Browser View */
        } else {

    		/* Error Received: 502 (Bad Gateway) */
        	if (receivedError.get()) {
        		output = "Bad Gateway";
        		
    		/* Error Request Method not supported: 501 (Not Implemented) */
        	} else if (notImplemented.get()) {
            	output = "Not Implemented";
            	
            /* Received Data Handling */ 	
        	} else if (receivedId > 0 && receivedData.get()) {
        		
            	StringBuilder buf=new StringBuilder("");
				
            	/* Check for Content-Type application/link-format (40) */
            	if (CoapData.getContentTypeString(packet.getOptContentType()).equalsIgnoreCase("application/link-format")) {
            		buf.append(CoapData.CoreLinkToHtml(packet.getData()).toString());
            	}
            	
    			buf.append("<p>");	
    			buf.append("<strong>id:" + packet.getID() + "</strong>");
    			buf.append("<strong>code:" + CoapData.getStatusString(packet.getCode()) + ", type:" + packet.getType() + ", option_count:" + packet.getOptionCount() + "</strong>");
    			buf.append("<strong>content_type:" + CoapData.getContentTypeString(packet.getOptContentType()) + ", max_age:" + packet.getOptMaxAge() + ", location_path:" + packet.getOptLocationPath() + "</strong>");

    			buf.append(Tools.stringToHTMLString(packet.getData()));
    			buf.append("</p>\n");	
    			
    			buf.insert(0, "<style TYPE=\"text/css\"> p { background-color: #E6E6E6; padding-top: 2px;} strong { background: white; display: block; margin: 2px;}</style>");
    			buf.insert(0, "<html><head><base href=\""+ request.getRequestLine().getUri().toString() +"\" /></head><body>");		
    			buf.append("</html></body>");
            	
            	output = buf.toString();
           
        	/* Error Time Out: 504 (Gateway Timeout) */	
        	} else {
            	output = "Gateway Timeout";        		
        	}

        	contentType = "text/html";
        	response.setStatusCode(200);
        	        	
        }
        
        final String foutput = output;
        
        /* Write reponse content */
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
	    				returnMsg.arg1 = controlClient(argc, argv, payload);

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
    	receivedError.set(true);
    	generateResponse();
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

	public native int controlClient(int argc, String argv, byte payload[]);
    
    static {
        System.loadLibrary("coap-client");
    }
    
}