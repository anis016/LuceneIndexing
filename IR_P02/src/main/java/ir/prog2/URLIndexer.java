package ir.prog2;

import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class URLIndexer {

    // Related to Jsoup
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0";
    private static final String RFERRRER = "http://www.google.com";
    private static final int TIME_OUT = 12 * 1000; // 12 seconds

    // Related to URL and Depth
    private int maxDepth;
    private String url;

    // Related to Fetching Animation
    private static int counter = 1;
    private static char[] animationChars = new char[]{'+', 'x'};

    public URLIndexer(int depth, String url) {
        this.maxDepth = depth;
        this.url = url;
    }

    /**
     * Create the Indexwriter for indexing.
     *
     * @param indexPath Index path where the given file/dir info will be stored
     * @throws IOException If there is a low-level I/O error
     */
    private IndexWriter createWriter(String indexPath) throws IOException {

        // Get the path for the indexing
        FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        IndexWriter writer = new IndexWriter(dir, iwc);

        return writer;
    }

    // URL Normalizations
    // https://en.wikipedia.org/wiki/URL_normalization#Normalizations_that_preserve_semantics
    public String urlNormalization(String malformedUrl) throws MalformedURLException {
        /*String text1 = "http://www.Example.com";
        String text2 = "HTTP://www.EXAMPLE.com:8080/a%c2%b1b";
        String text3 = "http://www.Example.com:80/%7Eusername/";*/

        // foo://example.com:8042/over/there?name=ferret#nose
        // \_/   \______________/\_________/ \_________/ \__/
        //  |            |            |            |       |
        // scheme     authority      path        query  fragment

        URI url;
        String normalizedURL;
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

    /**
     * Delete's recursively the folder and the contents.
     *
     * @param folder The folder to delete
     */
    public void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public void startFetchingAndIndexing(String indexDir) throws IOException {

        if (indexDir.endsWith("/")) {
            indexDir = indexDir.substring(0, indexDir.lastIndexOf("/"));
        }

        if ( new File(indexDir).exists() ) {
            deleteFolder(new File(indexDir));
        }

        // Creating new Index Directory
        IndexWriter indexWriter = createWriter(indexDir);

        // Adding files to the pages.txt
        final String fileName = indexDir.toString() + "/pages.txt";
        try {
            if ( ! new File(fileName).getParentFile().exists()) {
                new File(fileName).getParentFile().mkdirs();
            }
            new File(fileName).createNewFile();

        } catch (IOException e) {
            e.printStackTrace();
        }

        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            fileWriter = new FileWriter(fileName, true);
            bufferedWriter = new BufferedWriter(fileWriter);

            // Adding the seeder URL depth = 0 and Make it Normalized
            bufferedWriter.write(urlNormalization(this.url) + "\t" + 0 + "\n");
            String normalizedURL = urlNormalization(this.url);
            Document document = Jsoup.connect(normalizedURL)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .userAgent(USER_AGENT)
                    .referrer(RFERRRER)
                    .timeout(TIME_OUT)
                    .followRedirects(true)
                    .ignoreHttpErrors(true).get();
            String htmlFile = document.html();
            indexDocument(1, htmlFile, normalizedURL, indexWriter);

            List<String> visitedUrls = new ArrayList<>();
            dfsLinksTraversal(normalizedURL, bufferedWriter, indexWriter, visitedUrls, 1);
        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                if (bufferedWriter != null)
                    bufferedWriter.close();
                if (fileWriter != null)
                    fileWriter.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        indexWriter.commit();
        indexWriter.close();

        System.out.println("\n\nIndexing Completed");
    }

    private void dfsLinksTraversal(String url, BufferedWriter bufferedWriter,
                                   IndexWriter indexWriter, List<String> visited,
                                   int count) throws MalformedURLException {

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
                counter++;
                StringBuilder string = new StringBuilder();
                string
                        .append('\r')
                        .append(String.format("Fetching %c |", animationChars[counter % 2]))
                        .append(String.format(" Number of Documents Fetched: %3d |", counter))
                        .append(String.format(" Number of Documents Indexed: %3d", visited.size()));

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

                // Make the link Normalized
                String normalizedLink = urlNormalization(link);

                // System.out.println("link: " + link + ", valid: " + validLink + ", depth: " + count);
                if ( !visited.contains(normalizedLink) ) {

                    bufferedWriter.write(normalizedLink + "\t" + count + "\n");
                    String htmlFile = Jsoup.connect(normalizedLink)
                            .ignoreContentType(true)
                            .method(Connection.Method.GET)
                            .userAgent(USER_AGENT)
                            .referrer(RFERRRER)
                            .timeout(TIME_OUT)
                            .followRedirects(true)
                            .ignoreHttpErrors(true).get().html();

                    indexDocument(counter, htmlFile, normalizedLink, indexWriter);

                    dfsLinksTraversal(normalizedLink, bufferedWriter, indexWriter, visited, count + 1);
                }
            }

        } catch (SocketTimeoutException ste) {

        } catch (IOException ioe) {

        }
    }

    public void indexDocument(long counter, String htmlFile, String url, IndexWriter writer) throws IOException {

        String htmlTitle = HTMLParser.getTitle(htmlFile);
        String htmlBody = HTMLParser.getCleanedContents(htmlFile);

        String id    = String.valueOf(counter);
        String title = DocumentPreProcessing.dataPreProcessing(htmlTitle);
        String body  = DocumentPreProcessing.dataPreProcessing(htmlBody);

        // Make it a Lucene document
        org.apache.lucene.document.Document document = createDocument(id, title, htmlTitle, body, url);
        writer.addDocument(document);
    }

    /**
     * Create the Document for indexing.
     *
     * @param title Title of the document
     * @param htmlTitle HTML Title of the document
     * @param body Body of the document
     * @param url URL of the document
     */
    private org.apache.lucene.document.Document createDocument(String id, String title,
                               String htmlTitle, String body,
                               String url) {

        FieldType fieldType = new FieldType(TextField.TYPE_STORED);
        fieldType.setStoreTermVectors(true);

        org.apache.lucene.document.Document document = new org.apache.lucene.document.Document();
        document.add(new StringField(LuceneConstants.FIELD_ID, id, Field.Store.YES));
        document.add(new StringField(LuceneConstants.FIELD_TITLE, title, Field.Store.YES));
        document.add(new StringField(LuceneConstants.FIELD_HTML_TITLE, htmlTitle, Field.Store.YES));
        document.add(new Field(LuceneConstants.FIELD_CONTENTS, body, fieldType));
        document.add(new StringField(LuceneConstants.FIELD_URL, url, Field.Store.YES));

        return document;
    }
}
