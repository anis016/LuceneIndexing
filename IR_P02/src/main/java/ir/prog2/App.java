package ir.prog2;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Main Class for the Web Search Engine using Lucene
 * Using Lucene Version: 7.1.0
 * Team Name: TeamIndexer
 * Team Member:
 * 01. Roshmitha Thummala
 * 02. S.M. Andalib Hossain
 * 03. Sayed Anisul Hoque
 */
public class App {

    /**
     * Driver Class
     * Call: java -jar IR_P02.jar [seed URL] [crawl depth] [path to index folder] [query]
     * @param args arguments
     * @throws IOException if path value could not be read
     */
    public static void main(String... args) throws IOException {

        if(args.length != 4) {
            // LOGGER.error("Usage: java -jar IR_PO1.jar [path to document folder] [path to index folder] [VS/OK] [query]");
            System.out.println("java -jar IR P02.jar [seed URL] [crawl depth] [path to index folder] [query]");
            throw new IllegalArgumentException("Incorrect number of arguments provided (4 expected, " + args.length
                    + " provided): " + Arrays.toString(args));
        }

        // check for empty/null and fetch parameters
        String seedURL    = Preconditions.checkNotNull(args[0], "Seed URL should not be null.");
        String crawlDepth = Preconditions.checkNotNull(args[1], "Crawl depth should not be null.");
        String indexPath  = Preconditions.checkNotNull(args[2], "Index path should not be null");
        String userQuery  = Preconditions.checkNotNull(args[3], "Query should not be null");

        int depth = Integer.parseInt(crawlDepth);
        URLIndexer urlIndexer = new URLIndexer(depth, seedURL);

        // Check if user wants re-create the index or use the existing indexing folder.
        boolean flag;
        final Path indexDir = Paths.get(indexPath);
        if (indexDir.toFile().exists()) {

            System.out.println("Index folder Path already exists !");
            Directory directory = FSDirectory.open(Paths.get(indexPath));
            if(DirectoryReader.indexExists(directory) == true) {
                System.out.println("Found indexing files. Reading from the IndexDirectory.");
                System.out.print("Want to force re-create index directory ? (yes/no): ");

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
        } else {
            flag = true;
        }

        if (flag) {

            // Start the indexing process
            try {
                System.out.println("Indexing to the directory '" + indexPath + "'..." + "\n");

                // Start the fetching of documents.
                urlIndexer.startFetchingAndIndexing(indexPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\nSeeder URL: " + urlIndexer.urlNormalization(seedURL) );
        System.out.println("Searching for : " + userQuery + "\n");
        String query = DocumentPreProcessing.dataPreProcessing(userQuery);

        VSM vsm = new VSM(indexPath);
        vsm.calculateIDFandTF(indexPath);
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
    }
}
