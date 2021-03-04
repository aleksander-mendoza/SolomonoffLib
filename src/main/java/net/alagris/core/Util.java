package net.alagris.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.*;

public class Util {



    /**Just like List.removeIf but this one is */
    public static <X> void removeTail(List<X> list, int desiredLength) {
        while (list.size() > desiredLength) {
            list.remove(list.size() - 1);
        }
    }

    public static <X> ArrayList<X> filledArrayList(int size, X defaultElement) {
        return filledArrayListFunc(size, i -> defaultElement);
    }

    public  static <X> ArrayList<X> filledArrayListFunc(int size, Function<Integer, X> defaultElement) {
        ArrayList<X> arr = new ArrayList<>(size);
        for (int i = 0; i < size; i++) arr.add(defaultElement.apply(i));
        return arr;
    }

    public static <X> ArrayList<X> concatArrayLists(List<X> a, List<X> b) {
        ArrayList<X> arr = new ArrayList<>(a.size() + b.size());
        arr.addAll(a);
        arr.addAll(b);
        return arr;
    }

    public static <X, Y> Y fold(Collection<X> a, Y init, BiFunction<X, Y, Y> b) {
        for (X x : a) {
            init = b.apply(x, init);
        }
        return init;
    }

    /**
     * returns new size of list
     */
    public static <X> int shiftDuplicates(List<X> sortedArray, BiPredicate<X, X> areEqual, BiConsumer<X, X> mergeEqual) {
        if (sortedArray.size() > 1) {
            int i = 0;
            for (int j = 1; j < sortedArray.size(); j++) {
                X prev = sortedArray.get(i);
                X curr = sortedArray.get(j);
                if (areEqual.test(prev, curr)) {
                    mergeEqual.accept(prev, curr);
                } else {
                    i++;
                    sortedArray.set(i, curr);
                }
            }
            return i + 1;
        }
        return sortedArray.size();
    }

    /**
     * removes duplicates in an array. First the array is sorted adn then all the equal elements
     * are merged into one. You can specify when elements are equal and how to merge them
     */
    public static <X> void removeDuplicates(List<X> sortedArray, BiPredicate<X, X> areEqual, BiConsumer<X, X> mergeEqual) {
        removeTail(sortedArray, shiftDuplicates(sortedArray, areEqual, mergeEqual));

    }

    public static <X> Iterator<X> lazyConcatIterator(Iterator<X> lhs, Iterator<X> rhs) {
        return new Iterator<X>() {
            boolean finishedLhs = false;

            @Override
            public boolean hasNext() {
                if (finishedLhs) return rhs.hasNext();
                return lhs.hasNext() || rhs.hasNext();
            }

            @Override
            public X next() {
                if (finishedLhs) return rhs.next();
                if (lhs.hasNext()) return lhs.next();
                finishedLhs = true;
                return rhs.next();
            }
        };
    }
    /**
     * Yields undefined behaviour if underlying structure is mutated. If you need to
     * edit the structure of transitions, then to it in a new separate copy
     */
    public static <X> List<X> lazyConcatImmutableLists(List<X> lhs, List<X> rhs) {
        return new AbstractList<X>() {
            /**this should not change*/
            final int offset = lhs.size();

            @Override
            public Iterator<X> iterator() {
                return lazyConcatIterator(lhs.iterator(),rhs.iterator());
            }

            @Override
            public X get(int index) {
                return index < offset ? lhs.get(index) : rhs.get(index - offset);
            }

            @Override
            public int size() {
                return offset + lhs.size();
            }
        };
    }

    /**
     * This is a utility function that can take some list of transitions. If the list is null,
     * then a sink transition is implied. Sink transition is represented by null
     */
    public static <Y> List<Y> listOrSingletonWithNull(List<Y> list) {
        return list.isEmpty() ? Collections.singletonList(null) : list;
    }

    public static <X, Y> List<Y> mapListLazy(List<X> list, Function<X, Y> f) {
        return new AbstractList<Y>() {
            @Override
            public Y get(int index) {
                return f.apply(list.get(index));
            }

            @Override
            public int size() {
                return list.size();
            }
        };
    }

    public static <X, Y> Iterator<Y> mapIterLazy(Iterator<X> i, Function<X, Y> f) {
        return new Iterator<Y>() {
            @Override
            public void remove() {
                i.remove();
            }

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public Y next() {
                return f.apply(i.next());
            }
        };
    }

    public static Iterator<Integer> iterNumbers(int initInclusive, int endExclusive){
        return new Iterator<Integer>() {
            int i=initInclusive;
            @Override
            public boolean hasNext() {
                return i<endExclusive;
            }

            @Override
            public Integer next() {
                return i++;
            }
        };
    }
    public static <X> Iterator<X> iterArray(X[] array){
        return iterArray(0,array.length,array);
    }


    public static <X> Iterator<X> iterArray(int offset,int length,X[] array){
        return new Iterator<X>() {
            int i=offset;
            @Override
            public boolean hasNext() {
                return i<length;
            }

            @Override
            public X next() {
                return array[i++];
            }
        };
    }

