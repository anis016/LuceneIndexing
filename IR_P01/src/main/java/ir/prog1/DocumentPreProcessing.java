package ir.prog1;

import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

/**
 * Pre-processed the Document for Indexing and Searching.
 */
public class DocumentPreProcessing {

    /**
     * Given a text it removes the stopwords given as StopAnalyzer.ENGLISH_STOP_WORDS_SET
     * Also, applies PorterStemmer to each word.
     * @param text text
     * @return converted text
     * @throws IOException if path value couldnot be read
     */
    public static String removeStopWordsAndStemming(String text) throws IOException {

        StringReader reader = new StringReader(text);
        Tokenizer whiteSpaceTokenizer = new WhitespaceTokenizer();
        whiteSpaceTokenizer.setReader(reader);
        TokenStream tokenStream = new StopFilter(whiteSpaceTokenizer, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        tokenStream = new PorterStemFilter(tokenStream);

        final CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        StringBuilder buildContent = new StringBuilder();
        while (tokenStream.incrementToken()) {
            buildContent.append(charTermAttribute.toString() + " ");
        }
        tokenStream.end();
        tokenStream.close();

        return buildContent.toString();
    }

    /**
     * Remove all the non-words from the sentence(list of strings)
     * @param text list of words
     * @return string of sentence
     */
    public static String filterNonWords(List<String> text) {
        StringBuilder builder = new StringBuilder();
        for (String word : text) {
            if(word.length() <= 1) {
                continue;
            } else if (isWholeNumber(word)) {
                continue;
            } else {
                builder.append(word.toLowerCase() + " ");
            }
        }

        return builder.toString();
    }

    /**
     * Checks if given word is a whole number
     * @param word string
     * @return boolean value of true/false
     */
    public static boolean isWholeNumber(String word) {
        int firstLen = word.length();
        int counter = 0;
        for (int i = 0; i < firstLen; i++) {
            int ordinalNumber = (int) word.charAt(i);
            // match the ascii of number 0~9 => 48~57
            if ( (48 <= ordinalNumber) && (ordinalNumber <= 57) ) {
                counter ++;
            }
        }

        return (firstLen == counter);
    }

    /**
     * Handles the Data Pre-processing such as
     * 1. converting the paragraph into sentences
     * 2. filter non-words
     * 3. remove stopwords and stemming each word using PorterStemmer
     * @param text paragraph extracted
     * @return pre-processed data
     * @throws IOException if path value couldnot be read
     */
    public static String dataPreProcessing(String text) throws IOException {

        // Divide the Contents into several pieces
        List<String> sentences = Arrays.asList(text.split("[.!?,;:\\t\\\\\\\\\"\\\\(\\\\)\\\\\\'\\u2019\\u2013]|\\\\s\\\\-\\\\s"));
        StringBuilder builder = new StringBuilder();
        for (String sentence : sentences) {
            List<String> words = Arrays.asList(sentence.split("[^a-zA-Z0-9_\\\\+\\\\-\\\\]"));
            builder.append(DocumentPreProcessing.filterNonWords(words) + " ");
        }
        String body = builder.toString();
        // System.out.println("After Pre-processing: " + body);

        String processedBody = DocumentPreProcessing.removeStopWordsAndStemming(body);
        // System.out.println("Removed stop words and stemmed: " + processedBody);

        return processedBody;
    }

    /**
     * Example Driver Program
     * @param args arguments
     * @throws IOException if path value couldnot be read
     **/
    public static void main(String... args) throws IOException {
        String text = "AWESOME";
        String textout = DocumentPreProcessing.dataPreProcessing(text);
        System.out.println(textout);
    }

}
