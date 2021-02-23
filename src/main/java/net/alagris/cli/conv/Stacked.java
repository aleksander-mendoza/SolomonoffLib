package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class Stacked  extends  ArrayList<Solomonoff>{

    int compositionHeight;

    public Stacked() {

    }

    private void incrementHeight(Solomonoff sol) {
        if (sol instanceof AtomicVar) {
            compositionHeight += ((AtomicVar) sol).compositionHeight();
        } else {
            compositionHeight += 1;
        }
    }

    @Override
    public boolean add(Solomonoff sol) {
        incrementHeight(sol);
        return super.add(sol);
    }

    @Override
    public boolean addAll(Collection<? extends Solomonoff> c) {
        for (Solomonoff sol : c) {
            incrementHeight(sol);
        }
        return super.addAll(c);
    }

    @Override
    public void clear() {
        compositionHeight = 0;
        super.clear();
    }

    public Stacked(int capacity, Solomonoff repeat) {
        super(capacity);
        for (int i = 0; i < capacity; i++) {
            add(repeat);
        }
    }

    public Stacked(Solomonoff regex) {
        super(1);
        add(regex);
    }

    public Stacked(Stacked copy) {
        super(copy.size());
        for (int i = 0; i < copy.size(); i++) {
            add(copy.get(i));
        }
        assert this.compositionHeight == copy.compositionHeight;
    }

    public void countUsages(Consumer<AtomicVar> countUsage) {
        for (int i = 0; i < size(); i++) {
            get(i).countUsages(countUsage);
        }
    }

    public static Solomonoff dotStarRefl = new SolKleene(new SolConcat(
            new SolProd(new IntSeq(Kolmogorov.SPECS.reflect())),
            new AtomicSet(Atomic.NON_MARKERS)),'*');

    private void expandVariables(VarQuery query, ArrayList<Solomonoff> list) {
        for (int i = 0; i < size(); i++) {
            final Solomonoff sol = get(i);
            if (sol instanceof AtomicVar) {
                final AtomicVar v = (AtomicVar) sol;
                if (v.compositionHeight() > 1) {
                    final Stacked referenced = query.variableDefinitions(v);
                    assert referenced.compositionHeight == v.compositionHeight():referenced+" "+referenced.compositionHeight+" == "+v.compositionHeight();
                    referenced.expandVariables(query, list);
                }else{
                    list.add(sol);
                }
            } else {
                list.add(sol);
            }
        }
    }

    public void ensureVarsExpanded(VarQuery query) {
        assert size() <= compositionHeight;
        if (size() < compositionHeight) {
            final ArrayList<Solomonoff> list = new ArrayList<>(compositionHeight);
            expandVariables(query, list);
            clear();
            addAll(list);
        }
        assert size() == compositionHeight;
    }

    public void ensureEqualSize(Stacked other) {
        assert size() == compositionHeight;
        assert other.size() == other.compositionHeight:other+" "+other.size() +" "+ other.compositionHeight;
        if (size() == other.size()) {
            return;
        }
        if (size() < other.size()) {
            while (size() < other.size()) {
                add(dotStarRefl);
            }
        } else {
            while (size() > other.size()) {
                other.add(dotStarRefl);
            }
        }
    }

    public void prependMarker(int marker) {
        assert size() == compositionHeight;
        final Solomonoff in = new AtomicChar(marker);
        assert size() > 1;
        final Solomonoff out = new SolProd(new IntSeq(marker));
        final Solomonoff refl = new SolConcat(in, out);
        set(0, Optimise.concat(out, get(0)));
        final int last = size() - 1;
        set(last, Optimise.concat(in, get(last)));
        for (int i = 1; i < last; i++) {
            set(i, Optimise.concat(refl, get(i)));
        }
    }

    public Stacked diff(Stacked rhs) {
        if (compositionHeight() == 1 && rhs.compositionHeight() == 1) {
            final Solomonoff[] args = {get(0), rhs.get(0)};
            return new Stacked(new SolFunc(args, "subtract"));
        } else {
            throw new IllegalArgumentException("Cannot subtract pipelines");
        }
    }

    public int compositionHeight() {
        return compositionHeight;
    }

    public Stacked refl() {
        assert compositionHeight() == 1;
        Solomonoff r = get(0);
        clear();
        assert r instanceof Atomic.Set;
        add(new SolConcat(new SolProd(new IntSeq(Kolmogorov.SPECS.reflect())), r));
        return this;
    }

    public Stacked identity(VarQuery query) {
        ensureVarsExpanded(query);
        final Solomonoff[] args = {get(0)};
        clear();
        add(new SolFunc(args, "identity"));
        return this;
    }

    public Stacked clearOutput(VarQuery query) {
        ensureVarsExpanded(query);
        final Solomonoff[] args = {get(0)};
        clear();
        add(new SolFunc(args, "clearOutput"));
        return this;
    }

    public Stacked union(Stacked rhs, VarQuery query) {
        if (compositionHeight() == 1 && rhs.compositionHeight() == 1) {
            Solomonoff r = Optimise.union(get(0), rhs.get(0));
            clear();
            add(r);
        } else {
            ensureVarsExpanded(query);
            rhs.ensureVarsExpanded(query);
            ensureEqualSize(rhs);
            prependMarker(Atomic.LEFT_UNION_MARKER);
            rhs.prependMarker(Atomic.RIGHT_UNION_MARKER);
            for (int i = 0; i < size(); i++) {
                set(i, new SolUnion(get(i), rhs.get(i)));
            }
        }
        return this;
    }

    public Stacked concat(Stacked rhs, VarQuery query) {
        if (compositionHeight() == 1 && rhs.compositionHeight() == 1) {
            Solomonoff r = Optimise.concat(get(0), rhs.get(0));
            clear();
            add(r);
        } else {
            ensureVarsExpanded(query);
            rhs.ensureVarsExpanded(query);
            ensureEqualSize(rhs);
            rhs.prependMarker(Atomic.CONCAT_MARKER);
            for (int i = 0; i < size(); i++) {
                set(i, Optimise.concat(get(i), rhs.get(i)));
            }
        }
        return this;
    }

    public Stacked kleene(char type, VarQuery query) {
        if (compositionHeight() == 1) {
            set(0, new SolKleene(get(0), type));
            return this;
        } else {
            ensureVarsExpanded(query);
            if (type == '*' || type == '+') {
                prependMarker(Atomic.KLEENE_MARKER);
                for (int i = 0; i < size(); i++) {
                    set(i, new SolKleene(get(i), type));
                }
                return this;
            } else {
                assert type == '?';
                return union(new Stacked(size(), Atomic.EPSILON), query);
            }
        }
    }

    public Stacked power(int power, VarQuery query) {
        assert power >= 0;
        if (compositionHeight() == 1) {
            set(0, Optimise.power(get(0), power));
            return this;
        } else if (power == 0) {
            clear();
            add(Atomic.EPSILON);
            return this;
        } else {
            ensureVarsExpanded(query);
            prependMarker(Atomic.CONCAT_MARKER);
            for (int i = 0; i < size(); i++) {
                set(i, Optimise.power(get(i),power));
            }
            return this;
        }
    }

    public Stacked powerOptional(int power, VarQuery query) {
        assert power >= 0;
        if (compositionHeight() == 1) {
            set(0, Optimise.powerOptional(get(0), power));
            return this;
        } else if (power == 0) {
            clear();
            add(Atomic.EPSILON);
            return this;
        } else {
            ensureVarsExpanded(query);
            kleene('?', query);
            prependMarker(Atomic.CONCAT_MARKER);
            for (int i = 0; i < size(); i++) {
                set(i, Optimise.power(get(i),power));
            }
            return this;
        }
    }

    public Stacked comp(Stacked rhs) {
        addAll(rhs);
        return this;
    }

    public Stacked inv() {
        if (compositionHeight() != 1) {
            throw new IllegalArgumentException("Cannot invert lazy pipeline");
        }
        final Solomonoff[] args = {get(0)};
        set(0, new SolFunc(args, "inverse"));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        for (int i = 0; i < size(); i++) {
            Solomonoff part = get(i);
            sb.append("    ");
            if (part instanceof AtomicVar && ((AtomicVar) part).compositionHeight() > 1) {
                sb.append("@").append(((AtomicVar) part).encodeID());
            } else {
                part.toString(sb);
            }
            if (i + 1 < size()) sb.append(";");
            sb.append("\n");
        }
    }

    public void toStringAutoWeightsAndAutoExponentials(StringBuilder sb,boolean nonfunc, Function<EncodedID, StringifierMeta> meta) {
        for (int i = 0; i < size(); i++) {
            Solomonoff part = get(i);
            sb.append("    ");
            if (part instanceof AtomicVar && ((AtomicVar) part).compositionHeight() > 1) {
                sb.append("@").append(((AtomicVar) part).encodeID());
            } else {
                if(nonfunc){
                    sb.append("nonfunc ");
                }
                part.toStringAutoWeightsAndAutoExponentials(sb, meta);
            }
            if (i + 1 < size()) sb.append(";");
            sb.append("\n");
        }
    }
}
