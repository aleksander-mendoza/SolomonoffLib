package net.alagris.learn;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.swing.plaf.metal.MetalIconFactory.FolderIcon16;

import net.alagris.learn.GraphNFA.Edge;
import net.alagris.learn.NFA.Targets;

public class __ {

    /** Function taking X and returning Y. This function should be total */
    public interface F2<X, Y> {
        Y f(X x);
    }

    /** Function taking (X,Y) and returning Z. This function should be total */
    public interface F3<X, Y, Z> extends F2<X, F2<Y, Z>> {
        Z f(X x, Y y);

        @Override
        default F2<Y, Z> f(X x) {
            return y -> f(x, y);
        }
    }

    /** Function taking (X,Y,Z) and returning W. This function should be total */
    public interface F4<X, Y, Z, W> extends F2<X, F3<Y, Z, W>> {
        W f(X x, Y y, Z z);

        @Override
        default F3<Y, Z, W> f(X x) {
            return (y, z) -> f(x, y, z);
        }
    }

    /** Function taking (X,Y,Z,W) and returning V. This function should be total */
    public interface F5<X, Y, Z, W, V> extends F2<X, F4<Y, Z, W, V>> {
        V f(X x, Y y, Z z, W w);

        @Override
        default F4<Y, Z, W, V> f(X x) {
            return (y, z, w) -> f(x, y, z, w);
        }
    }

    /** Mutable variable */
    public static final class V1<A> {
        public A v;

        public V1(A v) {
            this.v = v;
        }

        public static <Y> V1<Y> of(Y y) {
            return new V1<>(y);
        }
    }

    /** Mutable variable */
    public static final class V2<A, B> implements P2<A, B> {
        public A a;
        public B b;

        public V2(A a, B b) {
            this.a = a;
            this.b = b;
        }

        public static <A> V2<A, A> swap(V2<A, A> p) {
            A tmp = p.a;
            p.a = p.b;
            p.b = tmp;
            return p;
        }

        public static <X, Y> V2<X, Y> of(X x, Y y) {
            return new V2<>(x, y);
        }

        @Override
        public A a() {
            return a;
        }

        @Override
        public B b() {
            return b;
        }
    }

    /** Alternative (coproduct) type */
    interface A2<X, Y> {
        <Z> Z match(F2<X, Z> left, F2<Y, Z> right);
    }

    /** Left alternative constructor */
    public static <X, Y> A2<X, Y> l(X x) {
        return new A2<X, Y>() {
            @Override
            public <Z> Z match(F2<X, Z> left, F2<Y, Z> right) {
                return left.f(x);
            }
        };
    }

    /** Right alternative constructor */
    public static <X, Y> A2<X, Y> r(Y y) {
        return new A2<X, Y>() {
            @Override
            public <Z> Z match(F2<X, Z> left, F2<Y, Z> right) {
                return right.f(y);
            }
        };
    }

    /** Pair (product) type */
    interface P2<A, B> {
        A a();

        B b();

        public static <A, B> P2<B, A> swap(P2<A, B> p) {
            return of(p.b(), p.a());
        }

        public static <A, B> int hash(A a, B b) {
            return (a == null ? 0 : a.hashCode()) ^ (b == null ? 0 : b.hashCode());
        }

        public static <A, B> boolean eqHeretogenous(Object p1A, Object p1B, Object p2A, Object p2B) {
            return Objects.equals(p2A, p1A) && Objects.equals(p2B, p1B);
        }

        public static <A, B> boolean eqHeretogenous(P2<?, ?> p1, P2<?, ?> p2) {
            return eqHeretogenous(p1.a(), p1.b(), p2.a(), p2.b());
        }

        public static <A, B> boolean eq(P2<A, B> p1, P2<A, B> p2) {
            return eqHeretogenous(p1, p2);
        }

        public static <A, B> P2<A, B> of(Entry<A, B> e) {
            return of(e.getKey(), e.getValue());
        }

        public static <A, B> P2<A, B> of(A a, B b) {
            return new P2<A, B>() {
                @Override
                public A a() {
                    return a;
                }

                @Override
                public B b() {
                    return b;
                }

                @Override
                public int hashCode() {
                    return hash(a, b);
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj instanceof P2) {
                        return eqHeretogenous(this, (P2<?, ?>) obj);
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return "(" + a() + ";" + b() + ")";
                }
            };
        }

