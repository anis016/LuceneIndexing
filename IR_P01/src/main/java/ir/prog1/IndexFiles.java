package ir.prog1;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Indexes the documents
 */
public class IndexFiles {

    /**
     * Initializes a new IndexFiles instance.
     */
    IndexFiles() {

    }


    /**
     * Reads the file contents
     *
     * @param filePath Document path which needs to be read.
     * @throws IOException If there is a low-level I/O error
     */
    public static String readFileContent(String filePath) throws IOException{
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return content;
    }

    /**
     * Recurse over directories and find files under the given directory.
     *
     * @param folderPath Document folder which needs to be read.
     * @param filesList Contains the html files those needs to be indexed
     */
    public static void getRecursiveFilesInCurrentDir(String folderPath, ArrayList<File> filesList) {

        File root = new File(folderPath);
        File[] files = root.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                getRecursiveFilesInCurrentDir(file.toString(), filesList);
            } else {
                // Filter the files based on "html"
                if(file.getAbsolutePath().endsWith(LuceneConstants.FILE_EXTENSION))
                    filesList.add(file);
            }
        }
    }

    /**
     * Indexes all the files in the director.
     *
     * @param docsPath Document folder which needs to be read.
     * @param indexPath The index path where indexes will be kept.
     * @throws IOException If there is a low-level I/O error
     */
    public static void indexDocuments(String docsPath, String indexPath) throws IOException {

        ArrayList<File> fileList = new ArrayList<>();
        getRecursiveFilesInCurrentDir(docsPath, fileList);

        int counter = 0;
        List<Document> documentList = new ArrayList<>();
        for (File file: fileList) {

            String content = readFileContent(file.toString());
            String htmlTitle = HTMLParser.getTitle(content);
            String htmlBody = HTMLParser.getCleanedContents(content);

            String id    = String.valueOf(counter);
            String title = DocumentPreProcessing.dataPreProcessing(htmlTitle);
            String body  = DocumentPreProcessing.dataPreProcessing(htmlBody);
            String path  = file.toString();

            // Make it document
            Document document = createDocument(id, title, htmlTitle, body, path);
            // Add the document to the document Lists
            documentList.add(document);
            counter += 1;

            System.out.println("Adding File: " + path);
        }

        // Create IndexWriter
        IndexWriter writer = createWriter(indexPath);

        // first clean the directory
        writer.deleteAll();
        writer.addDocuments(documentList);

        writer.commit();
        writer.close();
    }

    /**
     * Create the Document for indexing.
     *
     * @param title Title of the document
     * @param body Body of the document
     * @param path Path of the document
     */
    private static Document createDocument(String id, String title, String htmlTitle, String body, String path) {

        FieldType fieldType = new FieldType(TextField.TYPE_STORED);
        fieldType.setStoreTermVectors(true);

        Document document = new Document();
        document.add(new StringField(LuceneConstants.FIELD_ID, id, Field.Store.YES));
        document.add(new StringField(LuceneConstants.FIELD_TITLE, title, Field.Store.YES));
        document.add(new StringField(LuceneConstants.FIELD_HTML_TITLE, htmlTitle, Field.Store.YES));
        document.add(new Field(LuceneConstants.FIELD_CONTENTS, body, fieldType));
        document.add(new StringField(LuceneConstants.FIELD_PATH, path, Field.Store.YES));

        return document;
    }

    /**
     * Create the Indexwriter for indexing.
     *
     * @param indexPath Index path where the given file/dir info will be stored
     * @throws IOException If there is a low-level I/O error
     */
    private static IndexWriter createWriter(String indexPath) throws IOException {

        // Get the path for the indexing
        FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        // Create a new index in the directory,
        // removing any previously indexed documents.
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        // if needed to add new documents to an existing index,
        // then do as follows
        // iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        // control the RAM buffer, if indexing many documents
        // iwc.setRAMBufferSizeMB(256.0);

        IndexWriter writer = new IndexWriter(dir, iwc);

        return writer;
    }
}
