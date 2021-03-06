package net.alagris.cli.conv;

public interface VarQuery {
    Kolmogorov variableAssignment(EncodedID id);

    Solomonoff variableDefinitions(EncodedID id);

    int introduceAuxiliaryVar(Solomonoff definition);

}
