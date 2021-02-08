package net.alagris;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Sequence of integers implementation
 */
public final class IntSeq implements Seq<Integer>, Comparable<IntSeq>, List<Integer> {

	public static final IntSeq Epsilon = new IntSeq();

	private final int[] arr;
	private final int endExclusive;
	private final int offset;
	public IntSeq(Seq<Integer> q) {
		arr = new int[q.size()];
		offset = 0;
		endExclusive = arr.length;
		for(int i=0;i<arr.length;i++){
			arr[i] = q.get(i);
		}
	}

	public IntSeq(IntQueue q) {
		this(IntQueue.arr(q));
	}
	public IntSeq(CharSequence s) {
		this(s.codePoints().toArray());
	}
	/**This function consumes current instance of IntSeq in the sense of linear logic.*/
	public IntSeq mapLinear(Function<Integer,Integer> f) {
		for (int j = offset; j < endExclusive; j++) {
			arr[j] = f.apply(arr[j]);
		}
		return this;
	}
	public IntSeq map(Function<Integer,Integer> f) {
		int[] out = new int[size()];
		for (int j = offset,i=0; j < endExclusive; i++,j++) {
			out[i] = f.apply(arr[j]);
		}
		return new IntSeq(out);
	}
	public IntSeq(int... arr) {
		this(arr, 0, arr.length);
	}
	public IntSeq(int beginInclusive, int endExclusive, int[] arr) {
		this.arr = arr;
		this.offset = beginInclusive;
		this.endExclusive = endExclusive;
		assert offset <= arr.length : offset + " <= " + arr.length;
		assert endExclusive <= arr.length : endExclusive + " <= " + arr.length;
		assert this.endExclusive <= arr.length : this.endExclusive + " <= " + arr.length;
		assert 0 <= offset;
		assert endExclusive <= arr.length;
		assert 0 <= endExclusive;
	}
	public IntSeq(int[] arr, int offset, int size) {
		this.arr = arr;
		this.offset = offset;
		this.endExclusive = offset + size;
		assert offset <= arr.length : offset + " <= " + arr.length;
		assert offset + size <= arr.length : (offset + size) + " <= " + arr.length;
		assert endExclusive <= arr.length : endExclusive + " <= " + arr.length;
		assert 0 <= offset;
		assert endExclusive <= arr.length;
		assert 0 <= endExclusive;
	}

	public static IntSeq rand(int lenFromInclusive,int lenToExclusive,int minIntInclusive,int maxIntExclusive,Random rnd) {
		return rand(rnd.nextInt(lenToExclusive-lenFromInclusive)+lenFromInclusive,minIntInclusive,maxIntExclusive,rnd);
	}
    public static IntSeq rand(int len,int minIntInclusive,int maxIntExclusive,Random rnd) {
		assert minIntInclusive<maxIntExclusive:minIntInclusive+"<"+maxIntExclusive;
		final int[] arr = new int[len];
		for(int i=0;i<arr.length;i++){

			arr[i]=minIntInclusive+rnd.nextInt(maxIntExclusive-minIntInclusive);
			assert minIntInclusive<=arr[i]&&arr[i]<maxIntExclusive:minIntInclusive+" <= "+arr[i]+" < "+maxIntExclusive;
		}
 		return new IntSeq(arr);
    }
	public static void insertCodepointRange(StringBuilder sb,int index, int fromInclusive, int toInclusive) {
		if (fromInclusive == toInclusive) {
			sb.insert(index,"<["+fromInclusive+"]>");
		} else {
			sb.insert(index,"<["+fromInclusive+"-"+toInclusive+"]>");
		}
	}
	public static void appendCodepointRange(StringBuilder sb, int fromInclusive, int toInclusive) {
		if (fromInclusive == toInclusive) {
			sb.append("<[");
			sb.append(fromInclusive);
			sb.append("]>");
		} else {
			sb.append("<[");
			sb.append(fromInclusive);
			sb.append("-");
			sb.append(toInclusive);
			sb.append("]>");
		}
	}
	public static void appendRange(StringBuilder sb, int fromInclusive, int toInclusive) {
		if (isPrintableChar(fromInclusive) && isPrintableChar(toInclusive)) {
			if (fromInclusive == toInclusive) {
				sb.append("'");
				IntSeq.appendPrintableChar(sb, fromInclusive);
				sb.append("'");
			} else {
				sb.append("[");
				IntSeq.appendPrintableCharInsideRange(sb, fromInclusive);
				sb.append("-");
				IntSeq.appendPrintableCharInsideRange(sb, toInclusive);
				sb.append("]");
			}
		} else {
			appendCodepointRange(sb,fromInclusive,toInclusive);
		}
	}

