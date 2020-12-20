package crawler.actions;

import crawler.HtmlContentWithUrl;
import crawler.utilities.HtmlUtilities;
import crawler.utilities.WebCommunicationUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static crawler.utilities.WebCommunicationUtils.establishedCustomConnection;

/**
 * Action that fetches pages and sends them to the consumer.
 * It can be interrupted during the fetching process. It won't go deeper level after an interruption.
 */
public class CrawlHtmlPages implements InterruptibleAction {
    private final URL seedUrl;
    private final Integer workersNumber;
    private final Integer crawlDepth;
    private final Consumer<HtmlContentWithUrl> newlyFetchedPageConsumer;
    private boolean shouldStopCrawling = false;

    public CrawlHtmlPages(URL seedUrl,
                          Integer workersNumber,
                          Integer crawlDepth,
                          Consumer<HtmlContentWithUrl> newlyFetchedPageConsumer) {
        this.seedUrl = seedUrl;
        this.workersNumber = workersNumber;
        this.crawlDepth = crawlDepth;
        this.newlyFetchedPageConsumer = newlyFetchedPageConsumer;
    }

    @Override
    public void execute() {
        try {
            final Set<String> handledUrls = new HashSet<>();

            handledUrls.add(seedUrl.toString());
            Collection<URL> seedUrls = List.of(seedUrl);
            List<List<URL>> collectedListsOfUrlsInCurrentIteration = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < crawlDepth && !shouldStopCrawling && seedUrls.size() > 0; i++) {
//                Configure actions
                final boolean isLastDepthLevel = (i == crawlDepth - 1);
                final List<Action> configuredActions =
                        seedUrls.stream()
                                .map(url -> (Action) () -> {
                                    String htmlContent = fetchedHtmlContent(url);
                                    if (!htmlContent.isBlank())
                                        newlyFetchedPageConsumer.accept(new HtmlContentWithUrl(htmlContent, url));
                                    if (!isLastDepthLevel) {
                                        collectedListsOfUrlsInCurrentIteration.add(
                                                parsedAbsoluteUrls(url, htmlContent));
                                    }
                                })
                                .collect(toList(seedUrls.size()));
//                Execute actions
                new InParallel(configuredActions,
                               workersNumber,
                               10,
                               TimeUnit.SECONDS)
                        .execute();
//                Update seed urls and handled urls
                final Map<String, URL> newSeedUrlsMap = collectedListsOfUrlsInCurrentIteration
                        .stream()
                        .flatMap(Collection::stream)
                        .filter(url -> !handledUrls.contains(url.toString()))
                        .collect(Collectors.toMap(URL::toString, url -> url, (url, url2) -> url));
                seedUrls = newSeedUrlsMap.values();
                handledUrls.addAll(newSeedUrlsMap.keySet());
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private List<URL> parsedAbsoluteUrls(URL originalAbsoluteUrl, String htmlContent) {
        return HtmlUtilities.parsedLinksAsText(htmlContent)
                .stream()
                .map(urlAsString -> {
                    try {
                        return HtmlUtilities.convertedToAbsoluteUrl(urlAsString, originalAbsoluteUrl);
                    } catch (MalformedURLException ignored) {
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String fetchedHtmlContent(URL url) {
        final URLConnection connection;
        try {
            connection = establishedCustomConnection(url);
            return WebCommunicationUtils.fetchedHtmlContent(connection);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static <T> Collector<T, ?, List<T>> toList(int size) {
        return Collectors.toCollection(() -> new ArrayList<>(size));
    }

    @Override
    public String toString() {
        return "FetchData";
    }

    @Override
    public void interrupt() {
        shouldStopCrawling = true;
    }
}