package net.alagris.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import net.alagris.IntSeq;
import net.alagris.Pair;
import net.alagris.Pair.IntPair;
import net.alagris.ast.ThraxParser.V;

public interface Solomonoff {

	
	public static class VarMeta{
		int usagesLeft;
		final Weights weights;
		public VarMeta(Weights weights) {
			this.weights = weights;
		}
	}
	public static class Weights{
		Integer minOutgoing;
		Integer maxOutgoing;
		Integer minIncoming;
		Integer maxIncoming;
		Integer minLoopback;
		Integer maxLoopback;
		Integer eps;
		public Weights() {
			assertInvariants();
		}
		public void assertInvariants() {
			assert minOutgoing==null || minOutgoing <= maxOutgoing;
			assert (minOutgoing==null)==(maxOutgoing==null);
			assert minIncoming==null || minIncoming <= maxIncoming;
			assert (minIncoming==null)==(maxIncoming==null);
			assert minLoopback==null || minLoopback <= maxLoopback;
			assert (minLoopback==null)==(maxLoopback==null);
		}
		public Integer maxOutgoingOrEps() {
			return max(maxOutgoing,eps);
		}
		public Integer minOutgoingOrEps() {
			return min(minOutgoing,eps);
		}
		public Integer maxIncomingOrLoopback() {
			return max(maxIncoming,maxLoopback);
		}
		public Integer minIncomingOrLoopback() {
			return min(minIncoming,minLoopback);
		}
		public Weights union(Weights rhs) {
			minOutgoing = min(minOutgoing,rhs.minOutgoing);
			maxOutgoing = max(maxOutgoing,rhs.maxOutgoing);
			minIncoming = min(minIncoming,rhs.minIncoming);
			maxIncoming = max(maxIncoming,rhs.maxIncoming);
			minLoopback = min(minLoopback,rhs.minLoopback);
			maxLoopback = max(maxLoopback,rhs.maxLoopback);
			assert eps==null||!eps.equals(rhs.eps);
			eps = max(eps,rhs.eps);
			return this;
		}
		public Weights concat(Weights rhs) {
			if(eps!=null) {
				minLoopback = min(min(minLoopback,rhs.minLoopback),plus(minOutgoing,rhs.minIncoming));
				maxLoopback = max(max(maxLoopback,rhs.maxLoopback),plus(maxOutgoing,rhs.maxIncoming));
			}
			maxOutgoing = max(rhs.maxOutgoing,plus(maxOutgoing,rhs.eps));
			minOutgoing = min(rhs.minOutgoing,plus(minOutgoing,rhs.eps));
			minIncoming = min(minIncoming,plus(rhs.minIncoming,eps));
			maxIncoming = max(maxIncoming,plus(rhs.maxIncoming,eps));
			eps = plus(eps,rhs.eps);
			return this;
		}
		public Weights kleene() {
			eps = Kolmogorov.SPECS.weightNeutralElement();
			minLoopback = min(minLoopback,plus(minIncoming,minOutgoing));
			maxLoopback = max(maxLoopback,plus(maxIncoming,maxOutgoing));
			return this;
		}
		public Weights kleeneOneOrMore() {
			if(eps!=null)eps = Kolmogorov.SPECS.weightNeutralElement();
			minLoopback = min(minLoopback,plus(minIncoming,minOutgoing));
			maxLoopback = max(maxLoopback,plus(maxIncoming,maxOutgoing));
			return this;
		}
		public Weights kleeneOptional() {
			if(eps!=null)eps = Kolmogorov.SPECS.weightNeutralElement();
			return this;
		}
		public Weights addPost(int diff) {
			if(minOutgoing!=null) minOutgoing=minOutgoing+diff;
			if(maxOutgoing!=null) maxOutgoing=maxOutgoing+diff;
			if(eps!=null) eps=eps+diff;
			return this;
		}
		public Weights addPre(int diff) {
			if(minIncoming!=null) minIncoming=minIncoming+diff;
			if(maxIncoming!=null) maxIncoming=maxIncoming+diff;
			if(eps!=null) eps=eps+diff;
			return this;
		}
		public boolean winsOver(Weights rhs) {
			final Integer l = maxOutgoingOrEps();
			if(l==null)return true;
			final Integer r = rhs.minOutgoingOrEps();
			if(r==null)return true;
			return l>r;
		}
		/**How much needs to be added to rhs in order to make it always win over lhs.
		 * It always holds that 
		 * <code>
		 * rhs.add(lhs.diff(rhs)); <br>
		 * rhs.winsOver(lhs);
		 * </code> <br/>
		 * It's analogical to arithmetic <br/>
		 * <tt>rhs + (lhs - rhs + 1) > lhs</tt>
		 * <br>
		 * This diff should be used to infer post-weights of rhs right before performing union*/
		public int diffOutgoingFavourRight(Weights rhs) {
			final Integer l = maxOutgoingOrEps();
			if(l==null)return 0;
			final Integer r = rhs.minOutgoingOrEps();
			if(r==null)return 0;
			if(r <= l) {
				return l-r+1;
				//After performing addPost you obtain
				//rhs.minOutgoingOrEps := rhs.minOutgoingOrEps + l - r + 1
				// = rhs.minOutgoingOrEps + maxOutgoingOrEps - rhs.minOutgoingOrEps + 1
				// = maxOutgoingOrEps + 1
				//Then after performing the union, the transitions outgoing from rhs
				//will completely dominate over those outgoing from lhs
			}else {
				return 0;
			}
		}
		/**How much needs to be added to rhs in order to make it always win over lhs.
		 * It always holds that 
		 * <code>
		 * rhs.add(lhs.diff(rhs)); <br>
		 * rhs.winsOver(lhs);
		 * </code> <br/>
		 * It's analogical to arithmetic <br/>
		 * <tt>rhs + (lhs - rhs + 1) > lhs</tt>
		 * <br>
		 * This diff should be used to infer post-weights of rhs right before performing union*/
		public int diffOutgoingFavourLeft(Weights rhs) {
			final Integer l = minOutgoingOrEps();
			if(l==null)return 0;
			final Integer r = rhs.maxOutgoingOrEps();
			if(r==null)return 0;
			if(l <= r) {
				return l-r-1;
				//After performing addPost you obtain
				//rhs.maxOutgoingOrEps := rhs.maxOutgoingOrEps + l - r - 1
				// = rhs.maxOutgoingOrEps + minOutgoingOrEps - rhs.maxOutgoingOrEps - 1
				// = minOutgoingOrEps - 1
				//Then after performing the union, the transitions outgoing from lhs
				//will completely dominate over those outgoing from rhs 
			}else {
				return 0;
			}
		}
		/**Makes rhs take priority over lhs*/
		public int inferUnionFavourRight(Weights rhs) {
			final int diff = diffOutgoingFavourRight(rhs);
			if(diff!=0) {
				rhs.addPost(diff);
			}
			assert rhs.winsOver(this);
			union(rhs);
			return diff;
		}
		/**Makes rhs take priority over lhs*/
		public int inferKleeneFavourLoopback() {
			final int diff = diffIncomingAndLoopbackFavourLoopback();
			if(diff!=0) {
				addPost(diff);
			}
			kleene();
			return diff;
		}
		/**Makes rhs take priority over lhs*/
		public int inferKleeneOneOrMoreFavourLoopback() {
			final int diff = diffIncomingAndLoopbackFavourLoopback();
			if(diff!=0) {
				addPost(diff);
			}
			kleeneOneOrMore();
			return diff;
		}
		/**Makes rhs take priority over lhs*/
		public void inferKleeneOptional() {
			kleeneOptional();
		}
		/**Makes rhs take priority over lhs*/
		public int inferConcatFavourLoopback(Weights rhs) {
			final int diff = diffOutgoingAndLoopbackFavourLoopback(rhs);
			if(diff!=0) {
				addPost(diff);
			}
			concat(rhs);
			return diff;
		}
		/**This diff should be used to infer post-weights right before performing Kleene closure*/
		public int diffIncomingAndLoopbackFavourLoopback() {
			final Integer potentialNewMinLoopback = plus(minIncoming,minOutgoing);
			if(maxIncoming==null)return 0;
			assert potentialNewMinLoopback!=null;
			if(maxIncoming>=potentialNewMinLoopback) {
				return maxIncoming - potentialNewMinLoopback + 1;
				//after addPost you get
				//minOutgoing :=  minOutgoing + maxIncoming - potentialNewMinLoopback + 1
				// =  minOutgoing + maxIncoming - (minIncoming + minOutgoing) + 1
				// =  maxIncoming - minIncoming  + 1
				// then after performing kleene closure, you get minLoopback
				// minLoopback := min(minLoopback,minIncoming+minOutgoing) 
				// = min(minLoopback,minIncoming+maxIncoming - minIncoming  + 1)
				// = min(minLoopback,maxIncoming + 1)
				//So the new loopback will have higher weight than the incoming transitions,
				//but there might still be some existing loopbacks with lower weights.
				//It's ok, though because the previous loopbacks would have been made higher than 
				//maxIncoming, if the user desired so. 
			}else {
				return 0;
			}
		}
		/**This diff should be used to infer post-weights right before performing Kleene closure*/
		public int diffIncomingAndLoopbackFavourIncoming() {
			final Integer potentialNewMaxLoopback = plus(maxIncoming,maxOutgoing);
			if(minIncoming==null)return 0;
			assert potentialNewMaxLoopback!=null;
			if(potentialNewMaxLoopback>=minIncoming) {
				return minIncoming - potentialNewMaxLoopback - 1;
				//after addPost you get
				//maxOutgoing := maxOutgoing + minIncoming - potentialNewMaxLoopback - 1
				// =  maxOutgoing + minIncoming - (maxIncoming,maxOutgoing) - 1
				// =  minIncoming - maxIncoming  - 1
				// then after performing kleene closure, you get maxLoopback
				// maxLoopback := max(maxLoopback,maxIncoming+maxOutgoing) 
				// = max(maxLoopback,maxIncoming+minIncoming - maxIncoming  - 1) 
				// = max(maxLoopback,minIncoming - 1) 
				//So the new loopback will have lower weight than the incoming transitions,
				//but there might still be some existing loopbacks with higher weights.
				//It's ok, though because the previous loopbacks would have been made lower than 
				//maxIncoming, if the user desired so. 
			}else {
				return 0;
			}
			
		}
		/**This diff should be used to infer pre-weights for rhs expression (or equivalently post-weights for lhs) 
		 * right before performing concatenation*/
		public int diffOutgoingAndLoopbackFavourLoopback(Weights rhs) {
			final Integer maxConnection = plus(max(eps,maxOutgoing),rhs.maxIncoming);
			if(rhs.minLoopback==null||maxConnection==null) {
				return 0;
			}
			if(maxConnection>=rhs.minLoopback) {
				return rhs.minLoopback - maxConnection -1;
				//After adding pre-weight we obtain
				// maxOutgoing := maxOutgoing + rhs.minLoopback - maxConnection -1
				//  = maxOutgoing + rhs.minLoopback - (maxOutgoing + rhs.maxIncoming) -1
				//  = rhs.minLoopback - rhs.maxIncoming -1
				// Then after concatenation we obtain
				// maxOutgoing + rhs.maxIncoming 
				//   = rhs.minLoopback - rhs.maxIncoming -1 + rhs.maxIncoming
				//   = rhs.minLoopback -1
				// An analogical equation can be provided for post-weight
			}else {
				return 0;
			}
		}
		/**This diff should be used to infer pre-weights for rhs expression (or equivalently post-weights for lhs) 
		 * right before performing concatenation*/
		public int diffOutgoingAndLoopbackFavourOutgoing(Weights rhs) {
			final Integer minConnection = plus(min(eps,minOutgoing),rhs.minIncoming);
			if(rhs.maxLoopback==null||minConnection==null) {
				return 0;
			}
			if(rhs.maxLoopback >= minConnection) {
				return rhs.maxLoopback - minConnection + 1;
			}else {
				return 0;
			}
		}
		
