package ir.prog2;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Path;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

public class App {

    public static void main(String... args) throws IOException {

        if(args.length != 3) {
            // LOGGER.error("Usage: java -jar IR_PO1.jar [path to document folder] [path to index folder] [VS/OK] [query]");
            System.out.println("java -jar IR P02.jar [seed URL] [crawl depth] [path to index folder] [query]");
            throw new IllegalArgumentException("Incorrect number of arguments provided (4 expected, " + args.length
                    + " provided): " + Arrays.toString(args));
        }

        // check for empty/null and fetch parameter
        String seedURL    = Preconditions.checkNotNull(args[0], "Seed URL should not be null.");
        String crawlDepth = Preconditions.checkNotNull(args[1], "Crawl depth should not be null.");
        String indexPath  = Preconditions.checkNotNull(args[2], "Index path should not be null");
        // String userQuery  = Preconditions.checkNotNull(args[3], "Query should not be null");

        // int depth = Integer.parseInt(crawlDepth);
        int depth = 2;

        // Check if user wants re-create the index or use the existing indexing folder.
        boolean flag;
        final Path indexDir = Paths.get(indexPath);
        if (indexDir.toFile().exists()) {

            System.out.println("Index folder Path already exists !");
            Directory directory = FSDirectory.open(Paths.get(indexPath));
            if(DirectoryReader.indexExists(directory) == true) {
                System.out.println("Found indexing files. Reading from the IndexDirectory.");
            }
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

        if (flag) {

            // Start the indexing process
            Date start = new Date();
            try {
                System.out.println("Indexing to the directory '" + indexPath + "'..." + "\n");

                // Start the fetching of documents.
                URLIndexer urlIndexer = new URLIndexer(depth, seedURL);
                urlIndexer.startFetchingAndIndexing(indexPath);

                Date end = new Date();
                System.out.println("Took " + String.valueOf(end.getTime() - start.getTime()) + " total milliseconds for indexing.");

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("\n");
        }
    }
}
