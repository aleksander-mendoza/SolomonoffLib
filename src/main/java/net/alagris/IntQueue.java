package net.alagris;

import java.util.function.Supplier;

public class IntQueue implements Queue<Integer,IntQueue> {
    int value;
    IntQueue next;

    @Override
    public String toString() {
        return IntQueue.toString(this);
    }


    private static String toString(IntQueue ints) {
        if (ints == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(ints.value);
        ints = ints.next;
        while (ints != null) {
            sb.append(" ").append(ints.value);
            ints = ints.next;
        }
        return sb.toString();
    }


    public static IntQueue concat(IntQueue q, IntQueue tail) {
        return Queue.concat(q, tail);
    }

    public static IntQueue copyAndConcat(IntQueue q, IntQueue tail) {
        return Queue.copyAndConcat(q, tail, IntQueue::new);
    }


    public static boolean equals(IntQueue a, IntQueue b) {
        return Queue.equals(a, b);
    }


    @Override
    public Integer val() {
        return value;
    }

    @Override
    public void val(Integer integer) {
        value = integer;
    }

    @Override
    public void next(IntQueue next) {
        this.next = next;
    }

    @Override
    public IntQueue next() {
        return next;
    }

    public static IntQueue asQueue(IntSeq str) {
        return Queue.asQueue(str, 0, str.size(), IntQueue::new);
    }

    public static IntQueue asQueue(IntSeq str, int fromInclusive, int toExclusive) {
        return Queue.asQueue(str, fromInclusive, toExclusive, IntQueue::new);
    }
}