		public Weights strInit() {
			minIncoming = null;
			maxIncoming = null;
			minOutgoing = Kolmogorov.SPECS.weightNeutralElement();
			maxOutgoing = Kolmogorov.SPECS.weightNeutralElement();
			minLoopback = Kolmogorov.SPECS.weightNeutralElement();
			maxLoopback = Kolmogorov.SPECS.weightNeutralElement();
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
			minIncoming = min(minIncoming,rhs.minIncoming);
			maxIncoming = max(maxIncoming,rhs.maxIncoming);
			minOutgoing = min(minOutgoing,rhs.minOutgoing);
			maxOutgoing = max(maxOutgoing,rhs.maxOutgoing);
			minLoopback = min(minLoopback,rhs.minLoopback);
			maxLoopback = max(maxLoopback,rhs.maxLoopback);
			eps = max(eps,rhs.eps);
		}
	}
	public static Integer max(Integer a,Integer b) {
		if(a==null)return b;
		if(b==null)return a;
		return Math.max(a,b);
	}
	public static Integer min(Integer a,Integer b) {
		if(a==null)return b;
		if(b==null)return a;
		return Math.min(a,b);
	}
	public static Integer opt(Integer a) {
		if(a==null)return 0;
		return a;
	}
	public static Integer plus(Integer a,Integer b) {
		if(a==null||b==null)return null;
		return a+b;
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
	
	public void countUsages(Consumer<String> countUsage);
	
	int precedence();
	void toString(StringBuilder sb);
	/**prints to string but with automatic inference of lexicographic weights and automatically prefixing
	 * variables with exponentials whenever copies are necessary. Returns the highest weight that was appended 
	 * to the regular expression. 
	 * @return max and min weight of any outgoing transition (according to Glushkov's construction)*/
	Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Map<String,VarMeta> usagesLeft);
	
