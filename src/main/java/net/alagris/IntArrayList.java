package net.alagris;

import java.util.Arrays;
import java.util.PrimitiveIterator.OfInt;

public class IntArrayList implements Iterable<Integer> {
    private int[] data;
    private int size;

    public IntArrayList() {
        this(10);
    }

    public IntArrayList(String str) {
        data = str.codePoints().toArray();
        size = data.length;
    }

    public IntArrayList(int initialCapacity) {
        data = new int[Math.max(initialCapacity, 10)];
        size = 0;
    }

    public IntArrayList(IntArrayList other) {
        data = new int[other.size()];
        System.arraycopy(other.data, 0, data, 0, other.size());
        size = other.size;
    }

    public IntArrayList(IntArrayList a, IntArrayList b) {
        data = new int[a.size() + b.size()];
        System.arraycopy(a.data, 0, data, 0, a.size());
        System.arraycopy(b.data, 0, data, a.size(), b.size());
        size = data.length;
    }

    public IntArrayList(int codePointCount, OfInt iterator) {
        data = new int[Math.max(codePointCount, 10)];
        size = codePointCount;
        int i = 0;
        while (iterator.hasNext() && i<size) {
            data[i++] = iterator.nextInt();
        }
    }

    public int size() {
        return size;
    }

    public void ensureCapacity(final int capacity) {
        if (capacity > data.length) {
            int[] newData = new int[capacity];
            if (size != 0) {
                System.arraycopy(data, 0, newData, 0, size);
            }
            data = newData;
        }
    }

    public void append(IntArrayList b) {
        ensureCapacity(size() + b.size());
        System.arraycopy(b.data, 0, data, size(), b.size());
        size = size() + b.size();
    }
    
    public void append(int b) {
        ensureCapacity(size() + 1);
        data[size()] = b;
        size++;
    }

    public void prepend(IntArrayList b) {
        ensureCapacity(size() + b.size());
        System.arraycopy(data, 0, data, b.size(), size());
        System.arraycopy(b.data, 0, data, 0, b.size());
        size = size() + b.size();
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(int val) {
        for (int i = 0; i < size; i++) {
            if (data[i] == val)
                return true;
        }
        return false;
    }

    public void replace(int old, int replacement, int size) {
        for (int i = 0; i < size; i++) {
            if (data[i] == old)
                data[i] = replacement;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(size);
        for(int i=0;i<size;i++) {
            int val = data[i];
            if(val==MealyParser.CompiledStructDef.STRUCT_INSATNCE_SEPARATOR) {
                sb.append("<EOS>");
            }else if(val==MealyParser.CompiledStructDef.STRUCT_MEMEBER_SEPARATOR) {
                sb.append("<EOF>");
            }else if(val==0){
                sb.append("\\0");
            }else {
                sb.appendCodePoint(val);    
            }
            
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntArrayList) {
            IntArrayList o = (IntArrayList) obj;
            if (size == o.size) {
                for (int i = 0; i < size; i++) {
                    if (data[i] != o.data[i])
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public OfInt iterator() {
        return new OfInt() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public int nextInt() {
                return data[i++];
            }
        };
    }

    public int get(int charIndex) {
        if (charIndex < 0 || charIndex >= size)
            throw new IndexOutOfBoundsException(charIndex + " for size " + size);
        return data[charIndex];
    }

    public String toArrString() {
        return Arrays.toString(Arrays.copyOf(data, size));
    }

    public static IntArrayList singleton(int i) {
        return new IntArrayList(1, new OfInt() {
            
            @Override
            public boolean hasNext() {
                return true;
            }
            
            @Override
            public int nextInt() {
                return i;
            }
        });
    }
}
