package com.lucene.main;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class Searching {

    public static void searchingDocuments(String query, String indexPath) throws IOException {
        query = query.toLowerCase();
        System.out.println("Searching for: " + query);

        Directory directory = FSDirectory.open(Paths.get(indexPath));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(new BM25Similarity());

        int numDocs = indexReader.numDocs();
        // System.out.println("totalDocuments: " + numDocs);
        /*for (int docId = 0; docId < numDocs; docId++) {
            Terms termVector = indexReader.getTermVector(docId, "content");
            if (termVector == null) {
                System.out.println("No Term Vector Found");
                continue;
            }
            Document document = indexReader.document(docId);
            String documentContent = new String(document.get("content"));
            String documentID = new String(document.get("id"));

            System.out.println(documentID + " -- " + documentContent);
        }*/

        /**
         * A Term represents a word from text. This is the unit of search.
         * It is composed of two elements,
         * 1. the name of the field that the text occurred in, and
         * 2. the text of the word, as a string.
         */
        Term term = new Term("content", query);

        /**
         * TermQuery, is the Query that matches documents containing a term.
         */
        TermQuery termQuery = new TermQuery(term);

        int limit = Math.max(1, numDocs);
        TopDocs topDocs = indexSearcher.search(termQuery, limit);
        ScoreDoc[] hits = topDocs.scoreDocs;

        System.out.println("Total Hits: " + hits.length);
        for (ScoreDoc scoreDoc : hits) {
            int docId = scoreDoc.doc;
            Document document = indexSearcher.doc(docId);
            String print = String.format("id: %s, content: %s.", document.get("id"), document.get("content"));
            System.out.println(print);
        }
    }
}
