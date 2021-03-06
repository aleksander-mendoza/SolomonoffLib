package net.alagris.cli.conv;

import java.util.Objects;

public class EncodedID implements CharSequence {
    final String id;
    final VarState state;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        EncodedID encodedID = (EncodedID) o;
        return Objects.equals(id, encodedID.id) &&
                state == encodedID.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, state);
    }

    public EncodedID(String id, VarState state) {
        this.id = id;
        this.state = state;
    }

    public EncodedID(EncodedID id, VarState state) {
        this.id = id.id;
        this.state = state;
    }

    @Override
    public int length() {
        return id.length() + 1;
    }

    @Override
    public char charAt(int index) {
        return index == 0 ? state.encodingPrefixChar() : id.charAt(index - 1);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
    }


    @Override
    public String toString() {
        return encodeID();
    }

    public String encodeID() {
        return encodeID(id, state);
    }

    public String encodeInvID() {
        return encodeID(id, state.inv());
    }

    public static String encodeID(String id, VarState state) {
        return state.encodingPrefix + id;
    }

    public String id() {
        return id;
    }
}
