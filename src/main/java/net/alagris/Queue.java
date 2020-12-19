package net.alagris;

import java.util.HashSet;
import java.util.Objects;
import java.util.function.Supplier;

public interface Queue<X,N extends Queue<X,N>> {
    X val();

    void val(X x);

    void next(N next);

    N next();

    public static <X,N extends Queue<X,N>> boolean hasCycle(N q){
        final HashSet<N> elements = new HashSet<>();
        while(q!=null){
            if(!elements.add(q)){
                return true;
            }
            q = q.next();
        }
        return false;
    }
    public static <X,N extends Queue<X,N>> N concat(N q, N tail) {
        assert !hasCycle(q) && !hasCycle(tail);
        if (q == null) return tail;
        final N first = q;
        while ( q.next() !=null){
            q = q.next();
        }
        q.next(tail);
        assert !hasCycle(first);
        return first;
    }

    public static  <X,N extends Queue<X,N>> N copyAndConcat(N q, N tail, Supplier<N> make) {
        assert !hasCycle(q) && !hasCycle(tail);
        if (q == null) return tail;
        final N root = make.get();
        root.val(q.val());
        N curr = root;
        q = q.next();
        while (q != null) {
            curr.next(make.get());
            curr = curr.next();
            curr.val(q.val());
            q = q.next();
        }
        curr.next(tail);
        assert !hasCycle(root);
        return root;
    }

    public static  <X,N extends Queue<X,N>> boolean equals(N a, N b) {
        while (a != null && b != null) {
            if (!Objects.equals(a.val(), b.val())) return false;
            a = a.next();
            b = b.next();
        }
        return a == null && b == null;
    }

    public static <X,N extends Queue<X,N>> N asQueue(Seq<X> str,Supplier<N> make) {
        return asQueue(str,0,str.size(),make);
    }
    public static <X,N extends Queue<X,N>> N asQueue(Seq<X> str, int fromInclusive, int toExclusive,Supplier<N> make) {
        N q = null;
        assert fromInclusive <= str.size():fromInclusive+" "+str.size();
        assert 0 <= fromInclusive;
        assert fromInclusive <= toExclusive:fromInclusive+" "+toExclusive;
        assert toExclusive <= str.size();
        assert 0 <= toExclusive;
        for (int i = toExclusive - 1; i >= fromInclusive; i--) {
            N next = make.get();
            next.val( str.get(i));
            next.next(q);
            q = next;
        }
        assert !hasCycle(q);
        return q;
    }
    public static <X,N extends Queue<X,N>>  int len(N q){
        int len =0;
        while(q!=null){
            len++;
            q = q.next();
        }
        return len;
    }
}
