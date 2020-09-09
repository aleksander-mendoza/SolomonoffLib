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

    
    //You can use line comments
    /* and
    multiline
    comments
    */
        
    custom_alphabet = [a-z];
    binary_alphabet = [0-1];
    
    function1 : binary_alphabet* -> binary_alphabet*
    function1 = "01":"011" | "":"10"
    
    function2 = "this function has no type, hence it defaults to using .* as alphabet"
    
    function3 = "functions can be reused like this" function2 
    // It's actually more of a variable than a function at the moment    
    
    multiple_types :: .* -> .*
    multiple_types :: "a"*
    multiple_types :: "aaa"
    multiple_types = "aaa"
        
    //There exists a lattice of types.
    //The type .* is the most general one and all strings belong to it
    //Type # is the bottom type and no string belongs to it
    
    nothing_matched :: # -> .*
    nothing_matched = #
    
    // Type "abcd" is somethwere between # and .*
    
    // You can reuse functions as types for others
    some_transducer = "abc":"01f" 1 | "re":"2" 2
    reused_domain :: some_transducer -> .*
    reused_domain = "abc":"43"
    
    // This even has some resemblance to object-oriented
    // programming with abstract classes and extensions.
    // Here domain (left projection/input projection) of
    // one transducer can be used as basis for another
    
    // This type-system is very expressive. You can easily 
    // define finite state acceptors as a special
    // case of transducers of type .* -> ""
    plain_regex :: .* -> ""
    plain_regex = "abc" | "red"*
    
     
    
Detailed explanation can be found [here (glushkov construction)](https://arxiv.org/abs/2008.02239) and [here (multitape automata and better proofs)](https://arxiv.org/abs/2007.12940) and [here (tutorial explaining the implementation)](https://aleksander-mendoza.github.io/mealy_compiler.html). You can also see online demo [here (work in progress)](https://alagris.github.io/web/main.html)