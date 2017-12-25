package ir.prog2;

import java.io.IOException;

public class App {

    public static void main(String... args) throws IOException {

        // String url = "https://www.hackerrank.com/";
        String url = "http://testing-ground.scraping.pro/";
        int depth = 1;
        String text1 = "http://www.Example.com";
        String text2 = "HTTP://www.EXAMPLE.com:8080/a%c2%b1b";
        String text3 = "http://www.Example.com:80/%7Eusername/";

        URLTraversal urlTraversal = new URLTraversal(depth, url);
        System.out.println(urlTraversal.urlNormalization(text1));
        System.out.println(urlTraversal.urlNormalization(text2));
        System.out.println(urlTraversal.urlNormalization(text3));

        // urlTraversal.startFetching();
    }
}
