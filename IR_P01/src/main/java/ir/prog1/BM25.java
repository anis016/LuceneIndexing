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
 * Okapi BM25 model for ranked retrieval.
 */
public class BM25 {

    String indexPath = "";
    HashMap<String, HashMap<String, Integer>> documentTFMappings;
    HashMap<String, Double> invertedDictionary;
    HashMap<String, Integer> documentFrequency;
    double averageLength;

    /**
     * Initializes a new BM25 instance.
     * @param indexPath index path
     */
    public BM25(String indexPath) {

        this.indexPath = indexPath;
        this.documentTFMappings = new HashMap<>();
        this.invertedDictionary = new HashMap<>();
        this.documentFrequency = new HashMap<>();
        this.averageLength = 0.0;
    }

    /**
     * Calculates the IDF and TF for the documents indexed
     * @param indexPath indexing path
     * @throws IOException if path value couldnot be read
     */
    public void calculateIDFandTF(String indexPath) throws IOException {

        Directory directory = FSDirectory.open(Paths.get(indexPath));
        IndexReader indexReader = DirectoryReader.open(directory);

        long sumTf = 0;
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
                    long totalTermFreq = indexReader.totalTermFreq(termInstance);
                    int termFrequency = this.getTermFrequency(documentContent.trim(), termText);
                    terms.add(termText);

                    Double idf = Math.log10((double)numberOfdocs/(double)totalTermFreq);
                    if(!invertedDictionary.containsKey(term)) {
                        invertedDictionary.put(termText, idf);
                    }

                    sumTf += termFrequency;

                    // System.out.println("doc:" + docId + ", term: " + termText + ", TotalTermFreq = " + totalTermFreq);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            HashMap<String, Integer> termFrequency = Utils.getTermFrequencyOfEachWord(documentContent.trim(), terms);
            this.documentTFMappings.put(documentID, termFrequency);
        }
        this.calculateAverageLength(sumTf, numberOfdocs);
    }

    /**
     * Calculates the Average Length of the Document
     * @param totalTermFrequency total Term Frequency
     * @param numberOfDocs total number of documents
     */
    public void calculateAverageLength(long totalTermFrequency, int numberOfDocs) {

        double averageLength = (double) totalTermFrequency / (double) numberOfDocs;
        this.averageLength = averageLength;
    }

    /**
     * Given a document it calculates the number of given term in the document
     * @param document a document
     * @param term a term
     * @return number of term in the document
     */
    public int getTermFrequency(String document, String term) {
        int termFrequency = 0;
        String[] tokens = document.split(" ");
        for (String token : tokens) {
            if (term.equalsIgnoreCase(token)) {
                termFrequency ++;
            }
        }
        return termFrequency;
    }

    /**
     * Calculates the length of the document
     * @param document document
     * @return document length
     */
    public double getDocumentLength(HashMap<String, Integer> document) {
        double sum = 0;
        for(Map.Entry<String, Integer> entry : document.entrySet()) {
            sum += entry.getValue();
        }

        return (double) sum;
    }

    /**
     * Calculates the BM25 score for the query and the document
     * @param document document
     * @param q query
     * @return score
     */
    public double calculateBM25Score(HashMap<String, Integer> document, HashMap<String, Integer> q) {

        // Calculations from, Slide: IR05_ProbabilisticModel, Page, 55
        // query q
        List<String> queries = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : q.entrySet()) {
            queries.add(entry.getKey());
        }

        double result = 0.0;

        // calculate the BM25 TF
        double k1 = 1.2;
        double b  = 0.75;
        double documentLength = this.getDocumentLength(document);
        // Iterate over the terms in Query
        for (String query : queries) {
            int termFrequency = 0;
            double inverseDocumentFrequency = 0.0;
            if (document.containsKey(query)) {
                termFrequency = document.get(query);
            }

            if (this.invertedDictionary.containsKey(query)) {
                inverseDocumentFrequency = invertedDictionary.get(query);
            }

            double numerator = (k1 + 1.0) * termFrequency;
            double denominator = k1 * ((1.0 - b) + b * (documentLength / this.averageLength)) + termFrequency;
            double TF = numerator / denominator;

            result += inverseDocumentFrequency * TF;
        }

        return result;
    }

    /*public static void main(String... args) throws IOException {
        String indexPath = "/home/anis/index";
        String query = DocumentPreProcessing.dataPreProcessing("berlin girl");

        BM25 bm25 = new BM25(indexPath);
        bm25.documentIndexing(indexPath);

        HashMap<String, Integer> hashedQuery = Utils.makeQuery(query);
        HashMap<String, Double> matchedDocument = new HashMap<>();
        for (Map.Entry<String, HashMap<String, Integer>> document : bm25.documentTFMappings.entrySet()) {

            String documentId = document.getKey();
            HashMap<String, Integer> value = document.getValue();

            double bm25Score = bm25.calculateBM25Score(value, hashedQuery);

            if (bm25Score > 0.0) {
                Double score = bm25Score;
                score = Double.parseDouble(String.format("%.5f", score));
                matchedDocument.put(documentId, score);
            }
            // System.out.println("Matching document: " + documentId + " with Query: " + query + ", Similarity Score: " + bm25Score);
        }
        Utils.printRankedDocuments(indexPath, matchedDocument);
    }*/
}
