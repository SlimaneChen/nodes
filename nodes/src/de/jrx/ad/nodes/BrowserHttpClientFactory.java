package de.jrx.ad.nodes;

import org.apache.http.HttpHost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BrowserHttpClientFactory {

    private static DefaultHttpClient httpclient;

    public synchronized static DefaultHttpClient getThreadSafeClient() {
  
        //if (httpclient != null)
        //    return httpclient;
        
        httpclient = new DefaultHttpClient();
        
        ClientConnectionManager mgr = httpclient.getConnectionManager();
                
 	    /* Register "coap://"-UriScheme */
        SchemeRegistry schemeRegistry = mgr.getSchemeRegistry();
        schemeRegistry.register(new Scheme("coap", PlainSocketFactory.getSocketFactory(), 61616));
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        HttpParams params = httpclient.getParams();      

		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager (params , schemeRegistry);
        
		/* Initialize the HttpClient */
        httpclient = new DefaultHttpClient(cm, params);
        
        /* Set the Proxy */
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(Nodes.app);
        HttpHost proxy = new HttpHost("localhost", new Integer(prefs.getString("port", "8080")).intValue());
        httpclient.getParams().setParameter (ConnRoutePNames.DEFAULT_PROXY, proxy);
        
        /* Optional auth data for the proxy*/
//		httpclient.getCredentialsProvider().setCredentials(
//		new AuthScope("localhost", 8081),
//		new UsernamePasswordCredentials("username", "password"));       

       
  
        return httpclient;
    } 
}