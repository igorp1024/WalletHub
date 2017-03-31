import org.junit.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * K-complementary pairs find implementation.
 *
 * NOTE: this class requires JUnit library (http://junit.org). Tested on version 4.12.
 */
public class ComplementaryPairs {

    /**
     * Finds K-complementary pairs in the given array (pair (i, j) is K- complementary if K = A[i] +
     * A[j]). Builds the map of values (which references to indexes list) and after that finds
     * complementary values (looks for K-value_from_key) and builds list of pairs. Memory footprint
     * is about O(n), where n is amount of items in the array. Code efficiency is O(n). More
     * detailed: 1. Best case (no pairs, same single value in all array elements) is O(n). 2. Worst
     * case (n/2 pairs, unique values in the array) is O(n + n/2), so O(n).
     */
    public static List<Pair> findPairs(long k, int[] array) {

        // No pairs in null, zero-sized or arrays with 1 element
        if (array == null || array.length < 2) {
            return Collections.emptyList();
        }

        Map<Integer, List<Integer>> aggregated = new HashMap<>(array.length);

        // Building map of the values and storing their indexes in list (map entry value)
        for (int i = 0; i < array.length; i++) {
            int key = array[i];
            if (!aggregated.containsKey(key)) {
                aggregated.put(key, new LinkedList<>());
            }
            aggregated.get(key).add(i);
        }

        // Calculation of the values
        List<Pair> pairs = new LinkedList<>();
        for (Iterator<Integer> it = aggregated.keySet().iterator(); it.hasNext(); /**/) {

            int value = it.next();

            long complementaryValueAsLong = k - value;
            int complementaryValue = (int) complementaryValueAsLong;

            // If precision loss happens, this means that corresponding K-complementary for this
            // value is long which is not possible in array of integers, skipping the check
            // (fail fast optimization)
            boolean precisionLoss = (complementaryValueAsLong != complementaryValue);
            if (precisionLoss) {
                continue;
            }

            // If there's a K-complementary item in aggregated map, process it
            if (aggregated.containsKey(complementaryValue)) {

                List<Integer> valueIndexes = aggregated.get(value);
                List<Integer> complementaryValueIndexes = aggregated.get(complementaryValue);

                if (valueIndexes == complementaryValueIndexes) {
                    // The K value is even and there's a K/2 value in map found. If there's more
                    // than one index in corresponding set, these indexes produce pairs.
                    // Otherwise, single index produces no pair.
                    if (valueIndexes.size() > 1) {

                        // Produce pairs from single list of indexes
                        int i = 1;
                        List<Integer> surrogateComplementaryValueIndexes;
                        for (Integer nextFirst : valueIndexes) {
                            surrogateComplementaryValueIndexes =
                                valueIndexes.subList(i++, valueIndexes.size());
                            for (Integer nextSecond : surrogateComplementaryValueIndexes) {
                                pairs.add(new Pair(nextFirst, nextSecond));
                            }
                        }
                    }

                } else {

                    // Produce pairs from two lists of indexes
                    for (Integer nextFirst : valueIndexes) {
                        for (Integer nextSecond : complementaryValueIndexes) {
                            pairs.add(new Pair(nextFirst, nextSecond));
                        }
                    }

                    // One of the next iterations once the K-complementary key is reached it won't
                    // be treated as a K-complementary second part anymore, since it's first
                    // part is removed from the map
                    it.remove();
                }
            }
        }

        return pairs;
    }

    /**
     * A container for the values pair (keeps just indexes of that pair).
     */
    private static class Pair {

        private int firstIdx = -1;
        private int secondIdx = -1;

        public Pair(int firstIdx, int secondIdx) {
            this.firstIdx = firstIdx;
            this.secondIdx = secondIdx;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Pair pair = (Pair) o;

            if (firstIdx != pair.firstIdx) {
                return false;
            }
            return secondIdx == pair.secondIdx;
        }

        @Override
        public int hashCode() {
            int result = firstIdx;
            result = 31 * result + secondIdx;
            return result;
        }
    }

    public static void main(String[] args) {

        Assert.assertEquals(
            s(),
            findAndPrint(12345, null));

        Assert.assertEquals(
            s(),
            findAndPrint(12345, new int[]{}));

        Assert.assertEquals(
            s(),
            findAndPrint(10, new int[]{10}));

        Assert.assertEquals(
            s(),
            findAndPrint(10, new int[]{6, 5}));

        Assert.assertEquals(
            s(p(0, 1)),
            findAndPrint(10, new int[]{5, 5}));

        Assert.assertEquals(
            s(p(0, 8),  //
              p(1, 7),  //
              p(2, 6),  //
              p(3, 5)), //
            findAndPrint(10, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0}));

        Assert.assertEquals(
            s(p(0, 10), //
              p(1, 9),  //
              p(2, 8),  //
              p(3, 4), p(3, 5), p(3, 6), p(3, 7), p(4, 5), p(4, 6), p(4, 7), p(5, 6), p(5, 7),
              p(6, 7),  //
              p(13, 11)),
            findAndPrint(8, new int[]{1, 2, 3, 4, 4, 4, 4, 4, 5, 6, 7, 8, 9, 0}));

        Assert.assertEquals(
            s(p(0, 1), p(0, 3), p(0, 5),    //
              p(2, 1), p(2, 3), p(2, 5),    //
              p(4, 1), p(4, 3), p(4, 5),    //
              p(6, 1), p(6, 3), p(6, 5)),
            findAndPrint(7, new int[]{3, 4, 3, 4, 3, 4, 3}));

        Assert.assertEquals(
            s(p(0, 1), p(0, 3), p(0, 5),    //
              p(2, 1), p(2, 3), p(2, 5),    //
              p(6, 1), p(6, 3), p(6, 5)),
            findAndPrint(1, new int[]{-3, 4, -3, 4, 3, 4, -3}));

        Assert.assertEquals(
            s(p(0, 1), p(0, 2), p(1, 2)), //
            findAndPrint(0, new int[]{0, 0, 0}));

        Assert.assertEquals(
            s(), //
            findAndPrint(Integer.MAX_VALUE * 100, new int[]{0, 0, 0}));

        Assert.assertEquals(
            s(p(0, 1)), //
            findAndPrint(-1, new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}));

        Assert.assertEquals(
            s(p(0, 2), p(0, 3), p(0, 4), p(2, 3), p(2, 4), p(3, 4)), //
            findAndPrint(Integer.MAX_VALUE * 2L,
                         new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
                                   Integer.MAX_VALUE, Integer.MAX_VALUE}));

        Assert.assertEquals(
            s(p(1, 2)), //
            findAndPrint(Integer.MIN_VALUE * 2L,
                         new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE}));
    }

    /* Tests utility methods. */

    /**
     * Prints pair value for the given array.
     *
     * @param array array to fins the values in.
     */
    private static void printPair(Pair pair, int[] array) {
        int length = array.length;
        // Ideally this code should invoke getters, but since it is private class and for the code
        // brevity sake it just references the class properties directly.
        if (pair.firstIdx >= length || pair.secondIdx >= length) {
            throw new RuntimeException(
                String.format(
                    "Bad array (%d elements length) for the given pair of indexes: %d,%d ",
                    length, pair.firstIdx, pair.secondIdx
                )
            );
        }
        System.out.println(
            String.format(
                "[%d][%d] = %d, %d",
                pair.firstIdx, pair.secondIdx, array[pair.firstIdx], array[pair.secondIdx]
            )
        );
    }

    /**
     * Syntax sugar for invoking the find algorithm and dumping the result on the screen.
     */
    private static Set<Pair> findAndPrint(long k, int[] array) {

        System.out.println(
            String.format(
                "\n(%d)-complementary pairs in %s are:",
                k,
                Arrays.toString(array)
            )
        );

        List<Pair> pairs = findPairs(k, array);
        pairs.forEach(pair -> printPair(pair, array));

        Set<Pair> pairsSet = new HashSet<>(pairs.size());
        pairsSet.addAll(pairs);

        // Make sure no issues has happened during the conversion
        Assert.assertEquals(pairs.size(), pairsSet.size());

        return pairsSet;
    }

    /**
     * Syntax sugar for creating {@link Pair} class. Just for test code brevity purposes.
     */
    private static Pair p(int firstIdx, int secondIdx) {
        return new Pair(firstIdx, secondIdx);
    }

    /**
     * Syntax sugar for creating {@link Set<Pair>} class. Just for test code brevity purposes.
     */
    private static Set<Pair> s(Pair... pairs) {
        Set<Pair> pairsSet = new HashSet<>(pairs.length);
        for (Pair nextPair : pairs) {
            pairsSet.add(nextPair);
        }
        return pairsSet;
    }
}