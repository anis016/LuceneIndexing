package ir.prog1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DocumentReader {

    private List<String> fileLists = null;

    public DocumentReader() {
        this.fileLists = new ArrayList<>();
    }

    public List<String> getFileLists() {
        return fileLists;
    }

    /**
     * This function recursively reads the path and lists all the files
     * in a directory
     * @param path: path of the directory
     */
    public void recursiveFolderReader(String path) {
        Preconditions.checkArgument(path, "Path cannot be empty");

        File root = new File(path);
        File[] lists = root.listFiles();

        if (lists == null)
            return;

        for (File file : lists) {
            if (file.isDirectory()) {
                recursiveFolderReader(file.getAbsolutePath());
                // System.out.println("Dir: " + file.getAbsolutePath());
            } else {
                this.fileLists.add(file.getAbsolutePath());
                // System.out.println("File: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * This function reads the content in the path
     * @param path: path of the file
     * @return
     */
    public String fileReader(String path) {
        try {
            byte[] file = Files.readAllBytes(Paths.get(path));
            String content = new String(file);

            return content;
        } catch (IOException e) {
            System.out.println("File Content could not be read!");
            e.printStackTrace();
        }

        return null;
    }
}

/**
 * Usage:
 * // Read the docsPath and collect all the fileNames
 * DocumentReader reader = new DocumentReader();
 * reader.recursiveFolderReader(docsPath);
 * List<String> fileLists = reader.getFileLists();
 */
