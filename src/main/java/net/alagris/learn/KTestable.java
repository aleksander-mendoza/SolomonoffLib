package net.alagris.learn;

import java.util.HashSet;

import net.alagris.learn.__.I;
import net.alagris.learn.__.S;

public class KTestable<Sigma> {
    int k;
    HashSet<S<Sigma>> prefixes, // of length k-1
            suffixes, // of length k-1
            shortStrings, // of length less than k
            allowedSegments; // of length k

    public KTestable(int k, HashSet<S<Sigma>> prefixes, HashSet<S<Sigma>> suffixes, HashSet<S<Sigma>> shortStrings,
            HashSet<S<Sigma>> allowedSegments) {
        this.k = k;
        this.prefixes = prefixes;
        this.suffixes = suffixes;
        this.shortStrings = shortStrings;
        this.allowedSegments = allowedSegments;
    }

    boolean run(S<Sigma> input) {
        if (input.size() < k) {
            return shortStrings.contains(input);
        }
        if (!prefixes.contains(S.leftLazy(input, k - 1))) {
            return false;
        }
        if (!suffixes.contains(S.rightLazy(input, k - 1))) {
            return false;
        }
        for (int i = 0; i + k < input.size(); i++) {
            if (!allowedSegments.contains(S.subLazy(input, i, i + k)))
                return false;
        }
        return true;
    }

    public static <Sigma> KTestable<Sigma> learn(int k, Text<Sigma> samples) {
        HashSet<S<Sigma>> prefixes = new HashSet<>(); // of length k-1
        HashSet<S<Sigma>> suffixes = new HashSet<>(); // of length k-1
        HashSet<S<Sigma>> shortStrings = new HashSet<>(); // of length less than k
        HashSet<S<Sigma>> allowedSegments = new HashSet<>();
        I.foreach(samples, sample -> {
            for (int i = 0; i + k < sample.size(); i++) {
                allowedSegments.add(S.subLazy(sample, i, i + k));
            }
            if (sample.size() >= k - 1) {
                prefixes.add(S.leftLazy(sample, k - 1));
                suffixes.add(S.rightLazy(sample, k - 1));
            }
            if (sample.size() < k) {
                shortStrings.add(sample);
            }
            return null;
        });
        return new KTestable<>(k, prefixes, suffixes, shortStrings, allowedSegments);
    }

}
