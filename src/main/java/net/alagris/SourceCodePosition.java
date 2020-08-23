package net.alagris;

import org.antlr.v4.runtime.Token;

public class SourceCodePosition {

    public static final SourceCodePosition NONE = new SourceCodePosition("", -1, -1);
    final private int line;// line in source code
    final private int column;// column in source code
    final private String file;// file with source code

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public SourceCodePosition(String file, int line, int column) {
        this.line = line;
        this.file = file;
        this.column = column;
    }

    public SourceCodePosition(Token symbol) {
        this(symbol.getTokenSource().getSourceName(),symbol.getLine(),symbol.getCharPositionInLine());
    }

    public String getFile() {
        return file;
    }
    
    @Override
    public String toString() {
        return "in "+file+" at "+line+":"+column;
    }
}
