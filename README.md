# SolomonoffLib 


The language supports regular expressions of the following form

    "this is some literal string"
    "this is" | "union of languages"
    "this is Kleene closure"*
    "this is" : "output"
    ("this is" : "output under Kleene closure")*
    "this is" : "also output under Kleene closure"*
    "this is" : ("invalid because it leads to nondeterminism"*)
    "this is invalid":"because it leads to nondeterminism" | "this is invalid":"because it leads to nondeterminism as well"
    "this" [a-z] "is a range"
    "more ranges:" [a-z][a-z][0-9]*[a-z]
    "not a range [a-z]"
    "this is epsilon output":""
    "":"this is output from epsilon"
    "":"this is invalid because it leads to nondeterminism"*
    "this" 1 "is weighted expression"
    "this is weighted Kleen closure" 3 *
    "going " ( 3 "here is":"more important" | 2 "here is":"less important") " hence there is no nondeterminism"
    
All automata are always guaranteed to be functional (at most one output is generated for every input). On top of that, there is support for functions

    custom_alphabet = [asdf];
    custom_binary_alphabet = [01];
    permuted_alphabet = [10];
    
    function1 : permuted_alphabet -> permuted_alphabet
    function1() = "01":"011" | "":"10"
    
    function2() = "this function has no type, hence it defaults to using UNICODE as alphabet"
    
    function3() = "functions can be used like this" function2 "It's actually more of a variable at the moment."
    
    adsd() = "all functions can be typed"
    
    struct two_tape_automaton{
    	tape1 : UNICODE,
    	tape2 : custom_binary_alphabet
    }
    
    two_tape_function : two_tape_automaton -> UNICODE
    two_tape_function() = {tape1="x":"this regex",tape2="0":"simulates automaton reading two tapes at once"}
    
     struct two_tape_async_automaton{
    	tape1 : UNICODE*,
    	tape2 : custom_binary_alphabet*
    }
    
    two_tape_async_function : two_tape_async_automaton -> UNICODE
    two_tape_async_automaton() = {tape1="this automaton doesn't need to read tapes one by one#":"it can read any number of symbols as if it was asynchronous. (Hashtag is terminator symbol)", tape2="010101" }

Detailed explanation can be found [here (glushkov construction)](https://arxiv.org/abs/2008.02239) and [here (multitape automata and better proofs)](https://arxiv.org/abs/2007.12940) and [here (tutorial explaining the implementation)](https://aleksander-mendoza.github.io/mealy_compiler.html). You can also see online demo [here (work in progress)](https://alagris.github.io/web/main.html)