        /***/
        public static <A, B> P2<A, B> unordered(A a, B b) {
            return new P2<A, B>() {
                @Override
                public A a() {
                    return a;
                }

                @Override
                public B b() {
                    return b;
                }

                @Override
                public int hashCode() {
                    return hash(a, b);
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj instanceof P2) {
                        P2<?, ?> p2 = (P2<?, ?>) obj;
                        return eqHeretogenous(a(), b(), p2.a(), p2.b()) || eqHeretogenous(b(), a(), p2.a(), p2.b());
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return "{" + a() + "," + b() + "}";
                }

            };
        }
    }

    /** Triple (this is not the same as nested product!) type */
    interface P3<A, B, C> {
        A a();

        B b();

        C c();

        public static <A, B, C> int hash(A a, B b, C c) {
            return P2.hash(a, b) ^ (c == null ? 0 : c.hashCode());
        }

        public static boolean eqHeretogenous(Object p1A, Object p1B, Object p1C, Object p2A, Object p2B, Object p2C) {
            return Objects.equals(p2A, p1A) && Objects.equals(p2B, p1B) && Objects.equals(p2C, p1C);
        }

        public static boolean eqHeretogenous(P3<?, ?, ?> p1, P3<?, ?, ?> p2) {
            return eqHeretogenous(p1.a(), p1.b(), p1.c(), p2.a(), p2.b(), p2.c());
        }

        public static <A, B, C> boolean eq(P3<A, B, C> p1, P3<A, B, C> p2) {
            return eqHeretogenous(p1, p2);
        }

        public static <A, B, C> P3<A, B, C> of(A a, B b, C c) {
            return new P3<A, B, C>() {
                @Override
                public A a() {
                    return a;
                }

                @Override
                public B b() {
                    return b;
                }

                @Override
                public C c() {
                    return c;
                }

                @Override
                public int hashCode() {
                    return hash(a, b, c);
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj instanceof P3) {
                        return eqHeretogenous(this, (P3<?, ?, ?>) obj);
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return "(" + a() + ";" + b() + ";" + c + ")";
                }
            };
        }

        /***/
        public static <A, B, C> P3<A, B, C> unordered(A a, B b, C c) {
            return new P3<A, B, C>() {
                @Override
                public A a() {
                    return a;
                }

                @Override
                public B b() {
                    return b;
                }

                @Override
                public C c() {
                    return c;
                }

                @Override
                public int hashCode() {
                    return hash(a, b, c);
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj instanceof P3) {
                        P3<?, ?, ?> p2 = (P3<?, ?, ?>) obj;
                        return eqHeretogenous(a(), b(), c(), p2.a(), p2.b(), p2.c())
                                || eqHeretogenous(b(), a(), c(), p2.a(), p2.b(), p2.c());
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return "({" + a() + "," + b() + "};" + c + ")";
                }

            };
        }
    }

    /** Function composition */
    public static <X, Y, Z> F2<X, Z> o(F2<X, Y> f1, F2<Y, Z> f2) {
        return x -> f2.f(f1.f(x));
    }

    /**
     * Sequence (Function from [0...<size] to Alphabet). Also known as strings or
     * free monoids
     */
    public interface S<Alphabet> extends F2<Integer, Alphabet> {
        int size();

        public static <Alphabet> S<Alphabet> seq(int size, F2<Integer, Alphabet> f) {
            return new S<Alphabet>() {
                @Override
                public Alphabet f(Integer x) {
                    return f.f(x);
                }

                @Override
                public int size() {
                    return size;
                }

                @SuppressWarnings("unchecked")
                @Override
                public boolean equals(Object obj) {
                    return equal(this, (S<Alphabet>) obj);
                }

                int hash = 0;

                @Override
                public int hashCode() {

                    int h = hash;
                    if (h == 0 && size > 0) {
                        for (int i = 0; i < size; i++) {
                            h = 31 * h + Objects.hashCode(f.f(i));
                        }
                        hash = h;
                    }
                    return h;
                }

            };
        }

        public static <X> boolean equal(S<X> s1, S<X> s2) {
            if (s1.size() == s2.size()) {

            }
            return false;
        }

        public static <X, Y> S<Y> map(S<X> s, F2<X, Y> f) {
            return seq(s.size(), o(s::f, f));
        }

        public static S<Character> str(String s) {
            return seq(s.length(), s::charAt);
        }

        public static <X> S<X> empty() {
            return repeat(0, null);
        }

        public static <X> S<X> repeat(int size, X x) {
            return seq(size, i -> x);
        }

        static <X> S<X> subLazy(S<X> s, int beginInclusive, int endExclusive) {
            return subLazyOffset(s, beginInclusive, endExclusive - beginInclusive);
        }

