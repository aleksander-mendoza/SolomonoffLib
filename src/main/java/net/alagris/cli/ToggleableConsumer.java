package net.alagris.cli;

import java.util.function.Consumer;

public interface ToggleableConsumer<X> extends Consumer<X> {
    boolean isEnabled();
    void setEnabled(boolean value);
}
