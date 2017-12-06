package ir.prog1;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Searches the given query from the indexed documents.
 */
public class SearchFiles {

    IndexReader indexReader = null;
    IndexSearcher indexSearcher = null;
    Analyzer analyzer = null;
    QueryParser queryParser = null;

    /**
     * Initializes a new SearchFiles instance.
     * @param index path
     */
    public SearchFiles(String index) throws IOException {
        this.indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        this.indexSearcher = new IndexSearcher(this.indexReader);
        this.analyzer = new StandardAnalyzer();
        this.queryParser = new QueryParser(LuceneConstants.FIELD_CONTENTS, analyzer);
    }

    /**
     * Given a query it searches in the indexed path for match
     * @param searchString
     */
    public void searchIndex(String searchString) throws IOException {
        System.out.println("Searching for : " + searchString);
        ScoreDoc[] hits;

        try {
            String stemmedQuery = DocumentPreProcessing.dataPreProcessing(searchString);
            Query query = this.queryParser.parse(stemmedQuery);

            TopDocs results = indexSearcher.search(query, LuceneConstants.MAX_SEARH);
            hits = results.scoreDocs;

            int numTotalHits = Math.toIntExact(results.totalHits);
            System.out.println(numTotalHits + " total matching documents");
            System.out.println("Showing all the paths!");

            for (ScoreDoc scoreDoc : hits) {
                Document document = indexSearcher.doc(scoreDoc.doc);
                String path = document.get(LuceneConstants.FIELD_PATH);
                System.out.println("Hit: " + path);
            }


        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

}
