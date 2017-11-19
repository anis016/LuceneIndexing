package ir.prog1;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

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

            // Get the path for the indexing
            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            // Create a new index in the directory,
            // removing any previously indexed documens.
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            // if needed to add new documents to an existing index,
            // then do as follows
            // iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            // control the RAM buffer, if indexing many documents
            // iwc.setRAMBufferSizeMB(256.0);

            IndexWriter writer = new IndexWriter(dir, iwc);
            IndexFiles.indexDocs(writer, docDir);

            writer.close();
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
