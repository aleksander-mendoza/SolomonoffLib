package net.alagris;

public class Config {
    boolean eagerCopy = false;
    int minimalSymbol = 0;
    int maximalSymbol = Integer.MAX_VALUE;
    LexUnicodeSpecification.ExternalPipelineFunction externalPipelineFunction = (a, b) -> s -> s;

    public static Config config() {
        return new Config();
    }

    public static Config config(int minSymbol, int maxSymbol) {
        Config c = new Config();
        c.maximalSymbol = maxSymbol;
        c.minimalSymbol = minSymbol;
        return c;
    }

    public Config eagerCopy() {
        eagerCopy = true;
        return this;
    }
}
