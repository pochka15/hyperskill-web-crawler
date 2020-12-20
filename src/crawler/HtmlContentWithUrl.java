package crawler;

import java.net.URL;

public class HtmlContentWithUrl {
    public final String htmlContent;
    public final URL url;

    public HtmlContentWithUrl(String htmlContent, URL url) {
        this.htmlContent = htmlContent;
        this.url = url;
    }
}
