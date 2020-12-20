package crawler.utilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface StringToUrlConverter {
    Optional<URL> convertedUrl(String urlAsString);

    default StringToUrlConverter orElse(StringToUrlConverter other) {
        return urlAsString -> convertedUrl(urlAsString).or(() -> other.convertedUrl(urlAsString));
    }
}

public class UrlConverters {
    public static Optional<URL> relativeToAbsolute(String relativeUrlAsString, String absoluteUrlAsString) {
        if (!relativeUrlAsString.startsWith("/")) {
            final int lastSlashIndex = absoluteUrlAsString.lastIndexOf('/');
            try {
                return Optional.of(new URL(absoluteUrlAsString.substring(0, lastSlashIndex + 1).concat(relativeUrlAsString)));
            } catch (MalformedURLException ignored) {
            }
        }
        return Optional.empty();
    }

    public static Optional<URL> withProtocol(String urlWithoutProtocolAsString) {
        final Matcher matcher = Pattern.compile("^/*(.*)").matcher(urlWithoutProtocolAsString); // group 1 is without first slashes
        if (matcher.find() && urlWithoutProtocolAsString.contains("/")) {
            try {
                return Optional.of(new URL("https://" + matcher.group(1)));
            } catch (MalformedURLException ignored) {
            }
        }
        return Optional.empty();
    }

    public static Optional<URL> toURL(String urlAsString) {
        try {
            return Optional.of(new URL(urlAsString));
        } catch (MalformedURLException ignored) {
        }
        return Optional.empty();
    }
}