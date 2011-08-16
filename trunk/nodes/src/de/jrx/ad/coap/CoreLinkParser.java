package de.jrx.ad.coap;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.util.Log;

public class CoreLinkParser{
    
    public static List<CoreLink> parse(String data) {
    	List<CoreLink> links=new ArrayList<CoreLink>();
    	/* 
    	** CoRE Link Format Parser
    	** http://tools.ietf.org/html/draft-ietf-core-link-format-02
    	** RegEX by Matthias Kovatsch <kovatsch@inf.ethz.ch>
    	*/

    	Log.i("ad-coap-links", "parsing links..");
        	
	    	String	pLinks = "(<[^>]+>\\s*(;\\s*[^<\"\\s;,]+\\s*=\\s*([^<\"\\s;,]+|\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\")\\s*)*)";
	    	String	pElements = "^<([^>]+)>\\s*(;.+)\\s*$";
	    	String 	pKeys = ";\\s*([^<\"\\s;,]+)\\s*=\\s*(([^<\"\\s;,]+)|\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\")";
	    	
	    	Matcher matLinks = Pattern.compile(pLinks).matcher(data);
	    	Matcher matElements;
	    	Matcher matKeys;
	    	
	    	while (matLinks.find()) {
	    			    		
	    		Log.i("ad-coap-links", "found Links: \""+ matLinks.group() +"\" start: "+matLinks.start()+" end "+matLinks.end());
	    		matElements = Pattern.compile(pElements).matcher(matLinks.group());
	    		
	    		while (matElements.find()) {


	    			CoreLink link=new CoreLink();
	    			links.add(link);
	    			
	    			Log.i("ad-coap-links", "found Elements: \""+ matElements.group(1));	    
	    			link.setURI(matElements.group(1));
	    			
	    			Log.i("ad-coap-links", "found Elements: \""+ matElements.group(2));	 
	    			
	    			
		    		matKeys = Pattern.compile(pKeys).matcher(matElements.group(2));

		    		while (matKeys.find()) {
		    			
		    			Log.i("ad-coap-links", "found Key1: "+ matKeys.group(1));
		    			Log.i("ad-coap-links", "found Key2: "+ matKeys.group(2));	
		    			
		    			// Name
		    			if (matKeys.group(1).equalsIgnoreCase("n")) {
		    				link.setName(matKeys.group(2));
		    			}

		    			// Content-Type
		    			if (matKeys.group(1).equalsIgnoreCase("ct")) {
		    				link.setContent_Type(Integer.parseInt(matKeys.group(2)));
		    			}
		    			
		    		}
	    			
	    		}
	    		
	    	}
    	
    	
    	/* ** */
	    	return links;    	
    }
    
}