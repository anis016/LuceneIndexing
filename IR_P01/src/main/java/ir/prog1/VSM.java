package ir.prog1;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

class ValueComparator implements Comparator<String> {
    Map<String, Double> base;

    public ValueComparator(Map<String, Double> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with
    // equals.
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}

public class VSM {
    String indexPath = "";
    HashMap<String, HashMap<String, Integer>> documentTFMappings;
    HashMap<String, Double> invertedDictionary;

    public VSM(String indexPath) {

        this.indexPath = indexPath;
        this.documentTFMappings = new HashMap<>();
        this.invertedDictionary = new HashMap<>();
    }

    public String documentSimilarity(String indexPath) throws IOException {
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
            System.out.println("Doc:: " + documentID + ", Content: " + documentContent);

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
                    if(!invertedDictionary.containsKey(term)) { // this doesn't work.. why ?
                        invertedDictionary.put(termText, idf);
                    }

                    // System.out.println("doc:" + docId + ", term: " + termText + ", termFreq = " + termFrequency + ", docCount = " + documentCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            HashMap<String, Integer> termFrequency = this.getTermFrequencyOfEachWord(documentContent.trim(), terms);
            this.documentTFMappings.put(documentID, termFrequency);
        }

        return null;
    }

    public HashMap<String, Integer> getTermFrequencyOfEachWord(String document, List<String> terms) {
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

    public double getDotProduct(List<Double> v1, List<Double> v2) {
        int listSize = v1.size();
        double sum = 0.0;
        for(int i = 0; i < listSize; i++) {
            sum += (v1.get(i) * v2.get(i));
        }
        return sum;
    }

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

    public HashMap<String, Integer> makeQuery(String document) {
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

    public void printRankedDocuments(String indexPath, HashMap<String, Double> matchedDocuments) throws IOException {
        ValueComparator bvc = new ValueComparator(matchedDocuments);
        TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(bvc);

        sorted_map.putAll(matchedDocuments);

        Directory directory = FSDirectory.open(Paths.get(indexPath));
        IndexReader indexReader = DirectoryReader.open(directory);

        for (Map.Entry<String, Double> entry : sorted_map.entrySet()) {
            int key = Integer.parseInt(entry.getKey());
            Document document = indexReader.document(key);
            String documentID = new String(document.get(LuceneConstants.FIELD_ID));
            String documentContents = new String(document.get(LuceneConstants.FIELD_CONTENTS));

            System.out.println("Matching document: " + documentID + " with contents: " + documentContents.trim() + ", Similarity Score: " + entry.getValue());
        }
    }

    public static void main(String... args) throws IOException {

        String indexPath = "/home/anis/index";
        VSM vsm = new VSM(indexPath);
        vsm.documentSimilarity(vsm.indexPath);
        String query = DocumentPreProcessing.dataPreProcessing("to berlin girl");
        HashMap<String, Integer> hashedQuery = vsm.makeQuery(query);

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
            System.out.println("Matching document: " + documentId + " with Query: " + query + ", Similarity Score: " + similarityScore);
        }

        vsm.printRankedDocuments(indexPath, matchedDocument);

        /*for (Map.Entry<Integer, HashMap<String, Integer>> document : vsm.documentTFMappings.entrySet()) {
            Integer docId = document.getKey();
            HashMap<String, Integer> value = document.getValue();

            if (docId == 0) {
                for (Map.Entry<Integer, HashMap<String, Integer>> document2 : vsm.documentTFMappings.entrySet()) {
                    Integer docId2 = document2.getKey();
                    HashMap<String, Integer> value2 = document2.getValue();

                    if (docId2 == 1) {
                        Set flattenUniqueTerms = getFlattenTerms(value, value2);
                        HashMap<String, Double> doc1 = vsm.createTFIDFDocumentQueryMatrix(flattenUniqueTerms, value);
                        HashMap<String, Double> doc2 = vsm.createTFIDFDocumentQueryMatrix(flattenUniqueTerms, value2);

                        double result = vsm.cosineSimilarity(doc1, doc2);
                        System.out.println(result);
                        break;
                    }
                }
            }
        }*/

        /*for (Map.Entry<Integer, HashMap<String, Integer>> document : vsm.documentTFMappings.entrySet()) {
            Integer docId = document.getKey();
            HashMap<String, Integer> value = document.getValue();

            for(Map.Entry<String, Integer> entry : value.entrySet()) {
                String term = entry.getKey();
                Integer frequency = entry.getValue();

                System.out.println("DocID: " + docId + ", Term: " + term + ", Frequency: " + frequency);
            }
        }

        for(Map.Entry<String, Double> dictionary : vsm.invertedDictionary.entrySet()) {
            String keyTerm = dictionary.getKey();
            Double idf = dictionary.getValue();

            System.out.println("KeyTerm: " + keyTerm + ", IDF: " + idf);
        }*/

        /*StandardAnalyzer analyzer = new StandardAnalyzer();
        CharArraySet stopwords = analyzer.getStopwordSet();
        Iterator iterator = stopwords.iterator();
        while (iterator.hasNext()){
            char[] charStopword = (char[]) iterator.next();
            String stopword = new String(charStopword);
            System.out.println(stopword);
        }*/
    }
}
