package net.alagris;

import java.util.Map;
import java.util.Objects;

/**
 * Edge and vertex pair
 */
public interface EN<N, E> {

    public static class Pair<N, E> implements EN<N, E> {
        private final N vertex;
        private final E edge;

        public Pair(N vertex, E edge) {
            this.vertex = vertex;
            this.edge = edge;
        }

        @Override
        public E getEdge() {
            return edge;
        }

        @Override
        public N getVertex() {
            return vertex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(vertex, pair.vertex) &&
                    Objects.equals(edge, pair.edge);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vertex, edge);
        }
    }

    static <E, N> EN<N, E> ofMap(Map.Entry<N, E> e) {
        return of(e.getKey(), e.getValue());
    }

    static <E, N> EN<N, E> ofRev(Map.Entry<E, N> e) {
        return of(e.getValue(), e.getKey());
    }

    static <E, N> EN<N, E> of(N n, E e) {
        return new Pair<>(n, e);
    }

    E getEdge();

    N getVertex();
}