        static <X> S<X> subLazyOffset(S<X> s, int offset, int length) {
            return seq(length, i -> s.f(i + offset));
        }

        static <X> S<X> subLazyOffsetEnd(S<X> s, int offsetFromEnd, int length) {
            return subLazyOffset(s, s.size() - offsetFromEnd - length, length);
        }

        static <X> S<X> leftLazy(S<X> s, int length) {
            return subLazyOffset(s, 0, length);
        }

        static <X> S<X> rightLazy(S<X> s, int length) {
            return subLazyOffsetEnd(s, 0, length);
        }

        @SuppressWarnings("unchecked")
        static <X> S<X> copy(S<X> s) {
            Object[] arr = new Object[s.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = s.f(i);
            }
            return seq(arr.length, i -> (X) arr[i % arr.length]);
        }

        static <X> S<X> arr(X[] s) {
            return arrLen(s, s.length);
        }

        static <X> S<X> arrLen(X[] s, int length) {
            return seq(length, i -> s[i % length]);
        }

        static S<Integer> arrInt(int[] s) {
            return arrIntLen(s, s.length);
        }

        static S<Integer> arrIntLen(int[] s, int length) {
            return seq(length, i -> s[i % length]);
        }

        static <X> S<X> setLazy(S<X> s, int i, X elem) {
            return seq(s.size(), j -> i == j ? elem : s.f(j));
        }

        static <X> S<X> arrList(ArrayList<X> a) {
            return seq(a.size(), a::get);
        }

        static <X, Y> U foreach(S<X> s, F2<X, Y> f) {
            for (int i = 0; i < s.size(); i++)
                f.f(s.f(i));
            return U;
        }

        static <X, Y> U foreachenum(S<X> s, F3<Integer, X, Y> f) {
            for (int i = 0; i < s.size(); i++)
                f.f(i, s.f(i));
            return U;
        }

        static <X, Y> Y fold(Y init, S<X> s, F3<Y, X, Y> f) {
            for (int i = 0; i < s.size(); i++)
                init = f.f(init, s.f(i));
            return init;
        }
    }

    /** Both S and W (read-write sequence) */
    public interface WS<Alphabet> extends S<Alphabet>, W<Alphabet> {
        public static <Alphabet> WS<Alphabet> seq(int size, F2<Integer, Alphabet> f, F3<Integer, Alphabet, U> set) {
            return new WS<Alphabet>() {
                @Override
                public W<Alphabet> set(int i, Alphabet a) {
                    set.f(i, a);
                    return this;
                }

                @Override
                public Alphabet f(Integer x) {
                    return f.f(x);
                }

                @Override
                public int size() {
                    return size;
                }

            };
        }

        static <X> WS<X> arr(X[] s) {
            return seq(s.length, i -> s[i % s.length], (i, a) -> u(s[i % s.length] = a));
        }

    }

    /**
     * Recursive (decidable) set is defined by it's total characteristic function
     */
    public interface Set<E> extends F2<E, Boolean> {
    }

    /** Optional type (coproduct of unit type and some other type) */
    public interface O<E> {
        E force();

        default boolean isSome() {
            return false;
        }

        default <E2> O<E2> map(F2<E, E2> f) {
            return none();
        }

        public static <E> O<E> idem(O<O<E>> o) {
            return o.isSome() ? o.force() : none();
        }

        default <E2> E2 match(F2<E, E2> f, F2<U, E2> ifNone) {
            return ifNone.f(U);
        }
    }

    public static U U = null;

    public static <E> E trap(RuntimeException t) {
        throw t;
    }

    public static <E> O<E> none() {
        return () -> trap(new NoSuchElementException());
    }

    public static <E> O<E> some(E e) {
        return new O<E>() {
            @Override
            public E force() {
                return e;
            }

            @Override
            public boolean isSome() {
                return true;
            }

            @Override
            public <E2> O<E2> map(F2<E, E2> f) {
                return some(f.f(e));
            }

            @Override
            public <E2> E2 match(F2<E, E2> f, F2<U, E2> ifNone) {
                return f.f(e);
            }
        };
    }

    /** Constant function */
    public static <X, Y> F2<X, Y> c(Y y) {
        return x -> y;
    }

    /** Iterator (impure function with side-effects from unit to optional) */
    public interface I<E> {
        O<E> n();

        public static <Y, E> Y fold(Y y, I<E> i, F3<Y, E, Y> f) {
            V1<Y> v = V1.of(y);
            foreach(i, n -> v.v = f.f(v.v, n));
            return v.v;
        }

