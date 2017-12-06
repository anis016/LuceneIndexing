package ir.prog1;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * VSM model for ranked retrieval.
 */
public class VSM {
    String indexPath = "";
    HashMap<String, HashMap<String, Integer>> documentTFMappings;
    HashMap<String, Double> invertedDictionary;

    /**
     * Initializes a new VSM instance.
     * @param indexPath index path
     */
    public VSM(String indexPath) {

        this.indexPath = indexPath;
        this.documentTFMappings = new HashMap<>();
        this.invertedDictionary = new HashMap<>();
    }

    /**
     * Calculates the IDF and TF for the documents indexed
     * @param indexPath indexing path
     * @throws IOException if path value couldnot be read
     */
    public void calculateIDFandTF(String indexPath) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexPath));
        IndexReader indexReader = DirectoryReader.open(directory);

        int numberOfdocs = indexReader.numDocs();
        for (int docId = 0; docId < numberOfdocs; docId++) {

            Terms termVector = indexReader.getTermVector(docId, LuceneConstants.FIELD_CONTENTS);
            if (termVector == null) {
                // System.out.println("No Term Vector Found");
                continue;
            }
            Document document = indexReader.document(docId);
            String documentContent = new String(document.get(LuceneConstants.FIELD_CONTENTS));
            String documentID = new String(document.get(LuceneConstants.FIELD_ID));
            // System.out.println("Doc:: " + documentID + ", Content: " + documentContent);

            TermsEnum iterator = termVector.iterator();
            BytesRef term = null;
            List<String> terms = new ArrayList<>();
            while ((term = iterator.next()) != null) {

                try {
                    String termText = term.utf8ToString();
                    Term termInstance = new Term(LuceneConstants.FIELD_CONTENTS, term);
                    long termFrequency = indexReader.totalTermFreq(termInstance);
                    long documentCount = indexReader.docFreq(termInstance);
                    terms.add(termText);

                    Double idf = Math.log10((double)numberOfdocs/(double)termFrequency);
                    if(!invertedDictionary.containsKey(term)) {
                        invertedDictionary.put(termText, idf);
                    }

                    // System.out.println("doc:" + docId + ", term: " + termText + ", termFreq = " + termFrequency + ", docCount = " + documentCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            HashMap<String, Integer> termFrequency = Utils.getTermFrequencyOfEachWord(documentContent.trim(), terms);
            this.documentTFMappings.put(documentID, termFrequency);
        }

    }

    /**
     * Calculates the dot product between two lists (query and the document)
     * @param v1 document
     * @param v2 query
     * @return dot product value
     */
    public double getDotProduct(List<Double> v1, List<Double> v2) {
        int listSize = v1.size();
        double sum = 0.0;
        for(int i = 0; i < listSize; i++) {
            sum += (v1.get(i) * v2.get(i));
        }
        return sum;
    }

    /**
     * Normalizes the score of two lists (query and the document)
     * @param v1 document
     * @param v2 query
     * @return normalized value
     */
    public double getNormalize(List<Double> v1, List<Double> v2) {
        int listSize = v1.size();
        double sum1 = 0.0;
        double sum2 = 0.0;
        for(int i = 0; i < listSize; i++) {
            double val1 = v1.get(i);
            sum1 += (val1 * val1);

            double val2 = v2.get(i);
            sum2 += (val2 * val2);
        }

        return Math.sqrt(sum1 * sum2);
    }

    /**
     * Calculates the cosine score (dotproduct/normalize) for the query and the document.
     * @param v1 document
     * @param v2 query
     * @return score
     */
    public double cosineSimilarity(HashMap<String, Double> v1, HashMap<String, Double> v2) {
        List<Double> lv1 = new ArrayList<>();
        for(Map.Entry<String, Double> entry : v1.entrySet()) {
            lv1.add(entry.getValue());
        }

        List<Double> lv2 = new ArrayList<>();
        for(Map.Entry<String, Double> entry : v2.entrySet()) {
            lv2.add(entry.getValue());
        }

        double dotProduct = this.getDotProduct(lv1, lv2);
        double normalize  = this.getNormalize(lv1, lv2);

        return dotProduct/normalize;
    }

    /**
     * Flattens the query and document lists and get all the unique keys
     * @param document document
     * @param query query
     * @return set of keys
     */
    public Set getFlattenTerms(HashMap<String, Integer> document, HashMap<String, Integer> query) {
        Set words = new HashSet();

        for (Map.Entry<String, Integer> token : document.entrySet()) {
            words.add(token.getKey());
        }

        for (Map.Entry<String, Integer> token : query.entrySet()) {
            words.add(token.getKey());
        }

        return words;
    }

    /**
     * Creates the TFIDF Document Matrix
     * @param uniqueKeySets All the keys of query and document
     * @param document document
     * @return TFIDF Matrix
     */
    public HashMap<String, Double> createTFIDFDocumentQueryMatrix(Set uniqueKeySets, HashMap<String, Integer> document) {
        HashMap<String, Double> tfIDFDictionary = new HashMap<>();

        for(Object object : uniqueKeySets) {
            String key = (String) object;

            double idf = 0.0;
            if (invertedDictionary.containsKey(key)) {
                idf = invertedDictionary.get(key);
            }

            double tfidf = 0.0;
            for(Map.Entry<String, Integer> termsEntry : document.entrySet()) {
                String queryKey = termsEntry.getKey();
                Integer frequency = termsEntry.getValue();

                tfidf = 0.0;
                if (key.equalsIgnoreCase(queryKey)) {
                    tfidf = (double) frequency * idf;
                    break;
                }
            }
            tfIDFDictionary.put(key, tfidf);
        }

        return tfIDFDictionary;
    }

    /*public static void main(String... args) throws IOException {

        String indexPath = "/home/anis/index";
        VSM vsm = new VSM(indexPath);
        vsm.documentIndexing(indexPath);
        String query = DocumentPreProcessing.dataPreProcessing("to berlin girl");
        HashMap<String, Integer> hashedQuery = Utils.makeQuery(query);

        HashMap<String, Double> matchedDocument = new HashMap<>();
        for (Map.Entry<String, HashMap<String, Integer>> document : vsm.documentTFMappings.entrySet()) {
            String documentId = document.getKey();
            HashMap<String, Integer> value = document.getValue();

            Set flattenUniqueTerms = vsm.getFlattenTerms(value, hashedQuery);

            HashMap<String, Double> doc = vsm.createTFIDFDocumentQueryMatrix(flattenUniqueTerms, value);
            HashMap<String, Double> q   = vsm.createTFIDFDocumentQueryMatrix(flattenUniqueTerms, hashedQuery);

            double similarityScore = vsm.cosineSimilarity(doc, q);
            if (similarityScore > 0.0) {
                Double score = similarityScore;
                score = Double.parseDouble(String.format("%.5f", score));
                matchedDocument.put(documentId, score);
            }
            // System.out.println("Matching document: " + documentId + " with Query: " + query + ", Similarity Score: " + similarityScore);
        }

        Utils.printRankedDocuments(indexPath, matchedDocument);
    }*/
}
