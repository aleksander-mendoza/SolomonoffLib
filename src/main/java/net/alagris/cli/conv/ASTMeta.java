package net.alagris.cli.conv;

public class ASTMeta<AST> {
    final AST re;
    final boolean export;

    public ASTMeta(AST re, boolean export) {
        this.re = re;
        this.export = export;
    }

    @Override
    public String toString() {
        return re.toString();
    }
}