        public static <Y, E> Y pop(I<E> i, F2<E, Y> f, F2<U, Y> ifEmpty) {
            return i.n().match(f, ifEmpty);
        }

        public static <Y, E, E2> I<Y> zip(I<E> i, I<E2> i2, F3<E, E2, Y> f) {
            return () -> O.idem(i.n().map(e -> i2.n().map(e2 -> f.f(e, e2))));
        }

        public static <E, X> U foreach(Iterator<E> i, F2<E, X> f) {
            i.forEachRemaining(f::f);
            return U;
        }

        public static <E, X> U foreach(I<E> i, F2<E, X> f) {
            skipWhile(i, n -> {
                f.f(n);
                return true;
            });
            return U;
        }

        public static <E> O<E> until(I<E> i, F2<E, Boolean> pred) {
            return skipWhile(i, x -> !pred.f(x));
        }

        public static <E> O<E> until(Iterator<E> i, F2<E, Boolean> pred) {
            while (i.hasNext()) {
                E o;
                if (pred.f(o = i.next())) {
                    return some(o);
                }
            }
            return none();
        }

        public static <E> O<E> skipWhile(Iterator<E> i, F2<E, Boolean> pred) {
            return until(i, x -> !pred.f(x));
        }

        public static <E> O<E> skipWhile(I<E> i, F2<E, Boolean> pred) {
            O<E> o;
            while ((o = i.n()).match(pred, c(false))) {
            }
            return o;
        }

        public static <E> I<E> seq(S<E> seq) {
            return new I<E>() {
                int i = 0;

                @Override
                public O<E> n() {
                    return i < seq.size() ? some(seq.f(i++)) : none();
                }

            };
        }

        /** The negated version of this method is pass() */
        public static <X> I<X> filter(I<X> i, F2<X, Boolean> shouldFilterOut) {
            return () -> skipWhile(i, shouldFilterOut);
        }

        /** The negated version of this method is filter() */
        public static <X> I<X> pass(I<X> i, F2<X, Boolean> shouldPass) {
            return () -> until(i, shouldPass);
        }

        public static <X, Y> I<Y> map(I<X> i, F2<X, Y> f) {
            return () -> i.n().map(f);
        }

        /** just an alias for until() */
        public static <X> O<X> find(I<X> i, F2<X, Boolean> f) {
            return until(i, f);
        }

        static <X> O<X> find(Iterator<X> i, F2<X, Boolean> f) {
            return until(i, f);
        }

        static <X> I<X> single(X x) {
            return new I<X>() {
                boolean done = false;

                @Override
                public O<X> n() {
                    if (done) {
                        return none();
                    } else {
                        done = true;
                        return some(x);
                    }
                }
            };
        }

        static <X, Y> Iterator<Y> map(Iterator<X> i, F2<X, Y> f) {
            return new Iterator<Y>() {
                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public Y next() {
                    return f.f(i.next());
                }
            };
        }

        static <X> HashMap<X, Integer> enumerate(I<X> i) {
            return fold(new HashMap<X, Integer>(), i, (map, elem) -> {
                map.computeIfAbsent(elem, u -> map.size());
                return map;
            });
        }

        static <X> int count(I<X> i) {
            return fold(0, i, (c, e) -> c + 1);
        }

        /** The worse, over-engineered, imperative iterator */
        static <X> Iterator<X> worse(I<X> i) {
            return new Iterator<X>() {
                O<X> o = i.n();

                @Override
                public boolean hasNext() {
                    return o.isSome();
                }

                @Override
                public X next() {
                    O<X> p = o;
                    p = i.n();
                    return p.force();
                }
            };
        }

    }

    /** Unit type. The only inhabitant is null */
    public static final class U {
        private U() {
        }

        public static <E> U of(E e) {
            return null;
        }
    }

    /** Recursively enumerable set */
    public interface Enum<E> extends F2<U, I<E>> {

    }

    public static <X, Y> Y ignore(X x, Y y) {
        return y;
    }

    public static <X> U u(X x) {
        return U;
    }

    public static String lcp(String str1, String str2) {
        StringBuilder sb = new StringBuilder();
        I.skipWhile(I.zip(I.seq(S.str(str1)), I.seq(S.str(str2)), P2::of),
                p -> p.a() == p.b() ? ignore(sb.append(p.a()), true) : false);
        return sb.toString();
    }

    public static String lcpI(I<String> strings) {
        return I.pop(strings, first -> I.fold(first, strings, __::lcp), c(null));
    }

    public static <X, Y> Y let(X x, F2<X, Y> f) {
        return f.f(x);
    }

