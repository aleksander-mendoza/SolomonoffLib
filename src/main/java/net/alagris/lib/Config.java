package net.alagris.lib;

import net.alagris.core.*;

public class Config {
    public boolean eagerCopy = false;
    public int minimalSymbol = 0;
    public int maximalSymbol = 0xffffffff;
    public int midSymbol = 0x7fffffff;
    public boolean useStandardLibrary = true;
    public boolean useLearnLib = true;

    public static Config config() {
        return new Config();
    }

    public static  Config config(int minSymbol, int midSymbol, int maxSymbol) {
        Config c = new Config();
        c.maximalSymbol = maxSymbol;
        c.minimalSymbol = minSymbol;
        c.midSymbol = midSymbol;
        return c;
    }

    public Config eagerCopy() {
        eagerCopy = true;
        return this;
    }

}
