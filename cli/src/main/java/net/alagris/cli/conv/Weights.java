package net.alagris.cli.conv;

public class Weights {
	Integer minOutgoing;
	Integer maxOutgoing;
	Integer minIncoming;
	Integer maxIncoming;
	Integer minLoopback;
	Integer maxLoopback;
	Integer eps;

	@Override
	public String toString() {
		return "[minOutgoing=" + minOutgoing + ",maxOutgoing=" + maxOutgoing + ",minIncoming=" + minIncoming
				+ ",maxIncoming=" + maxIncoming + ",minLoopback=" + minLoopback + ",maxLoopback=" + maxLoopback
				+ ",eps=" + eps + "]";
	}

	public Weights(Weights w) {
		minOutgoing = w.minOutgoing;
		maxOutgoing = w.maxOutgoing;
		minIncoming = w.minIncoming;
		maxIncoming = w.maxIncoming;
		minLoopback = w.minLoopback;
		maxLoopback = w.maxLoopback;
		eps = w.eps;
		assertInvariants();
	}

	public Weights() {
		assertInvariants();
	}

	public void assertInvariants() {
		assert minOutgoing == null || minOutgoing <= maxOutgoing;
		assert (minOutgoing == null) == (maxOutgoing == null);
		assert minIncoming == null || minIncoming <= maxIncoming;
		assert (minIncoming == null) == (maxIncoming == null);
		assert minLoopback == null || minLoopback <= maxLoopback;
		assert (minLoopback == null) == (maxLoopback == null);
	}

	public Integer maxOutgoingOrEps() {
		return max(maxOutgoing, eps);
	}

	public Integer minOutgoingOrEps() {
		return min(minOutgoing, eps);
	}

	public Integer maxIncomingOrLoopback() {
		return max(maxIncoming, maxLoopback);
	}

	public Integer minIncomingOrLoopback() {
		return min(minIncoming, minLoopback);
	}

	public Weights union(Weights rhs) {
		assert this != rhs;
		minOutgoing = min(minOutgoing, rhs.minOutgoing);
		maxOutgoing = max(maxOutgoing, rhs.maxOutgoing);
		minIncoming = min(minIncoming, rhs.minIncoming);
		maxIncoming = max(maxIncoming, rhs.maxIncoming);
		minLoopback = min(minLoopback, rhs.minLoopback);
		maxLoopback = max(maxLoopback, rhs.maxLoopback);
		assert eps == null || !eps.equals(rhs.eps);
		eps = max(eps, rhs.eps);
		return this;
	}

	public Weights concat(Weights rhs) {
		assert this != rhs;
		if (eps != null) {
			minLoopback = min(min(minLoopback, rhs.minLoopback), plus(minOutgoing, rhs.minIncoming));
			maxLoopback = max(max(maxLoopback, rhs.maxLoopback), plus(maxOutgoing, rhs.maxIncoming));
		}
		maxOutgoing = max(rhs.maxOutgoing, plus(maxOutgoing, rhs.eps));
		minOutgoing = min(rhs.minOutgoing, plus(minOutgoing, rhs.eps));
		minIncoming = min(minIncoming, plus(rhs.minIncoming, eps));
		maxIncoming = max(maxIncoming, plus(rhs.maxIncoming, eps));
		eps = plus(eps, rhs.eps);
		return this;
	}

	public Weights kleene() {
		eps = Kolmogorov.SPECS.weightNeutralElement();
		minLoopback = min(minLoopback, plus(minIncoming, minOutgoing));
		maxLoopback = max(maxLoopback, plus(maxIncoming, maxOutgoing));
		return this;
	}

	public Weights kleeneOneOrMore() {
		if (eps != null)
			eps = Kolmogorov.SPECS.weightNeutralElement();
		minLoopback = min(minLoopback, plus(minIncoming, minOutgoing));
		maxLoopback = max(maxLoopback, plus(maxIncoming, maxOutgoing));
		return this;
	}

	public Weights kleeneOptional() {
		if (eps == null)
			eps = Kolmogorov.SPECS.weightNeutralElement();
		return this;
	}

	public Weights addPost(int diff) {
		if (minOutgoing != null)
			minOutgoing = minOutgoing + diff;
		if (maxOutgoing != null)
			maxOutgoing = maxOutgoing + diff;
		if (eps != null)
			eps = eps + diff;
		return this;
	}

