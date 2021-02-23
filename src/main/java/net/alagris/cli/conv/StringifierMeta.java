package net.alagris.cli.conv;

public class StringifierMeta {
    int usagesLeft;
    Weights weights;

    public StringifierMeta(int usagesLeft) {
        this.usagesLeft = usagesLeft;
    }

    public StringifierMeta increment() {
        usagesLeft++;
        return this;
    }
}
