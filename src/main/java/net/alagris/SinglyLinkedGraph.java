package net.alagris;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @param <V> State of vertex. This can be any meta information carried by each vertex.
 * @param <N> Vertices should NOT implement custom {@link Object#equals} or {@link Object#hashCode} because they are
 *            later used as keys in hash maps and distinct objects should represent distinct vertices.
 */
public interface SinglyLinkedGraph<V, E, N> {


    /**
     * Many graph algorithms need to mark vertices with colors or attach to them
     * any other ad-hoc meta information. This is what color of vertex is for. Each
     * vertex should store some Object field that can be freely changed at any moment.
     * It's not meant to be directly used by the end-user. It's only for internal use
     * in algorithms. There are no guarantees as to what is stored as color of vertex
     * at any given point. Any algorithm that requires coloring of vertices should
     * initialize them at the beginning. Moreover, using the same vertices
     * in parallel by two different algorithms may lead to data races.
     */
    Object getColor(N vertex);

    void setColor(N vertex, Object color);

    V getState(N vertex);

    void setState(N vertex, V v);

    /**
     * Returns number of edges outgoing from given vertex
     */
    public int size(N from);

    public Iterator<Map.Entry<E, N>> iterator(N from);

    /**
     * Collection of outgoing edges.
     */
    public Map<E, N> outgoing(N from);

    public void add(N from, E edge, N to);

    public boolean remove(N form, E edge, N to);

    public boolean contains(N form, E edge, N to);

    /**
     * Creates a new blank vertex
     */
    public N create(V state);

    /**
     * Copies a vertex and all it's meta-data but does not copy outgoing edges.
     * The resulting vertex should hence be of degree 0
     */
    public N shallowCopy(N other);

    /**
     * Performs deep cloning of entire graph. The graph must be homogeneous, that
     * is, all vertices must be of the same type S or its subtype that can be cast
     * to S.
     * <br/>
     * <br/>
     * Note, that while this method does indeed perform deep cloning, the edges E
     * themselves are only cloned shallowly. That's because edges are used as keys
     * of transition maps and keys by their definition should be immutable.
     * Therefore no deep cloning is necessary and if you require to deep-clone your
     * edges it's a sign that you are doing something wrong.
     */
    // @requires !cloned.contains(original);
    // @ensures cloned.contains(original);
    public static <V, E, N> N deepClone(SinglyLinkedGraph<V, E, N> g, N original, Map<N, N> cloned) {
        final N previouslyCloned = cloned.get(original);
        if (previouslyCloned != null) {// safety check in case the method precondition was violated
            return previouslyCloned;
        }
        final N clone = g.shallowCopy(original);// create new clone
        cloned.put(original, clone);
        // populate edges of clone
        for (Map.Entry<E, N> entry : (Iterable<Map.Entry<E, N>>) () -> g.iterator(original)) {
            final E edge = entry.getKey();
            final N otherConnected = entry.getValue();
            final N alreadyCloned = cloned.get(otherConnected);
            // clone targets of transitions if necessary
            if (alreadyCloned == null) {
                g.add(clone, edge, deepClone(g, otherConnected, cloned));
            } else {
                // reuse what has already been cloned.
                // This way the graph search won't loop
                // infinitely
                g.add(clone, edge, alreadyCloned);
            }
        }
        return clone;
    }

    public static <V, E, N> N deepClone(SinglyLinkedGraph<V, E, N> g, N original) {
        return deepClone(g, original, new HashMap<>());
    }

    /**
     * Implements depth-first search. Because the graph may contain loops, the
     * search cannot be done without collecting the visited vertices alongside,
     * hence this algorithm is called "collect" rather than "search". The graph must
     * be homogeneous, that is, all vertices must be of the same type G or its
     * subtype that can be cast to G.
     *
     * @param shouldContinue allows for early termination
     * @return collected set if successfully explored entire graph. Otherwise null
     * if early termination occurred
     */
    public static <V, E, N, S extends Set<N>> S collectSet(
            SinglyLinkedGraph<V, E, N> g, N startpoint, S collected, Predicate<N> shouldContinue) {
        return collect(g, startpoint, collected::add, shouldContinue) ? collected : null;
    }

    /**
     * @param collect adds state to some collection and returns true if was not collected before
     * @return true if collected everything, false if terminated early
     */
    public static <V, E, N> boolean collect(
            SinglyLinkedGraph<V, E, N> g, N startpoint, Function<N, Boolean> collect, Predicate<N> shouldContinue) {
        final boolean c = shouldContinue.test(startpoint);
        if (collect.apply(startpoint) && c) {
            for (Map.Entry<E, N> entry : (Iterable<Map.Entry<E, N>>) () -> g.iterator(startpoint)) {
                N otherConnected = entry.getValue();
                if (!collect(g, otherConnected, collect, shouldContinue)) {
                    return false;
                }
            }
        }
        return c;
    }

    public static <V, E, N> HashSet<N> collect(SinglyLinkedGraph<V, E, N> g, N startpoint) {
        return collectSet(g, startpoint, new HashSet<>(), x -> true);
    }

}