	public Weights addPre(int diff) {
		if (minIncoming != null)
			minIncoming = minIncoming + diff;
		if (maxIncoming != null)
			maxIncoming = maxIncoming + diff;
		if (eps != null)
			eps = eps + diff;
		return this;
	}

	public boolean outgoingWinsOverOutgoing(Weights rhs) {
		final Integer l = maxOutgoingOrEps();
		if (l == null)
			return true;
		final Integer r = rhs.minOutgoingOrEps();
		if (r == null)
			return true;
		return l > r;
	}

	/**
	 * How much needs to be added to rhs in order to make it always win over lhs. It
	 * always holds that <code>
	 * rhs.add(lhs.diff(rhs)); <br>
	 * rhs.winsOver(lhs);
	 * </code> <br/>
	 * It's analogical to arithmetic <br/>
	 * <tt>rhs + (lhs - rhs + 1) > lhs</tt> <br>
	 * This diff should be used to infer post-weights of rhs right before performing
	 * union
	 */
	public int diffOutgoingFavourRight(Weights rhs) {
		final Integer l = maxOutgoingOrEps();
		if (l == null)
			return 0;
		final Integer r = rhs.minOutgoingOrEps();
		if (r == null)
			return 0;
		if (r <= l) {
			return l - r + 1;
			// After performing addPost you obtain
			// rhs.minOutgoingOrEps := rhs.minOutgoingOrEps + l - r + 1
			// = rhs.minOutgoingOrEps + maxOutgoingOrEps - rhs.minOutgoingOrEps + 1
			// = maxOutgoingOrEps + 1
			// Then after performing the union, the transitions outgoing from rhs
			// will completely dominate over those outgoing from lhs
		} else {
			return 0;
		}
	}

	/**
	 * How much needs to be added to rhs in order to make it always win over lhs. It
	 * always holds that <code>
	 * rhs.add(lhs.diff(rhs)); <br>
	 * rhs.winsOver(lhs);
	 * </code> <br/>
	 * It's analogical to arithmetic <br/>
	 * <tt>rhs + (lhs - rhs + 1) > lhs</tt> <br>
	 * This diff should be used to infer post-weights of rhs right before performing
	 * union
	 */
	public int diffOutgoingFavourLeft(Weights rhs) {
		final Integer l = minOutgoingOrEps();
		if (l == null)
			return 0;
		final Integer r = rhs.maxOutgoingOrEps();
		if (r == null)
			return 0;
		if (l <= r) {
			return l - r - 1;
			// After performing addPost you obtain
			// rhs.maxOutgoingOrEps := rhs.maxOutgoingOrEps + l - r - 1
			// = rhs.maxOutgoingOrEps + minOutgoingOrEps - rhs.maxOutgoingOrEps - 1
			// = minOutgoingOrEps - 1
			// Then after performing the union, the transitions outgoing from lhs
			// will completely dominate over those outgoing from rhs
		} else {
			return 0;
		}
	}

	/** Makes rhs take priority over lhs */
	public int inferUnionFavourRight(Weights rhs) {
		assert this != rhs;
		final int diff = diffOutgoingFavourRight(rhs);
		if (diff != 0) {
			rhs.addPost(diff);
		}
		assert rhs.outgoingWinsOverOutgoing(this) : this + " " + rhs + " " + diff;
		union(rhs);
		return diff;
	}

	/** Makes loopback transitions take priority over incoming transitions */
	public int inferKleeneFavourLoopback() {
		final int diff = diffIncomingAndLoopbackFavourLoopback();
		if (diff != 0) {
			addPost(diff);
		}
		assert loopbackWinsOverIncoming();
		kleene();
		return diff;
	}

	/** Makes rhs take priority over lhs */
	public int inferKleeneOneOrMoreFavourLoopback() {
		final int diff = diffIncomingAndLoopbackFavourLoopback();
		if (diff != 0) {
			addPost(diff);
		}
		assert loopbackWinsOverIncoming();
		kleeneOneOrMore();
		return diff;
	}

	/**
	 * Optional is nothing morethan union with epsilon. That is,
	 * <code>X? = (''|X)</code>. Usually for unions, the weights of right side are
	 * inferred higher than for left side. This optional inference works the same
	 * way. Notice that <br/>
	 * <code>X w ? = ('' | X w) </code> <br/>
	 * where <code>w</code> is some weight. This method returns <code>w</code>.
	 */
	public int inferKleeneOptional() {
		if (eps == null) {
			final int diff = eps().diffOutgoingFavourRight(this);
			if (diff != 0) {
				addPost(diff);
			}
			eps = Kolmogorov.SPECS.weightNeutralElement();
			return diff;
		}
		return 0;
	}

