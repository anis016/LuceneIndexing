package com.lucene.main;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class App {

    static String getProjectPath(){

        String root = System.getProperty("user.dir");
        String projectName = "/LuceneTuts";

        return root + projectName;
    }

    public static void main(String... args) throws IOException {

        String indexPath = getProjectPath() + "/index/";
        String documentPath = getProjectPath() + "/data/file01.txt";

        Directory directory = FSDirectory.open(Paths.get(indexPath));
        boolean isExists = DirectoryReader.indexExists(directory);
        if (!isExists) {
            Indexing.indexDocuments(documentPath, indexPath);
        } else {
            System.out.println("Index directory already exists!");
        }

        String queryTerm = "Berlin";
        Searching.searchingDocuments(queryTerm, indexPath);
    }

}
