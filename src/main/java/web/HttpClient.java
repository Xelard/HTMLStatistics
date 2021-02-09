package web;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

public class HttpClient {

    private static final Logger log = Logger.getLogger(web.HttpClient.class.getName());

    private final URL url;

    public HttpClient(String url) throws MalformedURLException, IllegalArgumentException {
        this.url = new URL(url);
        checkURL(this.url);
    }

    private static void checkURL(URL url) {
        String protocol = url.getProtocol();
        if (!protocol.startsWith("http"))
            throw new IllegalArgumentException("Supported protocols: http, https. Got: " + protocol);
    }

    public HttpResponse connect() throws IOException {
        log.info("Connecting to " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        return new HttpResponse(connection);
    }
}
