package crawler.utilities;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class WebCommunicationUtils {
    public static String fetchedHtmlContent(URLConnection connection) {
        String siteText = "";
        final String contentType = connection.getContentType();
        if (contentType != null && contentType.contains("text/html")) {
            try (final BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
                siteText = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return siteText;
    }

    public static URLConnection establishedCustomConnection(URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64;" +
                " rv:63.0) Gecko/20100101 Firefox/63.0");
        return connection;
    }
}