	public static void appendRangeNoBrackets(StringBuilder sb, int fromInclusive, int toInclusive) {
		if (isPrintableChar(fromInclusive) && isPrintableChar(toInclusive)) {
			if (fromInclusive == toInclusive) {
				IntSeq.appendPrintableCharInsideRange(sb, fromInclusive);
			} else {
				IntSeq.appendPrintableCharInsideRange(sb, fromInclusive);
				sb.append("-");
				IntSeq.appendPrintableCharInsideRange(sb, toInclusive);
			}
		} else {
			appendCodepointRange(sb,fromInclusive,toInclusive);
		}
	}

	@Override
	public int size() {
		return endExclusive - offset;
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) > -1;
	}

	@Override
	public Integer get(int i) {
		return at(i);
	}

	public int at(int i) {
		return arr[offset + i];
	}

	private int hash = 0;

	@Override
	public int hashCode() {
		if (hash == 0) {
			int result = 1;
			for (int i = offset; i < endExclusive; i++)
				result = 31 * result + arr[i];
			hash = result;
		}
		return hash;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
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
		return indexOf(0,(int) o);
	}
	public int indexOf(int offset, int j) {
		int i = offset+this.offset;
		while (i < size())
			if (arr[i] == j)
				return i - this.offset;
			else
				i++;
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		int j = (int) o;
		int i = endExclusive;
		while (--i >= 0)
			if (arr[i] == j)
				return i - offset;
		return -1;
	}

	@Override
	public ListIterator<Integer> listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<Integer> listIterator(int index) {
		return new ListIterator<Integer>() {
			int i = index + offset;

			@Override
			public boolean hasNext() {
				return i < endExclusive;
			}

			@Override
			public Integer next() {
				return arr[i++];
			}

			@Override
			public boolean hasPrevious() {
				return i > offset;
			}

			@Override
			public Integer previous() {
				return arr[i--];
			}

			@Override
			public int nextIndex() {
				return i + 1;
			}

			@Override
			public int previousIndex() {
				return i - 1;
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
		final int from = fromIndex + offset;
		final int to = offset + toIndex;
		assert to <= endExclusive;
		assert from < endExclusive;
		return new IntSeq(arr, from, to);
	}

	@Override
	public Spliterator<Integer> spliterator() {
		return subList(0, size()).spliterator();
	}

	@Override
	public Stream<Integer> stream() {
		return Arrays.stream(arr, offset, endExclusive).boxed();
	}

	@Override
	public Stream<Integer> parallelStream() {
		return subList(0, size()).parallelStream();
	}

	public IntSeq concat(IntSeq rhs) {
		int[] n = new int[size() + rhs.size()];
		System.arraycopy(arr, offset, n, 0, size());
		System.arraycopy(rhs.arr, rhs.offset, n, size(), rhs.size());
		return new IntSeq(n);
	}

	@Override
	public boolean equals(Object obj) {
		IntSeq rhs = (IntSeq) obj;
		if (rhs.size() != size())
			return false;
		for (int i = 0; i < size(); i++) {
			if (at(i) != rhs.at(i))
				return false;
		}
		return true;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			int i = offset;

			@Override
			public boolean hasNext() {
				return i < endExclusive;
			}

			@Override
			public Integer next() {
				return arr[i++];
			}
		};
	}

	public Iterator<Integer> iter(int offset) {
		return new Iterator<Integer>() {
			int i = IntSeq.this.offset+offset;

			@Override
			public boolean hasNext() {
				return i < endExclusive;
			}

			@Override
			public Integer next() {
				return arr[i++];
			}
		};
	}

	@Override
	public void forEach(Consumer<? super Integer> action) {
		for (int i = offset; i < endExclusive; i++)
			action.accept(arr[i]);
	}

	@Override
	public Object[] toArray() {
		return Arrays.stream(arr, offset, endExclusive).boxed().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		Integer[] e = new Integer[size()];
		for (int i = offset; i < endExclusive; i++)
			e[i] = arr[i];
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
		Collection<Integer> ic = (Collection<Integer>) c;
		for (int i : ic) {
			if (!contains(i))
				return false;
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
        return toStringLiteral(this);
    }

    public static String toUnicodeString(Seq<Integer> seq) {
		final StringBuilder sb = new StringBuilder();
		for(int i=0;i<seq.size();i++){
			sb.appendCodePoint(seq.get(i));
		}
        return sb.toString();
    }

    /**Prints array of individual codepoints written in decimal notation.
	 * It's equivalent to Arrays.toString(seq.toArray())*/
    public static String toCodepointString(Seq<Integer> seq){
        if (seq.isEmpty()) return "[]";
		StringBuilder b = new StringBuilder("[");
		b.append(seq.get(0));
		for (int i = 1; i < seq.size(); i++) {
			b.append(", ").append(seq.get(i));
		}
		b.append("]");
		return b.toString();
	}
	@Override
	public int compareTo(IntSeq other) {
		int len1 = size();
		int len2 = other.size();
		int lim = Math.min(len1, len2);
		for (int k = 0; k < lim; k++) {
			int c1 = at(k);
			int c2 = other.at(k);
			if (c1 != c2) {
				return c1 - c2;
			}
		}
		return len1 - len2;
	}

	public int lexLenCompareTo(IntSeq other) {
		int len1 = size();
		int len2 = other.size();
		if (len1 < len2)
			return -1;
		if (len1 > len2)
			return 1;
		for (int k = 0; k < len1; k++) {
			int c1 = at(k);
			int c2 = other.at(k);
			if (c1 != c2) {
				return c1 - c2;
			}
		}
		return 0;
	}



	public Iterator<Integer> iteratorReversed() {
		return new Iterator<Integer>() {
			int i = size();

			@Override
			public boolean hasNext() {
				return i > 0;
			}

			@Override
			public Integer next() {
				return arr[--i];
			}
		};
	}

	public int lcp(IntSeq second) {
		final int minLen = Math.min(size(), second.size());
		int lcp = 0;
		while (lcp < minLen && at(lcp) == second.at(lcp)) {
			lcp++;
		}
		return lcp;
	}

	public IntSeq sub(int fromInclusive) {
		return new IntSeq(arr, offset + fromInclusive, size() - fromInclusive);
	}
	@Override
	public IntSeq sub(int fromInclusive, int endExclusive) {
		return new IntSeq( offset + fromInclusive, offset + endExclusive, arr);
	}

	public static boolean isPrintableChar(int c) {
		if(c == '\n' || c == '\t' ||c=='\r'|| c == '\0' || c == ' ' || c == '\b') return true;
		try {
			Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
			return (!Character.isISOControl(c)) && c != KeyEvent.CHAR_UNDEFINED && block != null
					&& block != Character.UnicodeBlock.SPECIALS
					&& block != Character.UnicodeBlock.SUPPLEMENTARY_PRIVATE_USE_AREA_A
					&& block != Character.UnicodeBlock.SUPPLEMENTARY_PRIVATE_USE_AREA_B;
		}catch (IllegalArgumentException e) {
			return false;
		}
	}
	public static StringBuilder appendPrintableCharInsideRange(StringBuilder sb, int c) {
		switch (c) {
			case '[':
				return sb.append("\\[");
			case ']':
				return sb.append("\\]");
			case '\\':
				return sb.append("\\\\");
			case '-':
				return sb.append("\\-");
			default:
				return appendPrintableChar(sb,c);
		}
	}
	public static StringBuilder appendPrintableChar(StringBuilder sb, int c) {
		switch (c) {
		case '\n':
			return sb.append("\\n");
		case '\r':
			return sb.append("\\r");
		case '\'':
			return sb.append("\\'");
		case '\t':
			return sb.append("\\t");
		case '\b':
			return sb.append("\\b");
		case '\0':
			return sb.append("\\0");
		default:
			return sb.appendCodePoint(c);
		}
	}

	public static String toStringLiteral(Seq<Integer> seq) {
		if(seq.isEmpty())return "''";
		boolean isPrintable = true;
		for (int i = 0; i < seq.size(); i++) {
			if (!isPrintableChar(seq.get(i))) {
				isPrintable = false;
				break;
			}
		}
		final StringBuilder sb = new StringBuilder();
		if (isPrintable) {
			sb.append("'");
			for (int i = 0; i < seq.size(); i++) {
				appendPrintableChar(sb, seq.get(i));
			}
			sb.append("'");
		}else {
			sb.append("<");
			sb.append(seq.get(0));
			for (int i = 1; i < seq.size(); i++) {
				sb.append(' ').append(seq.get(i));
			}
			sb.append(">");
		}
		return sb.toString();
	}

    public boolean isPrefixOf(int offsetBoth, IntSeq other) {
		assert offsetBoth<=size();
		assert offsetBoth>=0;
		if(size()>other.size())return false;
		for(int i=offsetBoth;i<size();i++){
			if(at(i)!=other.at(i))return false;
		}
		return true;
    }


	public boolean isSuffixOf(IntSeq other) {
		if(size()>other.size())return false;
		for(int i=0;i<size();i++){
			if(at(i)!=other.at(other.size()-size()+i))return false;
		}
		return true;
	}

	public IntSeq pow(int power) {
		assert power>=0;
		final int len = size();
		final int lenpow = len*power;
		final int[] arr = new int[lenpow];
		for(int i=offset,j=0;i<endExclusive;i++,j++) {
			for(int k=0;k<lenpow;k+=power) {
				arr[j+k]=this.arr[i];
			}
		}
		return new IntSeq(arr);
	}

	/**concatenation is a multiplicative monoid, null is multiplicative zero*/
	public static IntSeq concatOpt(IntSeq a, IntSeq b) {
		return a==null||b==null?null:a.concat(b);
	}
    /**Use it only if you are sure that this IntSeq is not used anywhere else.*/
	public int[] unsafe() {
		return arr;
	}

    public IntSeq copy() {
		return new IntSeq(Arrays.copyOfRange(arr,offset,endExclusive));
    }



}
