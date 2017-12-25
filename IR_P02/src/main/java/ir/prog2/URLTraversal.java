package ir.prog2;

import me.tongfei.progressbar.ProgressBar;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class URLTraversal {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0";
    private static final String RFERRRER = "http://www.google.com";
    private static final int TIME_OUT = 12 * 1000; // 12 seconds

    private int maxDepth;
    private String url;

    private static int counter = 0;
    private static char[] animationChars = new char[]{'+', 'x'};

    public URLTraversal(int depth, String url) {
        this.maxDepth = depth;
        this.url = url;
    }

    // URL Normalizations
    // https://en.wikipedia.org/wiki/URL_normalization#Normalizations_that_preserve_semantics
    public String urlNormalization(String malformedUrl) throws MalformedURLException {
        // foo://example.com:8042/over/there?name=ferret#nose
        // \_/   \______________/\_________/ \_________/ \__/
        //  |            |            |            |       |
        // scheme     authority      path        query  fragment

        URI url = null;
        String normalizedURL = null;
        try {
            url = new URI(malformedUrl).normalize();
            // System.out.println("Malformed: " + url);

            // Rule 1: Converting the scheme and host to lower case.
            String scheme = url.getScheme().toLowerCase();
            String authority = url.getAuthority().toLowerCase();

            // Remove the Port from the Authority
            String port = String.valueOf(url.getPort());
            if (authority.contains(port)) {
                authority = authority.replaceAll(":" + port, "");
            }

            String path = url.getPath();
            String query = url.getQuery();
            String fragment = url.getFragment();

            /*String build = String.format("scheme: %s | authority: %s | port: %s | path: %s | query: %s | fragment: %s",
                    scheme, port, authority, path, query, fragment);
            System.out.println(build);*/

            normalizedURL = new URI(scheme, authority, path, query, fragment).toString();

            // Rule 2: Remove the '/' from the end
            if (normalizedURL.endsWith("/")) {
                normalizedURL = normalizedURL.substring(0, normalizedURL.lastIndexOf("/"));
            }

        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }


        return normalizedURL;
    }

    public void startFetching() throws MalformedURLException {

        List<String> visitedUrls = new ArrayList<>();
        dfsLinksTraversal(this.url, visitedUrls, 0);
        System.out.println("\nFetching Completed");

        System.out.println("Listing Urls: " + visitedUrls.size());
        for (String string : visitedUrls) {
            System.out.println(string);
        }
    }

    private void dfsLinksTraversal(String url, List<String> visited, int count) {

        // mark current node as visited.
        visited.add(url);
        // System.out.println("Visited Node: " + url);

        // if recursion depth is reached then return
        if (count > maxDepth ) {
            return;
        }

        try {
            // ignoreHttpErrors:
            // In case a url throws response code 404, skip that url
            Document document = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .userAgent(USER_AGENT)
                    .referrer(RFERRRER)
                    .timeout(TIME_OUT)
                    .followRedirects(true)
                    .ignoreHttpErrors(true).get();

            Elements elements = document.select("a");
            for (Element element : elements) {
                ++counter;
                StringBuilder string = new StringBuilder();
                string
                        .append('\r')
                        .append(String.format("Fetching %c |", animationChars[counter % 2]))
                        .append(String.format(" Number of Documents Fetched: %d", counter));
                System.out.print(string);

                String link = element.absUrl("href");
                boolean validLink = Jsoup.isValid(link, Whitelist.basic());

                // Rule 1: Ignore the links that have empty
                if (link.equalsIgnoreCase("")) {
                    continue;
                }

                // Rule 2: Check if the link tag <a></a> is valid
                if (validLink == false) {
                    continue;
                }

                // Rule 3: Ignore the links that doesn't start with "http" or "https". ex: mailto
                if ( !(link.startsWith("http") || link.startsWith("https")) ) {
                    continue;
                }

                // System.out.println("link: " + link + ", valid: " + validLink + ", depth: " + count);
                if ( !visited.contains(link) ) {
                    dfsLinksTraversal(link, visited, count + 1);
                }
            }

        } catch (SocketTimeoutException ste) {
            ste.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
