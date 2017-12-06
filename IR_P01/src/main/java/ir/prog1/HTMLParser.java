package ir.prog1;


import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * Parsing and Retrieving fields and value from HTML.
 */
public class HTMLParser {


    /**
     * Initializes a new HTMLParser instance.
     */
    HTMLParser() {

    }

    /**
     * Get the title from the html document
     * 1. Search in <title></title> tag.
     * 2. If (1) could not be read then check for meta property tag for title
     *
     * @param html HTML String passed
     * @return title string
     */
    public static String getTitle(String html) {
        String title = "";

        Document document = Jsoup.parse(html);

        title = new String(document.title());
        if (title != null && title.length() > 0) {
            return title;
        }

        // Parse og tags
        Elements metaOgTitle = document.select("meta[property^=og:]");
        if (metaOgTitle.size() > 0) {
            for (Element tag : metaOgTitle) {
                String text = tag.attr("property");
                if ("og:title".equals(text)) {
                    title = new String(tag.attr("content"));
                    break;
                }
            }
        }

        return title;
    }

    /**
     * Clean HTML tags while preserving the line breaks using JSoup
     * 1. if the original html contains newline(\n), it gets preserved
     * 2. if the original html contains br or p tags, they gets translated to newline(\n).
     *
     * @param html HTML String passed
     * @return cleaned contents
     */
    public static String getCleanedContents(String html) {
        
        // Clean HTML tags
        String cleanedHTMLTags = cleanTagPerservingLineBreaks(html);
        // Unescape HTML eg. &lt; to < with StringEscapeUtils
        String cleanedEscapedChars = StringEscapeUtils.unescapeHtml4(cleanedHTMLTags);
        // Remove URLs
        String cleanedURLS = removeUrl(cleanedEscapedChars);
        // Remove extended chars
        String cleanedExtendedChars = removeExtendedChars(cleanedURLS);

        return cleanedExtendedChars.trim().replaceAll("\\s{2,}", " ");
    }

    /**
     * Clean HTML tags while preserving the line breaks using JSoup
     * 1. if the original html contains newline(\n), it gets preserved
     * 2. if the original html contains br or p tags, they gets translated to newline(\n).
     *
     * @param html String
     * @return cleaned content with tags removed but keeping line breaks
     */
    private static String cleanTagPerservingLineBreaks(String html) {

        String result = "";
        if (html == null)
            return html;
        Document document = Jsoup.parse(html);
        document.outputSettings(new Document.OutputSettings()
                .prettyPrint(false));// makes html() preserve linebreaks and
        // spacing
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        result = document.html().replaceAll("\\\\n", "\n");
        result = Jsoup.clean(result, "", Whitelist.none(),
                new Document.OutputSettings().prettyPrint(false));
        return result;
    }

    /**
     * Remove extended chars
     * @param str String
     * @return removed extended characters
     */
    public static String removeExtendedChars(String str) {
        return str.replaceAll("[^\\x00-\\x7F]", " ");
    }

    /**
     * Remove URLs from text
     *
     * @param str String
     * @return
     */
    public static String removeUrl(String str) {
        String regex = "\\b(https?|ftp|file|telnet|http|Unsure)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        str = str.replaceAll(regex, "");
        return str;
    }


    /**
     * Example Driver Program
     **/
    public static void main(String... args) throws IOException {
        String htmlString = "<html>" +
                              "<head>" +
                                "<title>My title</title>" +
                              "</head>" +
                              "<body>Body content</body>" +
                            "</html>";

        /*Document document = Jsoup.parse(htmlString);
        String title = getTitle(document);
        String body  = document.body().text();

        System.out.printf("Title: %s\n", title);
        System.out.printf("Body: %s\n", body);*/

        String htmlUrl = "http://www.stackoverflow.com";
        Document document = Jsoup.connect(htmlUrl).get();
        String title = getTitle(document.html());
        String cleanedContent = getCleanedContents(document.html());
        System.out.println(title);
        System.out.println(cleanedContent);
    }
}