    public static Iterator<Integer> iterArray(int[] array){
        return iterArray(0,array.length,array);
    }
    public static Iterator<Integer> iterArray(int offset,int length,int[] array){
        return new Iterator<Integer>() {
            int i=offset;
            @Override
            public boolean hasNext() {
                return i<length;
            }

            @Override
            public Integer next() {
                return array[i++];
            }
        };
    }

    /**
     * Searches a collection for an element that satisfies some predicate
     */
    public static <T> T find(Iterable<T> c, Predicate<T> pred) {
        for (T t : c) if (pred.test(t)) return t;
        return null;
    }

    public static <T> T find(T[] c, Predicate<T> pred) {
        for (T t : c) if (pred.test(t)) return t;
        return null;
    }


    public static <T> boolean forall(Iterable<T> c, Predicate<T> pred) {
        for (T t : c) if (!pred.test(t)) return false;
        return true;
    }

    public static <T> boolean forall(T[] c, Predicate<T> pred) {
        for (T t : c) if (!pred.test(t)) return false;
        return true;
    }

    public static <T> boolean exists(Iterable<T> c, Predicate<T> pred) {
        for (T t : c) if (pred.test(t)) return true;
        return false;
    }
    public static <T,U> boolean unique(Iterable<T> c, Function<T,U> f) {
        final HashSet<U> uniq = new HashSet<>();
        for (T t : c) if (!uniq.add(f.apply(t))) return false;
        return true;
    }
    public static <T,U> boolean unique(T[] c, Function<T,U> f) {
        final HashSet<U> uniq = new HashSet<>();
        for (T t : c) if (!uniq.add(f.apply(t))) return false;
        return true;
    }

    public static <T> int count(Iterable<T> c, Predicate<T> pred) {
        int i=0;
        for (T t : c) if (pred.test(t)) i++;
        return i;
    }

    public static <X> ArrayList<X> singeltonArrayList(X x) {
        ArrayList<X> a = new ArrayList<>(1);
        a.add(x);
        return a;
    }


    public static <X,Y extends List<X>> Y addAllIfAbsent(Y a,List<X> b){
        for(X s:b){
            if(!a.contains(s))a.add(s);
        }
        return a;
    }

    /**This is the proper way in which the indexOf function should have been implemented in
     * Java's standard library. Unfortunately the engineers who created Java didn't understand much
     * about programming.*/
    public static int indexOf(String s,int from,char c){
        while(from<s.length()){
            if(s.charAt(from)==c)break;
            else from++;
        }
        return from;
    }

    public static <T> int indexOf(T[] s,int from,Predicate<T> predicate){
        while(from<s.length){
            if(predicate.test(s[from]))break;
            else from++;
        }
        return from;
    }

