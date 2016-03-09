package net.coderodde.util.sorting;

import java.util.Arrays;
import java.util.Comparator;

/**
 * This class provides static methods for sorting object arrays using 
 * bottom-up (non-recursive) merge sort.
 * <p>
 * Initially, the input range is divided into chunks of 
 * {@code insertionsortThreshold} elements and are sorted using insertion sort.
 * (The last chunk is allowed to be shorter.) After that they are merged 
 * pairwise until the input range is sorted.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6
 * @param <T> the actual array component type.
 */
public class BottomUpMergesort<T extends Comparable<? super T>> {

    private static final int INSERTIONSORT_THRESHOLD = 10;

    /**
     * The actual array containing the range to be sorted.
     */
    private final T[] array;

    /**
     * The helper array for merging.
     */
    private final T[] buffer;

    /**
     * The starting (inclusive) index of the range to be sorted.
     */
    private final int fromIdx;

    /**
     * The length of the range being sorted.
     */
    private final int rangeLength;

    /**
     * The array holding current source array.
     */
    private T[] source;

    /**
     * The array holding current target array.
     */
    private T[] target;

    /**
     * The amount of array components at the beginning to skip from 
     * consideration in the source array.
     */
    private int sourceOffset;

    /**
     * The amount of array components at the beginning to skip from 
     * consideration in the target array. 
     */
    private int targetOffset;

    /**
     * Constructs a new sort object holding the state of sorting procedure.
     * 
     * @param array     the array containing the requested sort range.
     * @param fromIndex the starting (inclusive) index of the range to sort.
     * @param toIndex   the ending (exclusive) index of the range to sort.
     * @param cmp       the array component comparator.
     */
    private BottomUpMergesort(T[] array, 
                              int fromIndex, 
                              int toIndex) {
        if (array == null) {
            throw new NullPointerException("The input array is null.");
        }
        
        rangeCheck(array.length, fromIndex, toIndex);
        this.fromIdx = fromIndex;
        this.array = array;
        this.rangeLength = toIndex - fromIndex;
        this.buffer = Arrays.copyOfRange(array, fromIndex, toIndex);
    }

    /**
     * Performs the actual sorting.
     */
    private void sort() {
        if (rangeLength < 2) {
            return;
        }

        int runs = computeRunAmount();
        int mergePasses = computeAmountOfMergingPasses(runs);

        if (mergePasses % 2 == 0) {
            // We will need an even amount of merging passes over the input 
            // range in order to sort it. Let the input array be source so that 
            // the sorted range ends up in it.
            this.source = array;
            this.target = buffer;
            this.sourceOffset = fromIdx;
            this.targetOffset = 0;
        } else {
            // We need an odd number of merging passes over the input range in
            // order to sort it. Let the auxiliary buffer be the source so that
            // the sorted range ends up in the input array.
            this.source = buffer;
            this.target = array;
            this.sourceOffset = 0;
            this.targetOffset = fromIdx;
        }

        // Make the requested range be sorted into sorted chunks, each of length
        // 'insertionsortThreshold'. The last chunk may be shorter than that
        // threshold value.
        presortRuns(runs);

        // Initial runs are ready to be merged. 'runLength <<= 1' multiplies
        // 'runLength' by 2.
        for (int runLength = INSERTIONSORT_THRESHOLD; 
                 runLength < rangeLength;
                 runLength <<= 1) {
            mergePass(runs, runLength);
            // 'runs >>> 1' divides 'runs' by 2 ignoring the decimals.
            // '(runs & 1) != 0 ? 1 : 0' is zero if 'runs' is even, and one
            // otherwise.
            runs = (runs >>> 1) + ((runs & 1) != 0 ? 1 : 0);
            // Now make the target array a source array, and vice versa.
            swapArrayRoles();
        }
    }

    /**
     * Makes the source array a target array, and the target array a source 
     * array. Adjusts also the offsets of the two arrays.
     */
    private void swapArrayRoles() {
        // Swap the array roles.
        T[] tmparr = source;
        source = target;
        target = tmparr;
        
        // Swap the array offsets.
        int tmpOffset = sourceOffset;
        sourceOffset = targetOffset;
        targetOffset = tmpOffset;
    }

    /**
     * Computes the amount of runs in the requested range that are to be sorted
     * using insertion sort.
     * 
     * @return the amount of runs.
     */
    private int computeRunAmount() {
        return rangeLength / INSERTIONSORT_THRESHOLD +
              (rangeLength % INSERTIONSORT_THRESHOLD != 0 ? 1 : 0);
    }

    /**
     * Computes the amount of merging passes needed to be performed in order to
     * sort the requested range.
     * 
     * @param  runs the amount of runs in the requested input range after
     *         insertion sort was applied to small chunks.
     * @return the amount of merging passes needed to sort the input range.
     */
    private int computeAmountOfMergingPasses(int runs) {
        return 32 - Integer.numberOfLeadingZeros(runs - 1);
    }

    /**
     * Presorts the input range so that it contains sorted chunks of length
     * {@code insertionsortThreshold}. The last run may be shorter.
     * 
     * @param runs the amount of runs the requested range contains of.
     */
    private void presortRuns(int runs) {
        int localFromIndex = sourceOffset;

        // Presort all but the last chunk in the source array.
        for (int i = 0; i < runs - 1; ++i) {
            insertionSort(source, 
                          localFromIndex, 
                          localFromIndex += INSERTIONSORT_THRESHOLD);
        }

        // Presort the last chunk that may be shorter than 
        // 'insertionsortThreshold'.
        insertionSort(source,
                      localFromIndex,
                      Math.min(sourceOffset + rangeLength, 
                               localFromIndex + INSERTIONSORT_THRESHOLD));
    }