	/** Makes rhs take priority over lhs */
	public int inferConcatFavourLoopback(Weights rhs) {
		final int diff = diffConnectedAndLoopbackDiffConnectedAndEpsIncomingDiffOutgoingAndOutgoingFavourRhs(rhs);
		if (diff != 0) {
			addPost(diff);
		}
		concat(rhs);
		return diff;
	}

	/**
	 * This diff should be used to infer post-weights right before performing Kleene
	 * closure
	 */
	public int diffIncomingAndLoopbackFavourLoopback() {
		final Integer potentialNewMinLoopback = plus(minIncoming, minOutgoing);
		final Integer maxIncomingOrLoopback = max(maxIncoming,maxLoopback);
		if (maxIncomingOrLoopback == null)
			return 0;
		assert potentialNewMinLoopback != null;
		if (maxIncomingOrLoopback >= potentialNewMinLoopback) {
			return maxIncomingOrLoopback - potentialNewMinLoopback + 1;
			// after addPost you get
			// minOutgoing := minOutgoing + maxIncoming - potentialNewMinLoopback + 1
			// = minOutgoing + maxIncoming - (minIncoming + minOutgoing) + 1
			// = maxIncoming - minIncoming + 1
			// then after performing kleene closure, you get minLoopback
			// minLoopback := min(minLoopback,minIncoming+minOutgoing)
			// = min(minLoopback,minIncoming+maxIncoming - minIncoming + 1)
			// = min(minLoopback,maxIncoming + 1)
			// So the new loopback will have higher weight than the incoming transitions,
			// but there might still be some existing loopbacks with lower weights.
			// It's ok, though because the previous loopbacks would have been made higher
			// than
			// maxIncoming, if the user desired so.
		} else {
			return 0;
		}
	}

	public boolean loopbackWinsOverIncoming() {
		final Integer potentialNewMinLoopback = plus(minIncoming, minOutgoing);
		if (maxIncoming == null)
			return true;
		assert potentialNewMinLoopback != null;
		return maxIncoming < potentialNewMinLoopback;
	}

	public boolean incomingWinsOverLoopback() {
		final Integer potentialNewMaxLoopback = plus(maxIncoming, maxOutgoing);
		if (minIncoming == null)
			return true;
		assert potentialNewMaxLoopback != null;
		return potentialNewMaxLoopback < minIncoming;
	}

	/**
	 * This diff should be used to infer post-weights right before performing Kleene
	 * closure
	 */
	public int diffIncomingAndLoopbackFavourIncoming() {
		final Integer potentialNewMaxLoopback = plus(maxIncoming, maxOutgoing);
		if (minIncoming == null)
			return 0;
		assert potentialNewMaxLoopback != null;
		if (potentialNewMaxLoopback >= minIncoming) {
			return minIncoming - potentialNewMaxLoopback - 1;
			// after addPost you get
			// maxOutgoing := maxOutgoing + minIncoming - potentialNewMaxLoopback - 1
			// = maxOutgoing + minIncoming - (maxIncoming,maxOutgoing) - 1
			// = minIncoming - maxIncoming - 1
			// then after performing kleene closure, you get maxLoopback
			// maxLoopback := max(maxLoopback,maxIncoming+maxOutgoing)
			// = max(maxLoopback,maxIncoming+minIncoming - maxIncoming - 1)
			// = max(maxLoopback,minIncoming - 1)
			// So the new loopback will have lower weight than the incoming transitions,
			// but there might still be some existing loopbacks with higher weights.
			// It's ok, though because the previous loopbacks would have been made lower
			// than
			// maxIncoming, if the user desired so.
		} else {
			return 0;
		}

	}