    /**
     * Find the Levenshtein distance between two CharSequences if it's less than or
     * equal to a given threshold.
     *
     * <p>
     * This implementation follows from Algorithms on Strings, Trees and
     * Sequences by Dan Gusfield and Chas Emerick's implementation of the
     * Levenshtein distance algorithm from <a
     * href="http://www.merriampark.com/ld.htm"
     * >http://www.merriampark.com/ld.htm</a>
     * </p>
     *
     * <pre>
     * limitedCompare(null, *, *)             = IllegalArgumentException
     * limitedCompare(*, null, *)             = IllegalArgumentException
     * limitedCompare(*, *, -1)               = IllegalArgumentException
     * limitedCompare("","", 0)               = 0
     * limitedCompare("aaapppp", "", 8)       = 7
     * limitedCompare("aaapppp", "", 7)       = 7
     * limitedCompare("aaapppp", "", 6))      = -1
     * limitedCompare("elephant", "hippo", 7) = 7
     * limitedCompare("elephant", "hippo", 6) = -1
     * limitedCompare("hippo", "elephant", 7) = 7
     * limitedCompare("hippo", "elephant", 6) = -1
     * </pre>
     *
     * @param left the first CharSequence, must not be null
     * @param right the second CharSequence, must not be null
     * @param threshold the target threshold, must not be negative
     * @return result distance, or -1
     */
    private static int limitedCompare(CharSequence left, CharSequence right, final int threshold) { // NOPMD
        if (left == null || right == null) {
            throw new IllegalArgumentException("CharSequences must not be null");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must not be negative");
        }
        /*
         * This implementation only computes the distance if it's less than or
         * equal to the threshold value, returning -1 if it's greater. The
         * advantage is performance: unbounded distance is O(nm), but a bound of
         * k allows us to reduce it to O(km) time by only computing a diagonal
         * stripe of width 2k + 1 of the cost table. It is also possible to use
         * this to compute the unbounded Levenshtein distance by starting the
         * threshold at 1 and doubling each time until the distance is found;
         * this is O(dm), where d is the distance.
         *
         * One subtlety comes from needing to ignore entries on the border of
         * our stripe eg. p[] = |#|#|#|* d[] = *|#|#|#| We must ignore the entry
         * to the left of the leftmost member We must ignore the entry above the
         * rightmost member
         *
         * Another subtlety comes from our stripe running off the matrix if the
         * strings aren't of the same size. Since string s is always swapped to
         * be the shorter of the two, the stripe will always run off to the
         * upper right instead of the lower left of the matrix.
         *
         * As a concrete example, suppose s is of length 5, t is of length 7,
         * and our threshold is 1. In this case we're going to walk a stripe of
         * length 3. The matrix would look like so:
         *
         * <pre>
         *    1 2 3 4 5
         * 1 |#|#| | | |
         * 2 |#|#|#| | |
         * 3 | |#|#|#| |
         * 4 | | |#|#|#|
         * 5 | | | |#|#|
         * 6 | | | | |#|
         * 7 | | | | | |
         * </pre>
         *
         * Note how the stripe leads off the table as there is no possible way
         * to turn a string of length 5 into one of length 7 in edit distance of
         * 1.
         *
         * Additionally, this implementation decreases memory usage by using two
         * single-dimensional arrays and swapping them back and forth instead of
         * allocating an entire n by m matrix. This requires a few minor
         * changes, such as immediately returning when it's detected that the
         * stripe has run off the matrix and initially filling the arrays with
         * large values so that entries we don't compute are ignored.
         *
         * See Algorithms on Strings, Trees and Sequences by Dan Gusfield for
         * some discussion.
         */
        int n = left.length(); // length of left
        int m = right.length(); // length of right
        // if one string is empty, the edit distance is necessarily the length
        // of the other
        if (n == 0) {
            return m <= threshold ? m : -1;
        } else if (m == 0) {
            return n <= threshold ? n : -1;
        }
        if (n > m) {
            // swap the two strings to consume less memory
            final CharSequence tmp = left;
            left = right;
            right = tmp;
            n = m;
            m = right.length();
        }
        // the edit distance cannot be less than the length difference
        if (m - n > threshold) {
            return -1;
        }
        int[] p = new int[n + 1]; // 'previous' cost array, horizontally
        int[] d = new int[n + 1]; // cost array, horizontally
        int[] tempD; // placeholder to assist in swapping p and d
        // fill in starting table values
        final int boundary = Math.min(n, threshold) + 1;
        for (int i = 0; i < boundary; i++) {
            p[i] = i;
        }
        // these fills ensure that the value above the rightmost entry of our
        // stripe will be ignored in following loop iterations
        Arrays.fill(p, boundary, p.length, Integer.MAX_VALUE);
        Arrays.fill(d, Integer.MAX_VALUE);
        // iterates through t
        for (int j = 1; j <= m; j++) {
            final char rightJ = right.charAt(j - 1); // jth character of right
            d[0] = j;
            // compute stripe indices, constrain to array size
            final int min = Math.max(1, j - threshold);
            final int max = j > Integer.MAX_VALUE - threshold ? n : Math.min(
                    n, j + threshold);
            // ignore entry left of leftmost
            if (min > 1) {
                d[min - 1] = Integer.MAX_VALUE;
            }
            // iterates through [min, max] in s
            for (int i = min; i <= max; i++) {
                if (left.charAt(i - 1) == rightJ) {
                    // diagonally left and up
                    d[i] = p[i - 1];
                } else {
                    // 1 + minimum of cell to the left, to the top, diagonally
                    // left and up
                    d[i] = 1 + Math.min(Math.min(d[i - 1], p[i]), p[i - 1]);
                }
            }
            // copy current distance counts to 'previous row' distance counts
            tempD = p;
            p = d;
            d = tempD;
        }
        // if p[n] is greater than the threshold, there's no guarantee on it
        // being the correct
        // distance
        if (p[n] <= threshold) {
            return p[n];
        }
        return -1;
    }

    public static String findLevenshtein(String query, Iterable<String> dictionary) {
        int minDist = 9999;
        String closestMatch = null;
        for(String entry :dictionary){
            final int dist = limitedCompare(query,entry,minDist);
            if(dist>-1 && dist<minDist){
                closestMatch = entry;
                minDist = dist;
            }
        }
        return closestMatch;
    }

    public static String[] split(String str,char separator) {
       return split(str,separator,0);
    }

    public static String[] split(String str,char separator,int maxElements) {
        assert maxElements>=0;
        int requiredElements = 1;
        for(int i=0;i<str.length();i++)if(str.charAt(i)==separator)requiredElements++;
        final String[] parts = new String[maxElements==0?requiredElements:Math.min(maxElements,requiredElements)];
        maxElements--;
        int i=0;
        int fromInclusive=0;
        while(fromInclusive<=str.length()){
            assert i>=0;
            if(i==maxElements){
                assert maxElements<parts.length;
                parts[i++] = str.substring(fromInclusive);
                break;
            }
            final int toExclusive = indexOf(str,fromInclusive,separator);
            parts[i++] = str.substring(fromInclusive,toExclusive);
            fromInclusive = toExclusive+1;
        }
        assert i==parts.length:i+" "+Arrays.toString(parts);
        return parts;
    }

    public static String readString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toString();
    }

}
