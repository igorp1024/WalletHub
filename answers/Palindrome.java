import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Palindrome string test implementation.
 *
 * NOTE: this class requires JUnit library (http://junit.org). Tested on version 4.12.
 */
public class Palindrome {

    private static final int MIN_CHUNK_SIZE = 3;
    private static final int cores = Runtime.getRuntime().availableProcessors();

    /**
     * Checks the given string for being a palindrome. Splits it into imaginary chunk pairs and
     * compares them in parallel. Space efficiency is O(1), algorithm complexity is leaning to O(n),
     * where n is a string length (actually it is O(n/cpu_cores) in general case, but this is still
     * O(n) anyway).
     *
     * @param str a string to check.
     * @return true if the string is a palindrome, false otherwise.
     */
    public static boolean isAPalindrome(String str) {

        if (str == null || str.length() == 0) {
            return false;
        }
        if (str.length() == 1) {
            return true;
        }
        // A bit of paranoid optimisation, maybe even too paranoid but nevertheless
        if (str.length() == 2 && str.charAt(0) == str.charAt(1)) {
            return true;
        }

        int length = str.length(),

            // Comparison is being performed via pairs of chunks the source string is being
            // imaginary split in. For instance, for the string "abcdefgfedcba" the following pairs
            // of chunks are selected: (0¹)"abc", (1¹)"def", "g", (1²)"fed", (0²)"cba".
            // So, 0¹ is compared in reverse with 0² (which is pair #0)
            // and 1¹ is compared in reverse with 1² (which is pair #1).
            // In this case there will be two comparison rounds (two pairs of chunks).

            // We want pairs to be either at least of MIN_CHUNK_SIZE length (for small palindrome
            // strings) or as much as CPU cores for most efficient comparison (for large strings).
            comparisonRounds = Math.min(cores, Math.max(length / (MIN_CHUNK_SIZE * 2), 1)),
            preliminaryChunkLength = length / comparisonRounds / 2;

        int unmatchedCharsLeft = length - comparisonRounds * 2 * preliminaryChunkLength;
        // If there's just one char left which is not going to be matched with any other char,
        // this means that it is "symmetry axis" char which has no pair and is placed right in the
        // middle of the palindrome. No point in matching it.
        if (unmatchedCharsLeft > 1) {
            // Otherwise, remaining chars amount is less than chunk length, so we need to increase
            // the chunk size. This will make the middle pair of chunks overlap each other, but some
            // overhead preventing measures are taken during the comparison.
            preliminaryChunkLength++;
            // Updated chunk length may require updating comparison rounds amount. The formula
            // is simple:
            //      rounds = length / (chunk_length * 2);
            // The result should be an integer value (comparison rounds). If there's a remainder,
            // the result value is incremented (Math.ceil())
            comparisonRounds =
                (int) // Quite unlikely to have CPU cores more than java.lang.Integer.MAX_VALUE
                    Math.round(Math.ceil(Double.valueOf(length) / preliminaryChunkLength / 2));
        }

        // This value should be effective final or just final to use it in the lambdas
        final int chunkLength = preliminaryChunkLength;

        long failedCount = IntStream.range(0, comparisonRounds)
            .parallel() // Using common ForkJoinPool, in real use case it may be necessary
            // to create a new one and submit this task there to avoid possible locks.
            .mapToObj(chunkPairNo -> {
                for (int i = 0; i < chunkLength; i++) {

                    int leftPos = chunkPairNo * chunkLength + i;
                    int rightPos = length - chunkPairNo * chunkLength - 1 - i;

                    // Last pair of chunks may overlap, so no sense in comparing overlapping parts
                    // again (this happens when both pointers point to the same char or both of
                    // them had passed through the middle of the palindrome).
                    if (leftPos >= rightPos) {
                        break;
                    }
                    // If necessary chars don't match, it is not a palindrome
                    if (str.charAt(leftPos) != str.charAt(rightPos)) {
                        return false;
                    }
                }
                return true;
            })
            .filter(item -> !item)
            .count();

        return failedCount == 0;
    }

    public static void main(String[] args) {

        assertFalse(checkAndPrint((null)));
        assertFalse(checkAndPrint(("")));

        assertFalse(checkAndPrint(("paiposoborkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkrobosopiap")));
        assertFalse(checkAndPrint(("aiposoborkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkrobosopia")));
        assertFalse(checkAndPrint(("iposoborkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkrobosopi")));
        assertFalse(checkAndPrint(("posoborkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkrobosop")));
        assertFalse(checkAndPrint(("osoborkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkroboso")));
        assertFalse(checkAndPrint(("soborkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkrobos")));
        assertFalse(checkAndPrint(("oborkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkrobo")));
        assertFalse(checkAndPrint(("borkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkrob")));
        assertFalse(checkAndPrint(("orkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkro")));
        assertFalse(checkAndPrint(("rkmcfhnvkhitnifcoirdd█riocfintihkvnhfcmkr")));
        assertFalse(checkAndPrint(("kmcfhnvkhitnifcoirdd█riocfintihkvnhfcmk")));
        assertFalse(checkAndPrint(("mcfhnvkhitnifcoirdd█riocfintihkvnhfcm")));
        assertFalse(checkAndPrint(("cfhnvkhitnifcoirdd█riocfintihkvnhfc")));
        assertFalse(checkAndPrint(("fhnvkhitnifcoirdd█riocfintihkvnhf")));
        assertFalse(checkAndPrint(("hnvkhitnifcoirdd█riocfintihkvnh")));
        assertFalse(checkAndPrint(("nvkhitnifcoirdd█riocfintihkvn")));
        assertFalse(checkAndPrint(("vkhitnifcoirdd█riocfintihkv")));
        assertFalse(checkAndPrint(("khitnifcoirdd█riocfintihk")));
        assertFalse(checkAndPrint(("hitnifcoirdd█riocfintih")));
        assertFalse(checkAndPrint(("itnifcoirdd█riocfinti")));
        assertFalse(checkAndPrint(("tnifcoirdd█riocfint")));
        assertFalse(checkAndPrint(("nifcoirdd█riocfin")));
        assertFalse(checkAndPrint(("ifcoirdd█riocfi")));
        assertFalse(checkAndPrint(("fcoirdd█riocf")));
        assertFalse(checkAndPrint(("coirdd█rioc")));
        assertFalse(checkAndPrint(("oirdd█rio")));
        assertFalse(checkAndPrint(("irdd█ri")));
        assertFalse(checkAndPrint(("rdd█r")));
        assertFalse(checkAndPrint(("dd█")));
        assertFalse(checkAndPrint(("d█")));

        assertTrue(checkAndPrint(("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")));
        assertTrue(checkAndPrint(("paiposoborkmcfhnvkhitnifcoirddriocfintihkvnhfcmkrobosopiap")));
        assertTrue(checkAndPrint(("aiposoborkmcfhnvkhitnifcoirddriocfintihkvnhfcmkrobosopia")));
        assertTrue(checkAndPrint(("iposoborkmcfhnvkhitnifcoirddriocfintihkvnhfcmkrobosopi")));
        assertTrue(checkAndPrint(("posoborkmcfhnvkhitnifcoirddriocfintihkvnhfcmkrobosop")));
        assertTrue(checkAndPrint(("osoborkmcfhnvkhitnifcoirddriocfintihkvnhfcmkroboso")));
        assertTrue(checkAndPrint(("soborkmcfhnvkhitnifcoirddriocfintihkvnhfcmkrobos")));
        assertTrue(checkAndPrint(("oborkmcfhnvkhitnifcoirddriocfintihkvnhfcmkrobo")));
        assertTrue(checkAndPrint(("borkmcfhnvkhitnifcoirddriocfintihkvnhfcmkrob")));
        assertTrue(checkAndPrint(("orkmcfhnvkhitnifcoirddriocfintihkvnhfcmkro")));
        assertTrue(checkAndPrint(("rkmcfhnvkhitnifcoirddriocfintihkvnhfcmkr")));
        assertTrue(checkAndPrint(("kmcfhnvkhitnifcoirddriocfintihkvnhfcmk")));
        assertTrue(checkAndPrint(("mcfhnvkhitnifcoirddriocfintihkvnhfcm")));
        assertTrue(checkAndPrint(("cfhnvkhitnifcoirddriocfintihkvnhfc")));
        assertTrue(checkAndPrint(("fhnvkhitnifcoirddriocfintihkvnhf")));
        assertTrue(checkAndPrint(("hnvkhitnifcoirddriocfintihkvnh")));
        assertTrue(checkAndPrint(("nvkhitnifcoirddriocfintihkvn")));
        assertTrue(checkAndPrint(("vkhitnifcoirddriocfintihkv")));
        assertTrue(checkAndPrint(("khitnifcoirddriocfintihk")));
        assertTrue(checkAndPrint(("hitnifcoirddriocfintih")));
        assertTrue(checkAndPrint(("itnifcoirddriocfinti")));
        assertTrue(checkAndPrint(("tnifcoirddriocfint")));
        assertTrue(checkAndPrint(("nifcoirddriocfin")));
        assertTrue(checkAndPrint(("ifcoirddriocfi")));
        assertTrue(checkAndPrint(("fcoirddriocf")));
        assertTrue(checkAndPrint(("coirddrioc")));
        assertTrue(checkAndPrint(("oirddrio")));
        assertTrue(checkAndPrint(("irddri")));
        assertTrue(checkAndPrint(("rddr")));
        assertTrue(checkAndPrint(("dd")));
        assertTrue(checkAndPrint(("d")));

        for (int i = 1; i < 33; i++) {
            assertTrue(checkAndPrint(generatePalindrome(i)));
        }

        for (int i = 1; i < 33; i++) {
            assertFalse(
                checkAndPrint(
                    spoilString(generatePalindrome(i))));
        }

        // Some tests to check the randomly generated palindromes (without output)
        String palindrome;
        System.out.println("Generating palindromes from 2 to 1000 chars");
        for (int i = 0; i < 100; i++) {
            for (int j = 1; j < 1000 / 2; j++) {
                palindrome = generatePalindrome(j);
                assertTrue(isAPalindrome(palindrome));
            }
        }
        System.out.println("Generating spoiled palindromes from 2 to 1000 chars");
        for (int i = 0; i < 100; i++) {
            for (int j = 1; j < 1000 / 2; j++) {
                palindrome = spoilString(generatePalindrome(j));
                assertFalse(isAPalindrome(palindrome));
            }
        }

        System.out.println("All OK");
    }

    /* Tests utility methods. */

    /**
     * Syntax sugar for invoking the check algorithm and dumping the result on the screen.
     */
    private static boolean checkAndPrint(String str) {
        boolean aPalindrome = isAPalindrome(str);
        System.out.println(
            String.format(
                "%s palindrome: %s",
                (aPalindrome ? "a true" : "NOT a"),
                str
            )
        );
        return aPalindrome;
    }

    /**
     * Syntax sugar methods for generating palindromes consumable by test code.
     */

    private static final Random RANDOM = new Random(System.nanoTime());

    private static String generatePalindrome(int halfOfLength) {
        return generatePalindrome(halfOfLength, RANDOM.nextBoolean());
    }

    private static String generatePalindrome(int halfOfLength, boolean evenly) {

        final char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder result = new StringBuilder(halfOfLength);

        for (int i = 0; i < halfOfLength; i++) {
            result.append(alphabet[RANDOM.nextInt(alphabet.length)]);
        }

        return mirrorString(result.toString(), evenly);
    }

    private static String mirrorString(String result, boolean evenly) {
        return (evenly)
               ? mirrorStringEvenly(result)
               : mirrorStringUnevenly(result);
    }

    private static String mirrorStringEvenly(String str) {
        StringBuilder
            sb = new StringBuilder(str),
            sbr = new StringBuilder(str).reverse();
        return sb
            .append(sbr)
            .toString();
    }

    private static String mirrorStringUnevenly(String str) {
        int length = str.length();
        StringBuilder
            sb = new StringBuilder(str.substring(0, length - 1)),
            sbr = new StringBuilder(str.substring(0, length - 1)).reverse();
        return sb
            .append(str.substring(length - 1))
            .append(sbr)
            .toString();
    }

    private static String spoilString(String str) {
        int idx = RANDOM.nextInt(str.length());
        // Avoid making uneven palindrome from even one by putting the spoiling char
        // right in the middle
        if (str.length() % 2 == 0 && str.length() / 2 == idx) {
            idx++;
        }
        return new StringBuilder()
            .append(str.substring(0, idx))
            .append('█')
            .append(str.substring(idx))
            .toString();
    }
}