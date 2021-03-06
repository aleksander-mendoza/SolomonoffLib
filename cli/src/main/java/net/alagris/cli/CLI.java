package net.alagris.cli;

import picocli.CommandLine;

@CommandLine.Command(subcommands = {InteractiveRepl.class,Convert.class})
public class CLI {


    public static void main(String[] args) {
        System.exit(new CommandLine(new CLI()).execute(args));
    }

}
