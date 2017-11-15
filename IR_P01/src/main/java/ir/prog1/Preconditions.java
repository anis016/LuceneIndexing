package ir.prog1;

public final class Preconditions {

    private Preconditions() {

    }

    /**
     * Checks if the given string is null or empty
     * @param reference: the reference to check for null
     * @param errorMessage: the error message to provide if string is null or empty
     * @return the reference if it is null or not
     */
    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if(reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }

        return reference;
    }

    /**
     * Checks if the given string is null or empty
     * @param string: the string to check for null or empty
     * @param errorMessage: the error message to provide if string is null or empty
     * @return the string if it is null or empty
     */
    public static String checkArgument(String string, Object errorMessage) {
        if (string == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }

        if (string.isEmpty()) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }

        return string;
    }
}
