package net.coderodde.util.sorting;

import java.util.Arrays;

/**
 * This class implements a parallel merge sort.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Mar 9, 2016)
 */
public class ParallelMergesort {
   
    private static final int MINIMUM_THREAD_WORKLOAD = 100_000;
    
    public static <T extends Comparable<? super T>> void sort(T[] array) {
        sort(array, 0, array.length);
    }
    
    public static <T extends Comparable<? super T>> void sort(T[] array,
                                                              int fromIndex,
                                                              int toIndex) {
        int rangeLength = toIndex - fromIndex;
        int threads = Math.min(rangeLength / MINIMUM_THREAD_WORKLOAD,
                               Runtime.getRuntime().availableProcessors());
        
        threads = fixThreadCount(threads);
        
        if (threads < 2) {
            BottomUpMergesort.sort(array, fromIndex, toIndex);
            return;
        }
        
        int leftPartLength  = rangeLength >>> 1;
        int rightPartLength = rangeLength - leftPartLength;
        T[] aux = Arrays.copyOfRange(array, fromIndex, toIndex);
        
        SorterThread<T> thread1 = new SorterThread<>(threads >>> 1,
                                                     array,
                                                     aux,
                                                     fromIndex,
                                                     0,
                                                     leftPartLength);
        
        thread1.start();
        
        SorterThread<T> thread2 = new SorterThread<>(threads - threads >>> 1,
                                                     array,
                                                     aux,
                                                     fromIndex + leftPartLength,
                                                     leftPartLength,
                                                     rightPartLength);
        thread2.run();
        
        try {
            thread1.join();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(
                    "A SorterThread threw an IllegalStateException.");
        }
        
        merge(aux, array, 0, fromIndex, leftPartLength, rightPartLength);
    }
    
    private static <T extends Comparable<? super T>> 
    void merge(T[] source,
               T[] target,
               int sourceOffset,
               int targetOffset,
               int leftRunLength,
               int rightRunLength) {
        int left  = sourceOffset;
        int leftUpperBound = sourceOffset + leftRunLength;
        int right = leftUpperBound;
        int rightUpperBound = leftUpperBound + rightRunLength;
        int targetIndex = targetOffset;
        
        while (left < leftUpperBound && right < rightUpperBound) {
            target[targetIndex++] =
                    source[right].compareTo(source[left]) < 0 ?
                    source[right++] :
                    source[left++];
        }
        
        System.arraycopy(source, 
                         left, 
                         target, 
                         targetIndex, 
                         leftUpperBound - left);
        
        System.arraycopy(source, 
                         right, 
                         target, 
                         targetIndex, 
                         rightUpperBound - right);
    }
    
    private static int fixThreadCount(int threads) {
        int ret = 1;
        
        while (ret < threads) {
            ret <<= 1;
        }
        
        return ret;
    }
    
    private static int intLog(int number) {
        int log = 0;
        int num = 1;
        
        while (num < number) {
            num *= 2;
            log++;
        }
        
        return log;
    }
    
    
    private static final class SorterThread<T extends Comparable<? super T>> 
    extends Thread {
        
        private final int threads;
        private final T[] source;
        private final T[] target;
        private final int sourceOffset;
        private final int targetOffset;
        private final int rangeLength;
        
        SorterThread(int threads,
                     T[] source,
                     T[] target,
                     int sourceOffset, 
                     int targetOffset,
                     int rangeLength) {
            this.threads = threads;
            this.source = source;
            this.target = target;
            this.sourceOffset = sourceOffset;
            this.targetOffset = targetOffset;
            this.rangeLength = rangeLength;
        }
        
        @Override
        public void run() {
            if (threads < 2) {
                BottomUpMergesort.sort(target,
                                       targetOffset,
                                       targetOffset + rangeLength);
                return;
            }
            
            int leftPartLength = rangeLength / 2;
            
            SorterThread<T> thread1 = new SorterThread<>(threads / 2,
                                                         target,
                                                         source,
                                                         targetOffset,
                                                         sourceOffset,
                                                         leftPartLength);
            
            thread1.start();
            
            SorterThread<T> thread2 = new SorterThread<>(
                                                threads - threads / 2,
                                                target, 
                                                source,
                                                targetOffset + leftPartLength,
                                                sourceOffset + leftPartLength,
                                                rangeLength - leftPartLength);
            
            thread2.run();
            
            try {
                thread1.join();
            } catch (InterruptedException ex) {
                throw new IllegalStateException(
                        "A SorterThread threw InterruptedException.");
            }
            
            merge(source, 
                  target, 
                  sourceOffset, 
                  targetOffset, 
                  leftPartLength,
                  rangeLength - leftPartLength);
        }
    }
}
