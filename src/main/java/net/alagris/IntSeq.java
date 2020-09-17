package net.alagris;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Sequence of integers implementation
 */
public final class IntSeq implements Seq<Integer>, Comparable<IntSeq> {

    public static final IntSeq Epsilon = new IntSeq(new int[0]);

    public final int[] arr;
    public final int size;

    public IntSeq(String s) {
        this(s.codePoints().toArray());
    }

    private IntSeq(int[] arr) {
        this(arr, arr.length);
    }

    IntSeq(int[] arr, int size) {
        this.arr = arr;
        this.size = size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Integer get(int i) {
        return arr[i];
    }

    public IntSeq concat(IntSeq rhs) {
        int[] n = new int[size() + rhs.size()];
        System.arraycopy(arr, 0, n, 0, size());
        System.arraycopy(rhs.arr, 0, n, size(), rhs.size());
        return new IntSeq(n);
    }

    @Override
    public boolean equals(Object obj) {
        IntSeq rhs = (IntSeq) obj;
        return Arrays.equals(arr, rhs.arr);
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < arr.length;
            }

            @Override
            public Integer next() {
                return arr[i++];
            }
        };
    }

    @Override
    public String toString() {
        return Arrays.toString(arr);
    }


    @Override
    public int compareTo(IntSeq other) {
        int len1 = size();
        int len2 = other.size();
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            int c1 = get(k);
            int c2 = other.get(k);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }
}
