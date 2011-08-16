package de.jrx.ad.nodes.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import android.util.Log;

public class FallbackRequestHandler implements HttpRequestHandler {
	
	public FallbackRequestHandler(){
		
	}	
	
	public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
		String contentType = "text/html";
		
        Log.i("Request:", request.getRequestLine().toString());
        
        for (Header header : request.getAllHeaders()) {
	        Log.i("Request-Header:", header.getName() + " " + header.getValue());	 
        }
        
		HttpEntity entity = new EntityTemplate(new ContentProducer() {
    		public void writeTo(final OutputStream outstream) throws IOException {
    			OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
            
        		writer.write("Nodes HTTP-CoAP Mapper"); 
        		
    			writer.flush();
    			Log.i("»»ad-rest-service««", "Respond.");
    	
    			
    		}
    	});
		
		((EntityTemplate)entity).setContentType(contentType);
		
				
		response.setStatusCode(501);
		response.setEntity(entity);
	}

}