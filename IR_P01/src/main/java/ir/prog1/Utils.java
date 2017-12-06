package ir.prog1;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Provides common functionality.
 */
public class Utils {

    /**
     * Sorts the matched documents and Prints accordingly.
     * @param indexPath index path
     * @param matchedDocuments matched documents
     */
    public static void printRankedDocuments(String indexPath, HashMap<String, Double> matchedDocuments) throws IOException {

        System.out.println("Searched query matched with total " + matchedDocuments.size() + " documents.\n");

        ValueComparator bvc = new ValueComparator(matchedDocuments);
        TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(bvc);

        sorted_map.putAll(matchedDocuments);

        Directory directory = FSDirectory.open(Paths.get(indexPath));
        IndexReader indexReader = DirectoryReader.open(directory);
        int counter = 0;

        for (Map.Entry<String, Double> entry : sorted_map.entrySet()) {
            if (counter == 10)
                break;

            int key = Integer.parseInt(entry.getKey());
            Document document = indexReader.document(key);
            String documentID = new String(document.get(LuceneConstants.FIELD_ID));
            String documentHTMLTitle = new String(document.get(LuceneConstants.FIELD_HTML_TITLE));
            String documentPath = new String(document.get(LuceneConstants.FIELD_PATH));

            System.out.println("Rank: " + (counter+1) + ", Title: " + documentHTMLTitle.trim() + ", Relevance Score: " + entry.getValue() + ", Path: " + documentPath);
            counter ++;
        }
    }

    /**
     * Create TermFrequency Map from the given document
     * @param document matched documents
     * @return Term-Frequency Map
     */
    public static HashMap<String, Integer> makeQuery(String document) {
        Set words = new HashSet();
        List<String> tokens = Arrays.asList(document.trim().split(" "));
        for (String token : tokens) {
            words.add(token);
        }

        HashMap<String, Integer> termFrequency = new HashMap<>();
        for(Object object : words) {
            String term = (String) object;
            int count = 0;
            for (String token : tokens) {
                if (term.equalsIgnoreCase(token)) {
                    count ++;
                }
            }
            termFrequency.put(term, count);
        }

        return termFrequency;
    }

    /**
     * Create TermFrequency Map of the given term from the document
     * @param document matched documents
     * @param terms list of terms
     * @return Term-Frequency Map
     */
    public static HashMap<String, Integer> getTermFrequencyOfEachWord(String document, List<String> terms) {
        HashMap<String, Integer> termFrequency = new HashMap<>();
        // Explode the document into an ArrayList
        String[] tokens = document.split(" ");
        for(String term : terms) {
            int count = 0;
            for (String token : tokens) {
                if (term.equalsIgnoreCase(token)) {
                    count ++;
                }
            }

            termFrequency.put(term, count);
        }
        return termFrequency;
    }
}
