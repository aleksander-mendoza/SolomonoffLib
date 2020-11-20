package net.alagris;

public class IntQueue {
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
        if (q == null) return tail;
        final IntQueue first = q;
        while (q.next != null) {
            q = q.next;
        }
        q.next = tail;
        return first;
    }

    public static IntQueue copyAndConcat(IntQueue q, IntQueue tail) {
        if (q == null) return tail;
        final IntQueue root = new IntQueue();
        root.value = q.value;
        IntQueue curr = root;
        q = q.next;
        while (q != null) {
            curr.next = new IntQueue();
            curr = curr.next;
            curr.value = q.value;
            q = q.next;
        }
        curr.next = tail;
        return root;
    }


    public static boolean eq(IntQueue a, IntQueue b) {
        while (a != null && b != null) {
            if (a.value != b.value) return false;
            a = a.next;
            b = b.next;
        }
        return a == null && b == null;
    }

    public static IntQueue asQueue(IntSeq str, int fromInclusive, int toExclusive) {
        IntQueue q = null;
        assert fromInclusive <= str.size();
        assert 0 <= fromInclusive;
        assert toExclusive < str.size();
        assert 0 <= toExclusive;
        for (int i = toExclusive - 1; i >= fromInclusive; i--) {
            IntQueue next = new IntQueue();
            next.value = str.get(i);
            next.next = q;
            q = next;
        }
        return q;
    }
}