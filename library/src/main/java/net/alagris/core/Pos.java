package net.alagris.core;

import org.antlr.v4.runtime.Token;

/**Position in source code. Holds information about file, line and column of any character.*/
public class Pos {

    public static final Pos NONE = new Pos("<unknown>", -1, -1);
    final private int line;// line in source code
    final private int column;// column in source code
    final private String file;// file with source code

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public Pos(String file, int line, int column) {
        this.line = line;
        this.file = file;
        this.column = column;
    }

    public Pos(Token symbol) {
        this(symbol.getTokenSource().getSourceName(),symbol.getLine(),symbol.getCharPositionInLine());
    }

    public String getFile() {
        return file;
    }
    
    @Override
    public String toString() {
        return ("<unknown>".equals(file)?"":file+":")+line+":"+column;
    }
}
