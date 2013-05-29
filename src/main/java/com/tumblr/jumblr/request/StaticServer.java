package com.tumblr.jumblr.request;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * An HTTP server that returns a static page and offers up the requests that have been sent to it.
 * 
 * NOTE: I modified this from http://stackoverflow.com/a/3732328/1772907
 * 
 * @author Jackson
 */
@SuppressWarnings("restriction")
public class StaticServer {

    private final List<URI> requests;
    private final HttpServer server;

    /**
     * Creates a server to listen on the given port and path.
     * 
     * @param listenPort port to bind to
     * @param listenPath path to listen to (default "/callback")
     * @param responsePageLocation (default "callback_response_page.html")
     * @throws IOException 
     */
    public StaticServer(int listenPort, String listenPath, String responsePage) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(listenPort), 0);
        server.createContext(listenPath, new RequestHandler(this, responsePage));
        server.setExecutor(null); // creates a default executor
        server.start();
        
        this.requests = Collections.synchronizedList(new LinkedList<URI>());
    }
    
    public StaticServer(URI url, String responsePage) throws IOException {
        this(url.getPort(), url.getPath(), responsePage);
    }

    static class RequestHandler implements HttpHandler {

        private final StaticServer s;
        private String response;

        public RequestHandler(StaticServer s, String response) {
            this.s = s;
            this.response = response;
        }

        public void handle(HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();

            s.addRequest(t.getRequestURI());
        }
    }

    private void addRequest(URI uri) {
        requests.add(uri);
    }
    
    public URI waitForNextRequest() {
        while (requests.isEmpty()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return requests.get(0);
    }
    
    public void stop() {
        server.stop(1);
    }
}
