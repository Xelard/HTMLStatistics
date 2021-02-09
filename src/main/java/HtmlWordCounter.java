import web.HttpClient;
import web.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HtmlWordCounter {

    private static final Logger log = Logger.getLogger(HtmlParser.class.getName());

    private final String url;
    private final File saveFile;
    private final Pattern wordDelimiterPattern;

    private HtmlWordCounter(Builder builder) {
        url = builder.url;
        saveFile = builder.saveFile;
        wordDelimiterPattern = createDelimiterPattern(builder.wordDelimiters);
    }

    private static Pattern createDelimiterPattern(String... delimiters) {
        String pattern;
        if (delimiters.length == 0) {
            pattern = "\\s";
        } else {
            pattern = String.join("|", delimiters);
        }
        return Pattern.compile(pattern);
    }

    public void run(HttpResponse response) {
        try {
            InputStream inputStream = null;
            try {
                inputStream = response.getConnection().getInputStream();
                if (inputStream == null)
                    throw new IllegalStateException("Response has no input");

                if (writeToFile(inputStream))
                    inputStream = new FileInputStream(saveFile);

                checkContentType(response.getConnection());

                Map<String, Integer> statistics = new HashMap<>();
                new HtmlParser().parse(inputStream, response.getCharset(), data -> {
                    try (Scanner scanner = new Scanner(new String(data))) {
                        scanner.useDelimiter(wordDelimiterPattern);
                        while (scanner.hasNext()) {
                            String word = scanner.next().trim();
                            if (word.isEmpty())
                                continue;

                            Integer count = statistics.getOrDefault(word, 0);
                            statistics.put(word, ++count);
                        }
                    }
                });

                log.info("Statistics for " + url + ". Word delimiter pattern: '" + wordDelimiterPattern.pattern() + "'"
                        + System.lineSeparator() + statistics.entrySet().stream()
                        .map(v -> v.getKey() + ": " + v.getValue())
                        .collect(Collectors.joining(System.lineSeparator())));
            } finally {
                if (inputStream != null)
                    inputStream.close();
            }
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Something went wrong O_o", e);
        }
    }

    private boolean writeToFile(InputStream inputStream) throws IOException {
        if (saveFile == null)
            return false;

        log.info("Save html into " + saveFile);
        Files.copy(inputStream, saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return true;
    }

    private static void checkContentType(URLConnection connection) {
        String contentType = connection.getContentType();
        if (contentType == null || !contentType.startsWith("text/"))
            throw new IllegalStateException("Unsupported content type. Expected: text. Got: " + contentType);
    }

    public static Builder builder(String url) {
        return new Builder(url);
    }

    public static final class Builder {
        private final String url;
        private File saveFile;
        private String[] wordDelimiters = new String[]{};

        private Builder(String url) {
            this.url = url;
        }

        public Builder setSaveFile(File directory) throws IOException {
            File file;
            if (directory.isDirectory()) {
                file = File.createTempFile("web_page_", ".html", directory);
            } else {
                log.info("Directory not found, will save to temp directory");
                file = File.createTempFile("web_page_", ".html");
            }
            this.saveFile = file;
            return this;
        }

        public Builder setWordDelimiters(String... delimiters) {
            this.wordDelimiters = delimiters;
            return this;
        }

        public HtmlWordCounter build() {
            return new HtmlWordCounter(this);
        }
    }
}