	public static class SolUnion implements Solomonoff{
		final Solomonoff lhs,rhs;
		public SolUnion(Solomonoff lhs,Solomonoff rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
			assert !(rhs instanceof SolUnion);
		}
		@Override
		public int precedence() {
			return 0;
		}
		@Override
		public void toString(StringBuilder sb) {
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			}else {
				lhs.toString(sb);
			}
			sb.append("|");
			if(rhs.precedence()<precedence()) {
				sb.append("(");
				rhs.toString(sb);
				sb.append(")");
			}else {
				rhs.toString(sb);
			}
		}
		@Override
		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Map<String,VarMeta> usagesLeft) {
			final Weights lw;
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				lw = lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
				sb.append(")");
			} else {
				lw = lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
			}
			sb.append("|");
			final Weights rw;
			if(rhs.precedence()<precedence()) {
				sb.append("(");
				rw = rhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
				sb.append(")");
			} else {
				rw = rhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
			}
			final int diff = lw.inferUnionFavourRight(rw);
			if(diff!=0) {
				sb.append(" ").append(diff);
			}
			return lw;
		}
		@Override
		public void countUsages(Consumer<String> countUsage) {
			lhs.countUsages(countUsage);
			rhs.countUsages(countUsage);
		}
	}
	
	public static class SolConcat implements Solomonoff{
		final Solomonoff lhs,rhs;
		public SolConcat(Solomonoff lhs,Solomonoff rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
			assert !(rhs instanceof SolConcat);
		}
		@Override
		public int precedence() {
			return 1;
		}
		@Override
		public void toString(StringBuilder sb) {
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			}else {
				lhs.toString(sb);
			}
			sb.append(" ");
			if(rhs.precedence()<precedence()) {
				sb.append("(");
				rhs.toString(sb);
				sb.append(")");
			}else {
				rhs.toString(sb);
			}
		}
		@Override
		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb,  Map<String,VarMeta> usagesLeft) {
			final Weights lw;
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				lw = lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
				sb.append(")");
			}else {
				lw = lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
			}
			sb.append(" ");
			final StringBuilder rsb = new  StringBuilder();
			final Weights rw;
			if(rhs.precedence()<precedence()) {
				rsb.append("(");
				rw = rhs.toStringAutoWeightsAndAutoExponentials(rsb,usagesLeft);
				rsb.append(")");
			} else {
				rw = rhs.toStringAutoWeightsAndAutoExponentials(rsb,usagesLeft);
			}
			final int diff = lw.inferConcatFavourLoopback(rw);
			if(diff!=0) {
				sb.append(diff).append(" ");
			}
			sb.append(rsb);
			return lw;
		}
		@Override
		public void countUsages(Consumer<String> countUsage) {
			lhs.countUsages(countUsage);
			rhs.countUsages(countUsage);
		}
	}
	public static class SolProd implements Solomonoff{
		final IntSeq output;
		public SolProd(IntSeq output) {
			this.output = output;
		}
		@Override
		public int precedence() {
			return 3;
		}
		@Override
		public void toString(StringBuilder sb) {
			sb.append(":").append(output.toStringLiteral());
		}
		@Override
		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Map<String,VarMeta> usagesLeft) {
			sb.append(":").append(output.toStringLiteral());
			return eps();
		}
		@Override
		public void countUsages(Consumer<String> countUsage) {
		}
	}
	public static class SolKleene implements Solomonoff{
		final Solomonoff lhs;
		final char type;
		public SolKleene(Solomonoff lhs, char type) {
			this.lhs = lhs;
			this.type = type;
		}
		@Override
		public int precedence() {
			return 2;
		}
		@Override
		public void toString(StringBuilder sb) {
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			}else {
				lhs.toString(sb);
			}
			sb.append(type);
		}
		@Override
		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Map<String,VarMeta> usagesLeft) {
			final Weights w; 
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				w = lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
				sb.append(")");
			} else {
				w = lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
			}
			if(type=='*') {
				final int diff = w.inferKleeneFavourLoopback();
				if(diff!=0) {
					sb.append(diff);
				}
				sb.append('*');
			} else if(type =='+') {
				final int diff = w.inferKleeneOneOrMoreFavourLoopback();
				if(diff!=0) {
					sb.append(diff);
				}
				sb.append('+');
			} else if(type=='?') {
				w.inferKleeneOptional();
				sb.append('?');
			}
			return w;
		}
		@Override
		public void countUsages(Consumer<String> countUsage) {
			lhs.countUsages(countUsage);
		}
	}
	
	public static class SolFunc implements Solomonoff{
		final Solomonoff[] args;
		final String id;
		public SolFunc(Solomonoff[] args,String id) {
			this.args = args;
			this.id = id;
		}
		@Override
		public int precedence() {
			return 3;
		}
		@Override
		public void toString(StringBuilder sb) {
			sb.append(id).append("[");
			if(args.length>0) {
				args[0].toString(sb);
				for(int i=1;i<args.length;i++) {
					sb.append(",");
					args[1].toString(sb);
				}
			}
			sb.append("]");
		}
		@Override
		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb,
				Map<String,VarMeta> usagesLeft) {
			sb.append(id).append("[");
			final Weights out;
			if(args.length>0) {
				out = args[0].toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
				for(int i=1;i<args.length;i++) {
					sb.append(",");
					out.minMax(args[1].toStringAutoWeightsAndAutoExponentials(sb,usagesLeft));
				}
			}else {
				out = str();
			}
			sb.append("]");
			return out;
		}
		@Override
		public void countUsages(Consumer<String> countUsage) {
			for(Solomonoff arg:args) {
				arg.countUsages(countUsage);
			}
		}
	}
	public static class SolRange implements Solomonoff{
		final int fromInclusive,toInclusive;
		public SolRange(int fromInclusive,int toInclusive) {
			this.fromInclusive = fromInclusive;
			this.toInclusive = toInclusive;
		}
		@Override
		public int precedence() {
			return 3;
		}
		@Override
		public void toString(StringBuilder sb) {
			IntSeq.appendRange(sb, fromInclusive, toInclusive);
		}
		@Override
		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb,
				Map<String,VarMeta> usagesLeft) {
			IntSeq.appendRange(sb, fromInclusive, toInclusive);
			return str();
		}
		@Override
		public void countUsages(Consumer<String> countUsage) {
		}
	}
}