package web;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpResponse implements Closeable {

    private static final Logger log = Logger.getLogger(web.HttpResponse.class.getName());

    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset\\s*=\\s*[\"']?([^\\s,;\"']*)");
    private final HttpURLConnection connection;
    private String charset;
    private boolean charsetComputed;

    public HttpResponse(HttpURLConnection connection) {
        this.connection = connection;
    }

    public HttpURLConnection getConnection() {
        return connection;
    }

    public void checkOk() throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            InputStream errorStream = connection.getErrorStream();
            String error = "";
            if (errorStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                    error += System.lineSeparator() + reader.lines().collect(Collectors.joining(System.lineSeparator()));
                } catch (IOException e) {
                    log.warning("Cannot read the error stream");
                }
            }
            throw new IllegalStateException("Response is not OK: " + responseCode + " - " + connection.getResponseMessage()
                    + error);
        }
    }

    public String getCharset() {
        if (charsetComputed)
            return charset;

        String contentType = connection.getContentType();
        if (contentType != null) {
            Matcher matcher = CHARSET_PATTERN.matcher(contentType);
            if (matcher.find()) {
                charset = matcher.group(1);
                if (charset != null)
                    charset = charset.trim();
            }
        }
        charsetComputed = true;

        return charset;
    }

    @Override
    public void close() {
        connection.disconnect();
    }
}