	/**
	 * This diff should be used to infer pre-weights for rhs expression (or
	 * equivalently post-weights for lhs) right before performing concatenation
	 */
	public int diffConnectedAndLoopbackDiffConnectedAndEpsIncomingDiffOutgoingAndOutgoingFavourRhs(Weights rhs) {

		final Integer maxOutgoingIncomingConnection = plus(maxOutgoing, rhs.maxIncoming);
		final Integer maxEpsIncomingConnection = plus(eps, rhs.maxIncoming);
		final Integer maxOutgoingEpsConnection = plus(maxOutgoing, rhs.eps);
		final Integer  diffOutgoingIncomingConnectionAndLoopback = minus(rhs.minLoopback,
				plus(maxOutgoingIncomingConnection, 1));
		final Integer  diffEpsIncomingConnectionAndLoopback = minus(rhs.minLoopback, plus(maxEpsIncomingConnection, 1));
		// After adding pre-weight we obtain
		// maxOutgoing := maxOutgoing + rhs.minLoopback - maxConnection -1
		// = maxOutgoing + rhs.minLoopback - (maxOutgoing + rhs.maxIncoming) -1
		// = rhs.minLoopback - rhs.maxIncoming -1
		// Then after concatenation we obtain
		// maxOutgoing + rhs.maxIncoming
		// = rhs.minLoopback - rhs.maxIncoming -1 + rhs.maxIncoming
		// = rhs.minLoopback -1
		// An analogical equation can be provided for post-weight
		final Integer diffOutgoingEpsConnectionAndOutgoing = minus(rhs.minOutgoing, plus(maxOutgoingEpsConnection, 1));
		final Integer minDiff = min(diffOutgoingIncomingConnectionAndLoopback,
				min(diffEpsIncomingConnectionAndLoopback,diffOutgoingEpsConnectionAndOutgoing));
		if (minDiff!=null) {
			return minDiff;
		} else {
			return 0;
		}
	}

//	/**
//	 * This diff should be used to infer pre-weights for rhs expression (or
//	 * equivalently post-weights for lhs) right before performing concatenation
//	 */
//	public int diffOutgoingAndLoopbackFavourOutgoing(Weights rhs) {
//		final Integer minConnection = plus(min(eps, minOutgoing), rhs.minIncoming);
//		if (rhs.maxLoopback == null || minConnection == null) {
//			return 0;
//		}
//		if (rhs.maxLoopback >= minConnection) {
//			return rhs.maxLoopback - minConnection + 1;
//		} else {
//			return 0;
//		}
//	}

	public Weights strInit() {
		minIncoming = Kolmogorov.SPECS.weightNeutralElement();
		maxIncoming = Kolmogorov.SPECS.weightNeutralElement();
		minOutgoing = Kolmogorov.SPECS.weightNeutralElement();
		maxOutgoing = Kolmogorov.SPECS.weightNeutralElement();
		minLoopback = null;
		maxLoopback = null;
		eps = null;
		assertInvariants();
		return this;
	}

	public Weights emptyInit() {
		minIncoming = null;
		maxIncoming = null;
		minOutgoing = null;
		maxOutgoing = null;
		minLoopback = null;
		maxLoopback = null;
		eps = null;
		assertInvariants();
		return this;
	}

	public Weights epsInit() {
		minIncoming = null;
		maxIncoming = null;
		minOutgoing = null;
		maxOutgoing = null;
		minLoopback = null;
		maxLoopback = null;
		eps = Kolmogorov.SPECS.weightNeutralElement();
		assertInvariants();
		return this;
	}

	public void minMax(Weights rhs) {
		minIncoming = min(minIncoming, rhs.minIncoming);
		maxIncoming = max(maxIncoming, rhs.maxIncoming);
		minOutgoing = min(minOutgoing, rhs.minOutgoing);
		maxOutgoing = max(maxOutgoing, rhs.maxOutgoing);
		minLoopback = min(minLoopback, rhs.minLoopback);
		maxLoopback = max(maxLoopback, rhs.maxLoopback);
		eps = max(eps, rhs.eps);
	}

	public static Integer max(Integer a, Integer b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return Math.max(a, b);
	}

	public static Integer min(Integer a, Integer b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return Math.min(a, b);
	}

	public static Integer opt(Integer a) {
		if (a == null)
			return 0;
		return a;
	}

	public static Integer plus(Integer a, Integer b) {
		if (a == null || b == null)
			return null;
		return a + b;
	}

	public static Integer minus(Integer a, Integer b) {
		if (a == null || b == null)
			return null;
		return a - b;
	}

	public static Weights str() {
		return new Weights().strInit();
	}

	public static Weights empty() {
		return new Weights().emptyInit();
	}

	public static Weights eps() {
		return new Weights().epsInit();
	}
}
