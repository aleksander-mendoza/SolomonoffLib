package net.alagris.cli.conv;

public interface VarQuery {
    Kolmogorov variableAssignment(EncodedID id);

    Stacked variableDefinitions(EncodedID id);
}
