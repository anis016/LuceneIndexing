package ir.prog1;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;


public class VSM implements IDocumentSimilarity {
    String indexPath = "";

    public VSM(String indexPath) {
        this.indexPath = indexPath;
    }

    @Override
    public String documentSimilarity(String indexPath) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexPath));
        IndexReader indexReader = DirectoryReader.open(directory);

        for (int i = 0; i < indexReader.numDocs(); i++) {

            Document document = indexReader.document(i);
            IndexableField[] contents = document.getFields("contents");

            for (IndexableField field : contents) {
                String result = field.stringValue();



                System.out.println(result);
            }
        }

        return null;
    }

    public static void main(String... args) throws IOException {

        VSM vsm = new VSM("/home/anis/index");
        vsm.documentSimilarity(vsm.indexPath);

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
