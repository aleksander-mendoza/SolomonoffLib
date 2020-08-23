package net.alagris.learn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import net.alagris.learn.__.A2;
import net.alagris.learn.__.F2;
import net.alagris.learn.__.HM;
import net.alagris.learn.__.I;
import net.alagris.learn.__.S;
import net.alagris.learn.__.V1;

public class Patterns {

    private Patterns() {
    }

    public static <Sigma, X> S<Sigma> substitute(S<A2<Sigma, X>> pattern, F2<X, S<Sigma>> substitution) {
        return S.arrList(S.fold(new ArrayList<>(), pattern, (str, a) -> I.fold(new ArrayList<Sigma>(),
                (I<Sigma>) a.match(I::single, __.o(substitution, I::seq)), HM::listAdd)));
    }

    public static <Sigma, X> S<A2<Sigma, X>> learn(Text<Sigma> samples,I<X> variableGenerator) {
        HashSet<S<Sigma>> shortest = new HashSet<>();
        int len = I.fold(Integer.MAX_VALUE, samples, (minLen, sample) -> {
            if (sample.size() <= minLen) {
                if (sample.size() < minLen)
                    shortest.clear();
                shortest.add(sample);
                return sample.size();
            }
            return minLen;
        });
        if(shortest.isEmpty())return null;
        final ArrayList<A2<Sigma, X>> result = new ArrayList<>(len);
        loop:for(int i=0;i<len;i++) {
            Iterator<S<Sigma>> iter = shortest.iterator();
            S<Sigma> first = iter.next();
            Sigma sigma = first.f(i);
            while(iter.hasNext()) {
                S<Sigma> nextSample = iter.next();
                if(!Objects.equals(nextSample.f(i),sigma)) {
                    result.add(__.r(variableGenerator.n().force()));
                    continue loop;
                }
            }
            result.add(__.l(sigma));
        }
        return S.arrList(result);
    }
}
