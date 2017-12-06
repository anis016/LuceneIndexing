package ir.prog1;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;

/**
 * Main Class for the Information Retrieval System
 * Using Lucene Version: 7.1.0
 * Team Name: TeamIndexer
 * Team Member:
 * 01. Roshmitha Reddy
 * 02. S.M. Andalib Hossain
 * 03. Sayed Anisul Hoque
 */

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    // java -jar IR P01.jar [path to document folder] [path to index folder] [VS/OK] [query]
    public static void main(String... args) throws IOException {

        if(args.length != 4) {
            LOGGER.error("Usage: java -jar IR_PO1.jar [path to document folder] [path to index folder] [VS/OK] [query]");
            throw new IllegalArgumentException("Incorrect number of arguments provided (4 expected, " + args.length
                    + " provided): " + Arrays.toString(args));
        }

        // check for empty/null and fetch parameter
        String docsPath  = Preconditions.checkNotNull(args[0], "Index Path should not be empty.");
        String indexPath = Preconditions.checkNotNull(args[1], "Index Path should not be empty.");
        String model     = Preconditions.checkNotNull(args[2], "Ranking model should not be null");
        String userQuery = Preconditions.checkNotNull(args[3], "Query should not be null");

        /*LOGGER.info("docsPath: ", docsPath);
        LOGGER.info("indexPath: ", indexPath);
        LOGGER.info("model: ", model);
        LOGGER.info("user query: ", userQuery);*/

        final Path docDir = Paths.get(docsPath);
        if (!Files.isReadable(docDir)) {
            try {
                throw new IOException("Document directory '" + docDir.toAbsolutePath() +
                        "does not exist or is not readable, please check the path");

            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(1);
        }

        // Check if user wants re-create the index or use the existing indexing folder.
        boolean flag = true;
        final Path indexDir = Paths.get(indexPath);
        if (indexDir.toFile().exists()) {

            System.out.println("Index folder Path already exists !");
            Directory directory = FSDirectory.open(Paths.get(indexPath));
            if(DirectoryReader.indexExists(directory) == true) {
                System.out.println("Found indexing files. Reading from the IndexDirectory.");
            }
            System.out.println("Want to force re-create index directory ? (yes/no): ");

            Scanner readInput = new Scanner(System.in);
            String enterKey = readInput.nextLine();
            if (enterKey.equalsIgnoreCase("yes") ||  enterKey.equalsIgnoreCase("y")) {
                flag = true;
            } else {
                flag = false;
            }
        } else {
            flag = true;
        }

        // based upon user-requests, create new indexing
        if (flag == true) {
            // Start the indexing process
            Date start = new Date();
            try {
                System.out.println("Indexing to the directory '" + indexPath + "'...");
                IndexFiles.indexDocuments(docsPath, indexPath);

                Date end = new Date();
                System.out.println("Took " + String.valueOf(end.getTime() - start.getTime()) + " total milliseconds for indexing.");

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("\n");
        }

        // SearchFiles searchFiles = new SearchFiles(indexPath);
        // searchFiles.searchIndex(query);

        // Start the Ranking process using given Model (VSM/Okapi BM25)
        System.out.println("Searching for : " + userQuery);
        if (model.equalsIgnoreCase("VS")) {

            VSM vsm = new VSM(indexPath);
            vsm.calculateIDFandTF(indexPath);
            String query = DocumentPreProcessing.dataPreProcessing(userQuery);
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
            }

            Utils.printRankedDocuments(indexPath, matchedDocument);
        } else if (model.equalsIgnoreCase("OK")) {
            String query = DocumentPreProcessing.dataPreProcessing(userQuery);

            BM25 bm25 = new BM25(indexPath);
            bm25.calculateIDFandTF(indexPath);

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
            }
            Utils.printRankedDocuments(indexPath, matchedDocument);
        }
    }
}
