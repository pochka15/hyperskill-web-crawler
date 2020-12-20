package crawler.utilities;

public class IntegerUtilities {
    public static int parseIntOrDefault(String valueToParse, int defaultValue) {
        try {
            return Integer.parseInt(valueToParse);
        } catch (NumberFormatException ignored) {
        }
        return defaultValue;
    }

}
