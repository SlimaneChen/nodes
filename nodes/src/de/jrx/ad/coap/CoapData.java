package de.jrx.ad.coap;

import java.util.ArrayList;
import java.util.List;

public class CoapData {
	
	public static String getContentTypeString(Integer code) {
		
		/*
		**  Get Content-Type for HTTP based on libcoap
		*/
		
		String type = "";
		
		switch (code) {
			case  0:  type = "text/plain (UTF-8)";				break;
			case  1:  type = "text/xml (UTF-8)";				break;
			case  2:  type = "text/csv (UTF-8)";				break;
			case  3:  type = "text/html (UTF-8)";				break;
			case 21:  type = "image/gif";						break;
			case 22:  type = "image/jpeg";						break;
			case 23:  type = "image/png";						break;
			case 24:  type = "image/tiff";						break;
			case 25:  type = "audio/raw";						break;
			case 26:  type = "video/raw";						break;
			case 40:  type = "application/link-format";			break;
			case 41:  type = "application/xml";					break;
			case 42:  type = "application/octet-stream";		break;
			case 43:  type = "application/rdf+xml";				break;
			case 44:  type = "application/soap+xml";			break;
			case 45:  type = "application/atom+xml";			break;
			case 46:  type = "application/xmpp+xml";			break;
			case 47:  type = "application/exi";					break;
			case 48:  type = "application/x-bxml";				break;
			case 49:  type = "application/fastinfoset";			break;
			case 50:  type = "application/soap+fastinfoset";	break;
			case 51:  type = "application/json";				break;

			// 0xff /* any media type */		
			default: type = "*/*"; break;
		}
		
		return type;
	}

	public static Integer getStatusInteger(Integer code) {
		
		/*
		**  Get Status-Code as String for HTTP based on libcoap
		*/
		
		Integer status = 0;
		
		switch (code) {
			case  40:	status = 100; break; /* Continue */
			case  80:	status = 200; break; /* OK*/
			case  81:	status = 201; break; /* Created*/
			case 124:	status = 304; break; /* Not Modified*/
			case 160:	status = 400; break; /* Bad Request */
			case 164:	status = 404; break; /* Not Found */
			case 165:	status = 405; break; /* Method Not Allowed */
			case 175:	status = 415; break; /* Unsupported Media Type */
			case 200:	status = 500; break; /* Internal Server Error */
			case 203:	status = 503; break; /* Service Unavailable */
			case 204:	status = 504; break; /* Gateway Timeout */
			case 240:	status = 501; break; /* "Token Option required by server"; */
			case 241:	status = 501; break; /* "Uri-Authority Option required by server" */
			case 242:	status = 501; break; /* "Critical Option not supported" */
			default: status = 501; break;
		}
		return status;
		
	}

	public static String getStatusString(Integer code) {
		
		/*
		**  Get Status-Code as String for HTTP based on libcoap
		*/
		
		String status = "";
		
		switch (code) {
			case  40:	status = "100 Continue";							break;
			case  80:	status = "200 OK";									break;
			case  81:	status = "201 Created";								break;
			case 124:	status = "304 Not Modified";						break;
			case 160:	status = "400 Bad Request";							break;
			case 164:	status = "404 Not Found";							break;
			case 165:	status = "405 Method Not Allowed";					break;
			case 175:	status = "415 Unsupported Media Type";				break;
			case 200:	status = "500 Internal Server Error";				break;
			case 203:	status = "503 Service Unavailable";					break;
			case 204:	status = "504 Gateway Timeout";						break;
			case 240:	status = "Token Option required by server";			break;
			case 241:	status = "Uri-Authority Option required by server";	break;
			case 242:	status = "Critical Option not supported";			break;
			default: status = "Not Implemented"; break;
		}
		return status;
		
	}
	
	public static String CoreLinkToHtml(String data) {
		
		/*
		**  Convert the CoRE Link Format to HTML for the CoAP-Browser
		*/
		
			StringBuilder buf=new StringBuilder("");
			
			List<CoreLink> links=new ArrayList<CoreLink>();
	    	links = CoreLinkParser.parse(data);
	    	
	    	for (CoreLink link : links) {
	    		
	    		buf.append("<p>");
	    		
	    		if (!link.getName().equals("")) {
	    			buf.append(link.getName());
	    			buf.append("<br />");
	    		}
	    		if (link.getContent_Type()!=255) {
	    			buf.append(getContentTypeString(link.getContent_Type()));
	    			buf.append("<br />");
	    		}
	    		
	    		buf.append("<a href=\""+ link.getURI() +"\">" + link.getURI() + "</a>");
	    		buf.append("</p>");
	    		
	    	}

		
		return buf.toString();
		
	}
	
}

