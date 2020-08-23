package net.alagris;

import java.util.Stack;

public class CompilationError extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public CompilationError(String msg) {
        super(msg);
    }

    public static final class FuncDuplicateBody extends CompilationError {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        private final SourceCodePosition firstDeclaration;
        private final SourceCodePosition secondDeclaration;
        private final String name;

        public FuncDuplicateBody(SourceCodePosition firstDeclaration, SourceCodePosition secondDeclaration,
                String name) {
            super(name+" is implemented at "+firstDeclaration+" and "+secondDeclaration);
            this.firstDeclaration = firstDeclaration;
            this.secondDeclaration = secondDeclaration;
            this.name = name;
        }

        public SourceCodePosition getFirstDeclaration() {
            return firstDeclaration;
        }

        public SourceCodePosition getSecondDeclaration() {
            return secondDeclaration;
        }

        public String getName() {
            return name;
        }

    }

    public static final class FuncDuplicateType extends CompilationError {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        private final SourceCodePosition firstDeclaration;
        private final SourceCodePosition secondDeclaration;
        private final String name;

        public FuncDuplicateType(SourceCodePosition firstDeclaration, SourceCodePosition secondDeclaration,
                String name) {
            super(name+" is declared at "+firstDeclaration+" and "+secondDeclaration);
            this.firstDeclaration = firstDeclaration;
            this.secondDeclaration = secondDeclaration;
            this.name = name;
        }

        public SourceCodePosition getFirstDeclaration() {
            return firstDeclaration;
        }

        public SourceCodePosition getSecondDeclaration() {
            return secondDeclaration;
        }

        public String getName() {
            return name;
        }

    }

    public static final class IllegalCharacter extends CompilationError {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        private final SourceCodePosition pos;
        private final String charName;

        public IllegalCharacter(SourceCodePosition pos,String charName) {
            super("character "+charName+" is not allowed! "+pos);
            this.pos = pos;
            this.charName = charName;
        }

        public String getCharName() {
            return charName;
        }

        public SourceCodePosition getPos() {
            return pos;
        }

    }

    public static final class Errors extends Stack<CompilationError> {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        public <E extends CompilationError> E register(E item) {
            return (E) super.push(item);
        }

    }
}
