package net.alagris;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Sequence of integers implementation
 */
public final class IntSeq implements Seq<Integer>, Comparable<IntSeq>, List<Integer> {

    public static final IntSeq Epsilon = new IntSeq(new int[0]);

    public final int[] arr;
    public final int size;

    public IntSeq(String s) {
        this(s.codePoints().toArray());
    }

    public IntSeq(int... arr) {
        this(arr, arr.length);
    }

    public IntSeq(int[] arr, int size) {
        this.arr = arr;
        this.size = size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o)>-1;
    }

    @Override
    public Integer get(int i) {
        return arr[i];
    }

    private int hash = 0;
    @Override
    public int hashCode() {
        if(hash==0)hash = Arrays.hashCode(arr);
        return hash;
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public Integer set(int index, Integer element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, Integer element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        int j = (int)o;
        int i=-1;
        while(++i<size())if(arr[i]==j)return i;
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int j = (int)o;
        int i=size();
        while(--i>=0)if(arr[i]==j)return i;
        return -1;
    }

    @Override
    public ListIterator<Integer> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Integer> listIterator(int index) {
        return new ListIterator<Integer>() {
            int i = index;
            @Override
            public boolean hasNext() {
                return i<size();
            }

            @Override
            public Integer next() {
                return arr[i++];
            }

            @Override
            public boolean hasPrevious() {
                return i>0;
            }

            @Override
            public Integer previous() {
                return arr[i--];
            }

            @Override
            public int nextIndex() {
                return i+1;
            }

            @Override
            public int previousIndex() {
                return i-1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(Integer integer) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(Integer integer) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public List<Integer> subList(int fromIndex, int toIndex) {
        return new AbstractList<Integer>() {
            @Override
            public Integer get(int index) {
                if(index+fromIndex>=toIndex) throw new ArrayIndexOutOfBoundsException("index="+index+" size="+size());
                return arr[index+fromIndex];
            }

            @Override
            public int size() {
                return toIndex-fromIndex;
            }
        };
    }

    @Override
    public Spliterator<Integer> spliterator() {
        return subList(0,size()).spliterator();
    }

    @Override
    public Stream<Integer> stream() {
        return Arrays.stream(arr).boxed();
    }

    @Override
    public Stream<Integer> parallelStream() {
        return subList(0,size()).parallelStream();
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
                return i < size();
            }

            @Override
            public Integer next() {
                return arr[i++];
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Integer> action) {
        for(int i=0;i<size();i++)action.accept(arr[i]);
    }

    @Override
    public Object[] toArray() {
        return Arrays.stream(arr).boxed().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Integer[] e = new Integer[size()];
        for(int i=0;i<size();i++)e[i] = arr[i];
        return (T[]) e;
    }

    @Override
    public boolean add(Integer integer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Collection<Integer> ic = (Collection<Integer>)c;
        for(int i:ic){
            if(!contains(i))return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends Integer> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(Predicate<? super Integer> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAll(UnaryOperator<Integer> operator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sort(Comparator<? super Integer> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
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
