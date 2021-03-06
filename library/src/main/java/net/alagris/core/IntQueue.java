package net.alagris.core;

public class IntQueue implements Queue<Integer, IntQueue> {
    public int value;
    public IntQueue next;

    public IntQueue() {
    }

    public IntQueue(int value) {
        this.value = value;
    }


    @Override
    public String toString() {
        return IntQueue.toString(this);
    }


    public static String toString(IntQueue ints) {
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

    public static IntQueue reverseCopyAndConcat(IntQueue q, IntQueue tail) {
        return Queue.reverseCopyAndConcat(q, tail, IntQueue::new);
    }


    public static boolean equals(IntQueue a, IntQueue b) {
        return Queue.equals(a, b);
    }
    public static boolean equals(IntQueue a, IntSeq b) {
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

    public static int[] arr(IntQueue q) {
        final int[] arr = new int[Queue.len(q)];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = q.value;
            q = q.next;
        }
        return arr;
    }
}