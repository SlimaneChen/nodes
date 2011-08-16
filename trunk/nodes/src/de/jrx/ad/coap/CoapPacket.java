package de.jrx.ad.coap;

public class CoapPacket {
	
	/*
	 * CoAP Message Syntax
	 * http://tools.ietf.org/html/draft-ietf-core-coap-05#page-12
	 */
	
	/*
	 * TODO: Save the request-uri
	 */
	
	Integer version=1;				// Version (Ver)
	Integer type = null;			// Type (T): Confirmable (0), Non-Confirmable (1), Acknowledgement (2) or Reset (3)
	Integer option_count = null;	// Option Count (OC), Number of Options
	Integer code = null;			// Code: Request (1-31), Response (64-191), or is Empty (0)
	
	Integer id=null;				// Message-ID
	byte data[] = null;				// Payload

	public Integer getOptionCount() { return(option_count); }
	public void setOptionCount(Integer option_count) { this.option_count=option_count; }
	
	public Integer getCode() { return(code); }
	public void setCode(Integer code) { this.code=code; }

	public Integer getType() { return(type); }
	public void setType(Integer type) { this.type=type; }
	
	public Integer getID() { return(id); }
	public void setID(Integer id) { this.id=id; }

	public String getData() { return(new String(data)); }
	public void setData(byte data[]) {	this.data=data;	}
	
	
	/* 
	 * Options
	 * http://tools.ietf.org/html/draft-ietf-core-coap-05#section-5.10
	 */
	
	Integer content_type = null;	// 1. | Critical | Content-Type   | uint   | 1-2 B   | 0           |
	Integer max_age = 60;			// 2. | Elective | Max-Age        | uint   | 0-4 B   | 60          |
									// 3. | Critical | Proxy-Uri      | string | 1-270 B | (none)      |
	byte etag[] = null;				// 4. | Elective | ETag           | opaque | 1-8 B   | (none)      |
	String uri_host = "";			// 5. | Critical | Uri-Host       | string | 1-270 B | (see below) |
	String location_path = "";		// 6. | Elective | Location-Path  | string | 1-270 B | (none)      |
									// 7. | Critical | Uri-Port       | uint   | 0-2 B   | (see below) |
									// 8. | Elective | Location-Query | string | 1-270 B | (none)      |
	String uri_path = "";			// 9. | Critical | Uri-Path       | string | 1-270 B | (none)      |
	byte token[] = null;			// 11.| Critical | Token          | opaque | 1-8 B   | (empty)     |
	String uri_query = "";			// 15.| Critical | Uri-Query      | string | 1-270 B | (none)   
	
	
	public Integer getOptContentType() { return(content_type); }
	public void setOptContentType(Integer content_type) { this.content_type=content_type; }
	
	public Integer getOptMaxAge() { return(max_age); }
	public void setOptMaxAge(Integer max_age) { this.max_age=max_age; }

	public String getOptLocationPath() { return(location_path); }
	public void setOptLocationPath(String location_path) { this.location_path=location_path; }
}