    /**
     * Merges the first run with the second one, the third one with the fourth
     * one, and so on until all possible merges are performed. If there is an
     * odd number of runs, the last one is copied into the target array as it 
     * may appear in the target array as two smaller unmerged runs.
     * 
     * @param  runs      the amount of runs in the source array.
     * @param  runLength the current run length.
     * @return the amount of runs merged.
     */
    private void mergePass(int runs, int runLength) {
        int runIndex = 0;

        for (; runIndex < runs - 1; runIndex += 2) {
            // Set up the indices.
            int leftIndex = sourceOffset + runIndex * runLength;
            int leftBound = leftIndex + runLength;
            int rightBound = Math.min(leftBound + runLength, 
                                      rangeLength + sourceOffset);
            int targetIndex = targetOffset + runIndex * runLength;

            // Perform the actual merging.
            merge(leftIndex, leftBound, rightBound, targetIndex);
        }
        
        if (runIndex < runs) {
            // There was an odd number of runs in the source array, and
            // thus, the last run was an "orphan" run. We need to copy it 
            // to the current target array as it may appear there as two
            // smaller unmerged runs.
            System.arraycopy(source,
                             sourceOffset + runIndex * runLength,
                             target,
                             targetOffset + runIndex * runLength,
                             rangeLength - runIndex * runLength);
        }
    }

    /**
     * Sorts the entire array.
     * 
     * @param <T>   the array component type.
     * @param array the array to sort.
     */
    public static <T extends Comparable<? super T>> void sort(T[] array) {
        sort(array, 0, array.length);
    }

    /**
     * Sorts the range {@code array[fromIndex], array[fromIndex + 1], ..., 
     * array[toIndex - 2], array[toIndex - 1]}.
     * 
     * @param <T>       the array component type.
     * @param array     the array containing the requested range.
     * @param fromIndex the starting (inclusive) index of the range to sort.
     * @param toIndex   the ending (exclusive) index of the range to sort.
     */
    public static <T extends Comparable<? super T>> void sort(T[] array, 
                                                              int fromIndex, 
                                                              int toIndex) {
        new BottomUpMergesort(array, fromIndex, toIndex).sort();
    }

    /**
     * Sorts the range {@code array[fromIndex,], array[fromIndex + 1], ...,
     * array[toIndex - 2], array[toIndex - 1]} using insertion sort. This 
     * implementation is <b>stable</b>.
     * 
     * @param <T>       the array component type.
     * @param array     the array holding the requested range.
     * @param fromIndex the starting (inclusive) index.
     * @param toIndex   the ending (exclusive) index.
     */
    public static <T extends Comparable<? super T>> 
        void insertionSort(T[] array,
                           int fromIndex,
                           int toIndex) {
        for (int i = fromIndex + 1; i < toIndex; ++i) {
            T element = array[i];
            int j = i;

            for (; j > fromIndex && array[j - 1].compareTo(element) > 0; --j) {
                array[j] = array[j - 1];
            }

            array[j] = element;
        }
    }

    /**
     * Merges the sorted ranges {@code source[leftIndex, leftBound)} and
     * {@code source[rightIndex, rightBound)} putting the result to 
     * {@code target} starting from component with index {@code targetIndex}.
     * 
     * @param <T>         the array component type.
     * @param source      the source array.
     * @param target      the target array.
     * @param leftIndex   the (inclusive) starting index of the left run.
     * @param leftBound   the (exclusive) ending index of the left run.
     * @param rightIndex  the (inclusive) starting index of the right run.
     * @param rightBound  the (exclusive) ending index of the right run.
     * @param targetIndex the starting index of the result run in the target
     *                     array.
     * @param cmp         the element comparator.
     */
    private void merge(int leftIndex,
                       int leftBound,
                       int rightBound,
                       int targetIndex) {
        int rightIndex = leftBound;

        while (leftIndex < leftBound && rightIndex < rightBound) {
            target[targetIndex++] = 
                    source[rightIndex].compareTo(source[leftIndex]) < 0 ?
                        source[rightIndex++] :
                        source[leftIndex++];
        }

        System.arraycopy(source, 
                         leftIndex, 
                         target, 
                         targetIndex, 
                         leftBound - leftIndex);

        System.arraycopy(source, 
                         rightIndex, 
                         target, 
                         targetIndex, 
                         rightBound - rightIndex);
    }

    /**
     * Checks that {@code fromIndex} and {@code toIndex} are sensible and throws
     * an exception if they are not.
     * 
     * @param arrayLength the length of the array.
     * @param fromIndex   the starting (inclusive) index of the range to sort.
     * @param toIndex     the ending (exclusive) index of the range to sort.
     * @throws IllegalArgumentException if {@code fromIndex} is larger than
     *                                  {@code toIndex}.
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex} is negative
     *                                        of {@code toIndex} is too large.
     */
    private static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                    "'fromIndex' is larger than 'toIndex': " +
                    fromIndex + " > " + toIndex);
        }

        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(
                    "'fromIndex' is negative: " + fromIndex);
        }

        if (toIndex > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(
                    "'toIndex' is too large: " + toIndex + ", array length: " +
                    arrayLength);
        }
    }
}