    public static <X> O<X> any(Collection<X> c) {
        Iterator<X> iter = c.iterator();
        return iter.hasNext() ? some(iter.next()) : none();
    }

    public static <X> I<X> iter(Iterable<X> x) {
        final Iterator<X> iter = x.iterator();
        return () -> iter.hasNext() ? some(iter.next()) : none();
    }

    public static <X> X id(X x) {
        return x;
    }

    public static <X> void v(X x) {
    }

    public static <X, SS extends java.util.Set<X>> SS addInPlace(SS s, X x) {
        s.add(x);
        return s;
    }

    public static <X, SS extends java.util.Set<X>> SS removeInPlace(SS s, X x) {
        s.remove(x);
        return s;
    }

    public static <X> HashSet<X> collectSet(I<X> i) {
        return I.fold(new HashSet<>(), i, __::addInPlace);
    }

    

    /**
     * Utility function for working with HashMaps and HashSets in functional style
     */
    public static final class HM {// used as namespace
        private HM() {
        }
        
        
        public static <X, A, B> HashMap<A, HashSet<B>> append(HashMap<A, HashSet<B>> map, A key, B value) {
            map.computeIfAbsent(key, x -> new HashSet<>()).add(value);
            return map;
        }

        public static <X, A, B, C> C putNested(HashMap<A, HashMap<B, C>> map, A keyA, B keyB, C value) {
            return map.computeIfAbsent(keyA, x -> new HashMap<>()).put(keyB, value);
        }

        /**
         * @param e element to be added to this set
         * @return <tt>true</tt> if this set did not already contain the specified
         *         element
         */
        public static <X, A, B> boolean appendAndCheck(HashMap<A, HashSet<B>> map, A key, B value) {
            return map.computeIfAbsent(key, x -> new HashSet<>()).add(value);
        }

        /**
         * @return <tt>true</tt> if the set contained the specified element element
         */
        public static <X, A, B> boolean removeAndCheck(HashMap<A, HashSet<B>> map, A key, B value) {
            HashSet<B> e = map.get(key);
            return e == null ? false : e.remove(value);
        }

        /**
         * @return <tt>true</tt> if the set contained the specified element element
         */
        public static <X, A, B, C> boolean removeAndCheckKey(HashMap<A, HashMap<B, C>> map, A keyA, B keyB) {
            HashMap<B, C> e = map.get(keyA);
            return e == null ? false : null != e.remove(keyB);
        }

        public static <X, A, B> HashMap<A, HashSet<B>> collectSetMap(I<X> iter, F2<X, A> key, F2<X, B> value) {
            HashMap<A, HashSet<B>> map = new HashMap<>();
            I.foreach(iter, p -> append(map, key.f(p), value.f(p)));
            return map;
        }

        public static <X, A, B> HashMap<A, B> collectMap(I<X> iter, F2<X, A> key, F2<X, B> value) {
            HashMap<A, B> map = new HashMap<>();
            I.foreach(iter, p -> map.put(key.f(p), value.f(p)));
            return map;
        }

        static <X, L extends List<X>> L listAdd(L a, X x) {
            a.add(x);
            return a;
        }

        public static <X> O<X> anyCommon(java.util.Set<X> s1, java.util.Set<X> s2) {
            for (X x1 : s1) {
                if (s2.contains(x1))
                    return some(x1);
            }
            return none();
        }
    }

    public static <X> X coalsece(X x, X otherwise) {
        return x == null ? otherwise : x;
    }

    /** Write-only sequence (mutable version of sequence/string) */
    public interface W<Alphabet> {
        int size();

        W<Alphabet> set(int i, Alphabet a);
    }

    /** Bijective function */
    public interface Iff<X, Y> extends F2<X, Y> {
        /** inverse of f(x) */
        X i(Y y);
    }

    /** The negated version of this method is passInPlace() */
    public static <X, C extends java.util.Collection<X>> C filterInPlace(C set, F2<X, Boolean> shouldFilterOut) {
        set.removeIf(shouldFilterOut::f);
        return set;
    }

    /** The negated version of this method is filterInPlace() */
    public static <X, C extends java.util.Collection<X>> C passInPlace(C set, F2<X, Boolean> shouldPass) {
        return filterInPlace(set, x -> !shouldPass.f(x));
    }

    public static <X> Iterator<X> singletonIterator(X x) {
        return new Iterator<X>() {
            boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public X next() {
                if (hasNext)
                    hasNext = false;
                else
                    throw new NoSuchElementException();

                return x;
            }
        };
    }

}
