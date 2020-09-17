# SolomonoffLib 

## Regular expressions

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
    
## Vernacular language

The language of regular expressions gain additional power from
being embedded in "vernacular" language of functions. 



    
    //You can use line comments
    /* and
    multiline
    comments
    */
        
    custom_alphabet = [a-z]
    binary_alphabet = [0-1]
    
    function1 :: binary_alphabet* -> binary_alphabet*
    function1 = "01":"011" | "":"10"
    
All automata are always guaranteed to be functional (at most one output is generated for every input).
Hence the type of every transuder is of the form `A -> B` (rather than `A × B`). 
 
    
    function2 = "this function has no type, hence it defaults to using .* as alphabet"
    
    function3 = "functions can be reused like this" function2 
    // It's actually more of a variable than a function at the moment    
    
    multiple_types :: .* -> .*
    multiple_types :: "a"* -> .*
    multiple_types :: "aaa" -> .*
    multiple_types = "aaa"
        
There exists a lattice of types.
The type .* is the most general one and all strings belong to it
Type # is the bottom type and no string belongs to it
    
    nothing_matched :: # -> .*
    nothing_matched = #
    
    // Type "abcd" is somethwere between # and .*
    
You can reuse functions as types for others

    some_transducer = "abc":"01f" 1 | "re":"2" 2
    reused_domain :: some_transducer -> .*
    reused_domain = "abc":"43"
    
This even has some resemblance to object-oriented
 programming with abstract classes and extensions.
 Here domain (left projection/input projection) of
 one transducer can be used as basis for another
    
This type-system is very expressive. You can easily 
 define finite state acceptors as a special
case of transducers of type `.* -> ""`

    plain_regex :: .* -> ""
    plain_regex = "abc" | "red"*
    
You can use letters for convenience, but in reality
everything is a 32bit integer. You can specify  
everything directly in integers if you wish.

    with_integers = <1> <3> | (<43> <1243>)*
    int_ranges = <1-54> | <32-54> <1> <100-100>*  
    
The dot . and hash # themselves are not anything magical
They are just syntactic sugars for
  
    # = "\0" 
    . = <1-2147483647>
    
Note that zero cannot in normal circumstances appear in
any string, hence it stands for bottom type that matches
nothing. (Also note that `2147483647==Integer.MAX_VALUE`)
    
Compiler makes sure that # doesn't appear in 
output string like a "normal" symbol.
     
    zero_not_allowed_on_input = "\0" //won't compile
    zero_not_allowed_on_output = "a":"\0" //won't compile
    
However, you can write
    
    zero_allowed_on_input = # 
    
Such expressions are useful only for one purpose - as types of other regexes.
We can easily use it to say that some regex should be the empty language.
(just like `Void` type in Haskell/Java/etc).
    
    required_to_be_empty_language :: zero_allowed_on_input -> .*
    
This compiler employs one special optimisation technique.
Because `\0` is not allowed on output as a "normal" symbol,
it becomes interpreted as a "special" symbol instead.
Whenever you write

    mirror = "reflect ":"#" [a-z] " input" 
    
The `#` is interpreted as "reflection" of input. Here
is example of how evaluation of such expression would look like

    
    mirror("reflect a input")="a"
    mirror("reflect b input")="b" 
    ...
    mirror("reflect z input")="z" 

This way instead of having to create 26 transitions labeled with
 `[a-a]:a`, `[b-b]:b`, ... `[c-c]:c`, compiler creates only one transition
 `[a-z]:#` that get's interpreted in a special way during automaton
 evaluation. The type system is aware of this feature and in fact,
 can very efficiently test such reflected ranges. 
 
     mirror :: .* -> [a-z] // is true
     mirror :: .* -> [a-b] // is false
     mirror :: .* -> "a"|"b"|...|"z" // is true
     mirror :: .* -> . // is true
     mirror :: .* -> "a" // is false
     
This feature is very useful for writing parts of
regular expressions that leave most of input intact and rewrite
only some interesting places. For instance here is how to implement
a replace-all function

    replace_abc_with_x = ("":"#" . | "abc":"x" 1 )*

Not that we have to write `"":"#" .` instead of `.:"#"` because
the mirror symbol `#` reflects input that appears after it (this follows directly from
the nature of Glushkov's construction).

While Glushkov's construction by itself guarantees very small automata,
they are always additionally compressed and optimised using a pseudo-minimization
algorithm. For example a regular expression like

    large_regex = <1> | <2> | ... | <999>
    
should have (according to Glushkov's algorithm) one thousand states.
However, optimisation compresses it down to only 2 states and
a thousand edges. Of course a much better approach would be

    small_regex = <1-999>
    
which has only 2 states and one edge. A more interesting example would be

    tricky_regex = ("a":"y" 2 | "a":"x" 3)("b":"y" 2 | "b":"x" 3)
    
which should normally have 5 states, but gets compressed down to just 3, because
the alternatives with lower weights can clearly be discarded.
Notice that this compression algorithm is not the same as minimisation
algorithm! In fact, because the automata are always nondeterministic,
they are often already very small and and attempts at building minimal deterministic ones
would either fail completely (nondeterministic functional transducers are strictly more
powerful than deterministic ones) or yield (possibly exponentially) larger automata, than 
their nondeterministic versions. Hence the pseudo-minimisation algorithm
attempts to compress nondeterministic automata as much as possible, but doesn't
try to find the smallest nondeterministic automaton possible (because the problem is hard
and would consume too much resources).



## Usage

#### From jar

You can compile the project using

    ./gradlew fatJar

Then you can run
 
    java -jar build/libs/Mealy.jar sample.mealy

Wait until you see

    All loaded correctly!
    
Then you can start evaluating transducer by typing
 
    [function name] [input string]

like for example 
    
    mirror reflect k input

will output

    k 
    
#### From java

You can very easily use the compiler for Java API using

    final OptimisedHashLexTransducer compiled = new OptimisedHashLexTransducer(
                    CharStreams.fromFileName(args[0]), true,true);
    String output = compiled.run("function name","input string");


## Mathematics and more
    
Detailed explanation can be found [here (glushkov construction)](https://arxiv.org/abs/2008.02239) and [here (multitape automata and better proofs)](https://arxiv.org/abs/2007.12940) and [here (tutorial explaining the implementation)](https://aleksander-mendoza.github.io/mealy_compiler.html). You can also see online demo [here (work in progress)](https://alagris.github.io/web/main.html)