package ir.prog1;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class IndexFiles {

    IndexFiles() {

    }

    /**
     * Indexes the given file using the given writer, or if a directory is given,
     * recurses over files and directories found under the given directory.
     *
     * @param writer Writer to the index where the given file/dir info will be stored
     * @param path The file to index, or the directory to recurse into to find files to index
     * @throws IOException If there is a low-level I/O error
     */
    public static void indexDocs(final IndexWriter writer, Path path) throws IOException {

        if (Files.isDirectory(path)) {

            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    /**
     * Indexes a single document.
     *
     * @param writer Writer to the index where the given file/dir info will be stored
     * @param file The file to index
     * @throws IOException If there is a low-level I/O error
     */
    public static void indexDoc(IndexWriter writer, Path file, long lastModified) {
        try (InputStream stream = Files.newInputStream(file)) {

            Document doc = new Document();
            Field pathField = new StringField(LuceneConstants.FIELD_PATH, file.toString(), Field.Store.YES);
            doc.add(pathField);

            doc.add(new LongPoint("modified", lastModified));
            doc.add(new TextField(LuceneConstants.FIELD_CONTENTS,
                    new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

            if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                System.out.println("adding " + file);
                writer.addDocument(doc);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
