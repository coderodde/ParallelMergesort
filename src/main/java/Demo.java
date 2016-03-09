import java.util.Arrays;
import java.util.Random;
import net.coderodde.util.sorting.ParallelMergesort;

public class Demo {

    private static final int SIZE = 5000000;
    private static final int FROM_INDEX = 3;
    private static final int TO_INDEX = SIZE - 2;

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        Random random = new Random(seed);
        Integer[] array1 = createRandomIntegerArray(SIZE, random);
        Integer[] array2 = array1.clone();

        System.out.println("Seed: " + seed);

        //// java.util.Arrays.sort
        long ta = System.currentTimeMillis();
        Arrays.sort(array1, FROM_INDEX, TO_INDEX);
        long tb = System.currentTimeMillis();

        System.out.println(
                "java.util.Arrays.sort() in " + (tb - ta) + " ms. Sorted: " +
                isSorted(array1, FROM_INDEX, TO_INDEX));

        //// net.coderodde.util.sorting.BottomUpMergesort.sort
        ta = System.currentTimeMillis();
        ParallelMergesort.sort(array2, FROM_INDEX, TO_INDEX);
        tb = System.currentTimeMillis();

        System.out.println(
                "net.coderodde.util.sorting.ParallelMergesort.sort() " +
                (tb - ta) + " ms. Sorted: " + 
                isSorted(array2, FROM_INDEX, TO_INDEX));

        System.out.println(
                "Arrays identical: " + arraysIdentical(array1, array2));
    }
    
    static <T extends Comparable<? super T>> boolean isSorted(T[] array,
                                                              int fromIndex,
                                                              int toIndex) {
        for (int i = fromIndex; i < toIndex - 1; ++i) {
            if (array[i].compareTo(array[i + 1]) > 0) {
                return false;
            }
        }

        return true;
    }

    static <T extends Comparable<? super T>> boolean isSorted(T[] array) {
        return isSorted(array, 0, array.length);
    }
    
    static <T> boolean arraysIdentical(T[] arr1, T[] arr2) {
        if (arr1.length != arr2.length) {
            return false;
        }

        for (int i = 0; i < arr1.length; ++i) {
            if (arr1[i] != arr2[i]) {
                return false;
            }
        }

        return true;
    }
    
    static Integer[] createRandomIntegerArray(int size, Random random) {
        Integer[] ret = new Integer[size];

        for (int i = 0; i < size; ++i) {
            ret[i] = random.nextInt(size);
        }

        return ret;
    }
}
