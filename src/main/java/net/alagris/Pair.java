package net.alagris;

import java.util.Map;
import java.util.Objects;

public interface Pair<X,Y> extends Map.Entry<X,Y>{
	@Override
	default X getKey() {
		return l();
	}
	@Override
	default Y getValue() {
		return r();
	}
	@Override
	default Y setValue(Y value) {
		throw new UnsupportedOperationException();
	}
    X l();
    Y r();
    static IntPair of(int x,int y){
        return new IntPair(x,y);
    }
    static <X,Y> Pair<X,Y> of(final X x,final Y y){
        return new Pair<X, Y>() {
            @Override
            public X l() {
                return x;
            }

            @Override
            public Y r() {
                return y;
            }

            @Override
            public String toString() {
                return "("+x+","+y+")";
            }

            @Override
            public int hashCode() {
                return x.hashCode() ^ y.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if(obj==null)return false;
                final Pair<X,Y> o = (Pair<X,Y>) obj;
                return Objects.equals(o.r(),r()) && Objects.equals(o.l(),l());
            }
        };
    }

    class IntPair implements Pair<Integer,Integer>{
        public final int l;
        public final int r;

        public IntPair(int l, int r) {
            this.l = l;
            this.r = r;
        }
        @Override
        public Integer l() {
            return l;
        }
        @Override
        public Integer r() {
            return r;
        }
        public int getL() {
            return l;
        }

        public int getR() {
            return r;
        }

        @Override
        public boolean equals(Object o) {
            IntPair intPair = (IntPair) o;
            return l == intPair.l &&
                    r == intPair.r;
        }

        @Override
        public int hashCode() {
            return l ^ r;
        }

        @Override
        public String toString() {
            return "(" + l +
                    "," + r +
                    ")";
        }
    }
}
