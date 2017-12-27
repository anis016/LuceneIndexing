package ir.prog2;

import java.util.Comparator;
import java.util.Map;

/**
 * Comparator class
 */
public class ValueComparator implements Comparator<String> {
    Map<String, Double> base;


    /**
     * Initializes a new ValueComparator instance.
     * @param base mapping of terms and value
     */
    public ValueComparator(Map<String, Double> base) {
        this.base = base;
    }

    /**
     * Imposes orderings that are inconsistent with equals.
     * @param a first term
     * @param b second term
     */
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}
