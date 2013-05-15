package com.tumblr.jumblr.request;

import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import com.sun.net.httpserver.*;
import com.tumblr.jumblr.JumblrClient;
//import com.google.common.collect.MultiMap;

/**
 * An HTTP server.
 * 
 * NOTE: I modified this from http://stackoverflow.com/a/3732328/1772907
 * 
 * @author Jackson
 */
@SuppressWarnings("restriction")
public class CallbackServer {

    private URI request;
    private HttpServer server;
    private static final int PORT = 8004; // This port is set in the app settings in Tumblr and can't easily be changed

    public static Token tumblrAuthenticate(JumblrClient client, OAuthService service) throws IOException {
        Token request = service.getRequestToken();
        openBrowser(service.getAuthorizationUrl(request));

        CallbackServer s = new CallbackServer(PORT);
        String response = s.getQuery().replaceAll("oauth_token=\\w+&oauth_verifier=(\\w+)", "$1");

        Token access = service.getAccessToken(request, new Verifier(response));
        System.out.printf("Access token: %s%n", access);
        client.setToken(access.getToken(), access.getSecret());

        return access;
    }

    /**
     * Sole constructor.
     * 
     * @param listenPort port to bind to (default 80)
     */
    public CallbackServer(int listenPort) {
        request = null;

        try {
            server = HttpServer.create(new InetSocketAddress(listenPort), 0);
            server.createContext("/callback", new MyHandler(this));
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class MyHandler implements HttpHandler {

        private final CallbackServer s;

        public MyHandler(CallbackServer s) {
            this.s = s;
        }

        public void handle(HttpExchange t) throws IOException {
            String response = readResource("callback_response_page.html");

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();

            s.setRequest(t.getRequestURI());
        }
    }
    
    private static String readResource(String name) throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream(name);
        Reader r = new BufferedReader(new InputStreamReader(is));
        
        StringBuffer sb = new StringBuffer();
        while (r.ready()) {
            sb.append((char) r.read());
        }
        r.close();

        return sb.toString();
    }

    private void setRequest(URI uri) {
        request = uri;
    }

    private void waitForQuery() {
        while (request == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        server.stop(1);
    }

    public String getQuery() {
        waitForQuery();
        return request.getQuery();
    }
    
    /*
    Multimap<String, String> getUrlParameters(String url) {
        try {
            Multimap<String, String> ret = ArrayListMultimap.create();
            for (NameValuePair param : URLEncodedUtils.parse(new URI(url), "UTF-8")) {
                ret.put(param.getName(), param.getValue());
            }
            return ret;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
     */
    
    public static void openBrowser(String url) throws IOException {
        System.out.println(Desktop.isDesktopSupported());
        Desktop d = Desktop.getDesktop();
        URI uri;
        try {
            uri = new URI(url);
            d.browse(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}