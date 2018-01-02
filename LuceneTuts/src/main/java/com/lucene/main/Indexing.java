package com.lucene.main;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Indexing {

    private static List<String> getDocumentsContents(String docsPath) throws IOException {

        String content = new String(Files.readAllBytes(Paths.get(docsPath)));
        List<String> contentsList = Arrays.asList(content.split("\\."));
        return contentsList;
    }

    public static void indexDocuments(String docsPath, String indexPath) throws IOException {
        IndexWriter indexWriter = createIndexWriter(indexPath);

        List<String> contents = getDocumentsContents(docsPath);
        int id = 1;
        List<Document> documentList = new ArrayList<>();
        for (String content : contents) {
            String docId = String.valueOf(id);
            Document document = createDocument(docId, content.toLowerCase());
            documentList.add(document);
            id++;
        }

        indexWriter.addDocuments(documentList);
        indexWriter.commit();
        indexWriter.close();

        System.out.println("Indexing Completed!");
    }

    private static Document createDocument(String id, String content) {
        FieldType fieldType = new FieldType(TextField.TYPE_STORED);
        fieldType.setStoreTermVectors(true);

        Document document = new Document();
        document.add(new StringField("id", id, Field.Store.YES));
        document.add(new Field("content", content, fieldType));

        return document;
    }

    private static IndexWriter createIndexWriter(String index) throws IOException {

        FSDirectory directory = FSDirectory.open(Paths.get(index));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);

        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        indexWriterConfig.setSimilarity(new BM25Similarity());
        IndexWriter writer = new IndexWriter(directory, indexWriterConfig);


        return writer;
    }
}
