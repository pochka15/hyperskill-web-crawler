package crawler.utilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtilities {
    public static String parsedTitle(String htmlContent) {
        final Matcher matcher = Pattern.compile("(<title>)(.*)</title>").matcher(htmlContent);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }

    public static Collection<String> parsedLinksAsText(String htmlContent) {
        final Matcher matcher = Pattern.compile("<a.*?href=[\"'](.*?)['\"].*?>.*?</a>", Pattern.DOTALL)
                .matcher(htmlContent);
        final ArrayList<String> retLinks = new ArrayList<>();
        while (matcher.find()) {
            retLinks.add(matcher.group(1));
        }
        return retLinks;
    }

    public static URL convertedToAbsoluteUrl(String urlAsString, URL originalAbsoluteUrl)
            throws MalformedURLException {
        final Optional<URL> convertedUrl = ((StringToUrlConverter)
                url -> url.startsWith("http") ? UrlConverters.toURL(url) : Optional.empty())
                .orElse(url -> UrlConverters.relativeToAbsolute(url, originalAbsoluteUrl.toExternalForm()))
                .orElse(UrlConverters::withProtocol).convertedUrl(urlAsString);
        if (convertedUrl.isPresent()) {
            return convertedUrl.get();
        } else {
            throw new MalformedURLException("Unable to convert url: " + urlAsString + " to absolute url");
        }
    }
}