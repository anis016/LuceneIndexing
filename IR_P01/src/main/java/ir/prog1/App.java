package ir.prog1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Date;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    // java -jar IR P01.jar [path to document folder] [path to index folder] [VS/OK] [query]
    public static void main(String... args) throws IOException {

        if(args.length != 3) {
            LOGGER.error("Usage: java -jar IR_PO1.jar [path to document folder] [path to index folder]");
            throw new IllegalArgumentException("Incorrect number of arguments provided (2 expected, " + args.length
                    + " provided): " + Arrays.toString(args));
        }

        String docsPath  = Preconditions.checkNotNull(args[0], "Index Path should not be empty.");
        String indexPath = Preconditions.checkNotNull(args[1], "Index Path should not be empty.");
        String query     = Preconditions.checkNotNull(args[2], "Query should not be null");

        LOGGER.info("docsPath: ", docsPath);
        LOGGER.info("indexPath: ", indexPath);

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

        // Start the searching process
        SearchFiles searchFiles = new SearchFiles(indexPath);
        searchFiles.searchIndex(query);
    }
}
