package de.jrx.ad.nodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Tools {

	public static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         * http://senior.ceng.metu.edu.tr/2009/praeda/2009/01/11/a-simple-restful-client-at-android/
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
	
    public static byte[] convertStreamToBytes(InputStream stream, int aMaxBytes) throws IOException {
    	/* 
    	 * http://lists.apple.com/archives/Java-dev/2006/Jan/msg00391.html
    	 */
    	int length = 0;
    	byte[] bytes = new byte[aMaxBytes];

    			while (length < aMaxBytes) {
    				int count = stream.read(bytes, length, aMaxBytes - length);
    				
    				if (count < 0) {
    					byte[] buffer = new byte[length];
    					
    					System.arraycopy(bytes, 0, buffer, 0, length);
    					
    					return buffer;
    				}
    				
    				length += count;
    			}
    			

    			return bytes;
    		}
    
    public static String stringToHTMLString(String string) {
	    StringBuffer sb = new StringBuffer(string.length());
	    // true if last char was blank
	    boolean lastWasBlankChar = false;
	    int len = string.length();
	    char c;

	    for (int i = 0; i < len; i++)
	        {
	        c = string.charAt(i);
	        if (c == ' ') {
	            // blank gets extra work,
	            // this solves the problem you get if you replace all
	            // blanks with &nbsp;, if you do that you loss 
	            // word breaking
	            if (lastWasBlankChar) {
	                lastWasBlankChar = false;
	                sb.append("&nbsp;");
	                }
	            else {
	                lastWasBlankChar = true;
	                sb.append(' ');
	                }
	            }
	        else {
	            lastWasBlankChar = false;
	            //
	            // HTML Special Chars
	            if (c == '"')
	                sb.append("&quot;");
	            else if (c == '&')
	                sb.append("&amp;");
	            else if (c == '<')
	                sb.append("&lt;");
	            else if (c == '>')
	                sb.append("&gt;");
	            //else if (c == '\n')
	                // Handle Newline
	                //sb.append("&lt;br/&gt;");
	            else {
	                int ci = 0xffff & c;
	                if (ci < 160 )
	                    // nothing special only 7 Bit
	                    sb.append(c);
	                else {
	                    // Not 7 Bit use the unicode system
	                    sb.append("&#");
	                    sb.append(new Integer(ci).toString());
	                    sb.append(';');
	                    }
	                }
	            }
	        }
	    return sb.toString();
	}

    public static String decodeURL(String uri) {
    	
    	// Find the scope-id at the ipv6 local address
        String re1="(?<=\\%5Bfe80)(.*)(\\%\\w*)(?=\\%5D)";

        
        Pattern p = Pattern.compile(re1,Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(uri);
        if (m.find())
        {
            String var1=m.group(2);
            uri = uri.replace(var1, "");
            
            try {
        		uri = URLDecoder.decode(uri, "UTF-8");
        	} catch (UnsupportedEncodingException e) {
        		e.printStackTrace();
        	}

        	uri = uri.replaceFirst("]", var1 + "]");
        	
        } else {

        	try {
        		uri = URLDecoder.decode(uri, "UTF-8");
        	} catch (UnsupportedEncodingException e) {
        		e.printStackTrace();
        	}
        	
        }
    	
    	// Delete leading slash
        if (uri.charAt(0) == '/' ) {
    		Log.i("uri", "/");
    		uri = uri.substring(1);
    	}
        
        // Check if the link-locale ipv6 address has a scope is, if not add it
        
        re1="(?<=\\[fe80)([a-f0-9:]*)(?=\\])";

        p = Pattern.compile(re1,Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        m = p.matcher(uri);
        if (m.find())
        {
            SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(Nodes.app);

        	uri = uri.replaceFirst("]", "%" + prefs.getString("coapinterface", "eth0") + "]");
        }

        
	    return uri;
	}

}
