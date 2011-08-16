package de.jrx.ad.coap;

public class CoreLink {
	
	String uri="/";
	String name=""; 		// 'n'
	Integer content_type=255;	// 'ct' any media type

	public String getURI() { return(uri); }
	public void setURI(String uri) { this.uri=uri; }
	
	public String getName() { return(name); }
	public void setName(String name) { this.name=name; }

	public Integer getContent_Type() { return(content_type); }
	public void setContent_Type(Integer content_type) { this.content_type=content_type; }
	
}