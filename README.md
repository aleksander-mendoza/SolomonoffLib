# SolomonoffLib 

## Regular expressions

The language supports regular expressions of the following form

    'this is some literal string'
    'this is' | 'union of languages'
    'this is Kleene closure of zero or more elements'*
    'this is Kleene closure of one or more elements'+
    'this is Kleene closure of zero or one elements'?
    'this is' : 'output'
    ('this is' : 'output under Kleene closure')*
    'this is' : 'also output under Kleene closure'*
    'this is' : ('invalid because it leads to nondeterminism'*)
    'this is invalid':'because it leads to nondeterminism' | 'this is invalid':'because it leads to nondeterminism as well'
    'this' [a-z] 'is a range'
    'more ranges:' [a-z][a-z][0-9]*[a-z]
    'not a range [a-z]'
    'this is epsilon output':''
    '':'this is output from epsilon'
    '':'this is invalid because it leads to nondeterminism'*
    'this' 1 'is weighted expression'
    'this is weighted Kleen closure' 3 *
    'going ' ( 3 'here is':'more important' | 2 'here is':'less important') ' hence there is no nondeterminism'
    
## Vernacular language

The language of regular expressions gains additional power from
being embedded in 'vernacular' language of functions. 
    
    //You can use line comments
    /* and
    multiline
    comments
    */
        
    custom_alphabet = [a-z]
    binary_alphabet = [0-1]
    
    function1 :: binary_alphabet* -> binary_alphabet*
    function1 = '01':'011' | '':'10'
    
    function2 = 'functions can be reused ' function1 ' like this'
    // It's actually more of a variable than a function at the moment 
    
All automata are always guaranteed to be functional (at most one output is generated for every input).
Hence the type of every transducer is of the form `A -> B` (rather than `A × B`).
The dot `.` stands for all possible symbols (except for `\0` which is not considered to be
a "possible" symbol). Hence dot is the alphabet ∑ and `\0` is assumed to be 
"some symbol outside of alphabet", which often comes in handy. 

The type `.*` is the top type. Every string belongs to it. Conversely
there is also the bottom type, denoted with `#`. No string belongs to `#` (not even `\0`).
Notice that strings containing `\0` live completely outside of this type hierarchy.   
    
    function2 = 'this function has no type, hence it defaults to # -> .*'
    
    function3 :: .* -> .*
    function3 = .* : 'ala'
    
    function4 :: [a-z] -> .*
    function4 :: . : 'test'
 
 It's possible to assign multiple types to a single function 
 (hence you can observe that the type system is polymorphic).
        
    
    multiple_types :: # -> .*
    multiple_types :: 'abc' -> .*
    multiple_types :: [a-z] 'bc' -> .*
    multiple_types :: [b-x][b-x][b-x] -> .*
    multiple_types :: [b-x][b-x][b-x] -> ''
    multiple_types = [a-z][a-x][b-z]
    
    //There exists a lattice of types. 

Notice that if `f` is of type `A -> B` then any
string accepted by `A` is also accepted by `f` but the opposite doesn't hold.
Not all strings accepted by `f` are necessarily accepted by `A`.
When it comes to output, the opposite holds. Every string returned/printed
by `f` is accepted by `B` but not every string accepted by `B` can be
 printed by `f`. 

You can reuse functions as types for others. It's a very powerful feature.

    x = 'abc':'01f' 1 | 're':'2' 2
    y :: x -> .*
    y = 'abc':'43' | 're':'kk'
    //everything that is accepted by x must
    //also be accepted by y
    
    z :: 'abc' -> x
    z = 'abc':'re'
    //everything that is printed by z is 
    //guaranteed to be accepted by x
    
This even has some resemblance to object-oriented
 programming with abstract classes and extensions.
 Here domain (left projection/input projection) of
 one transducer can be used as basis for another.

    
This type-system is very expressive. You can easily 
 define finite state acceptors as a special
case of transducers of type `# -> ''`.

    plain_regex :: # -> ''
    plain_regex = 'abc' | 'red'*
    
You can use letters for convenience, but in reality
everything is a 32bit integer. You can specify  
everything directly in integers if you wish.

    with_integers = <1> <3> | (<43> <1243>)*
    int_ranges = <1-54> | <32-54> <1> <100-100>*  
    
The dot `.` itself is not anything magical
It's just a syntactic sugars for
  
    . = <1-2147483647>
    // dots can be used in names of variables
    
    // Note that 2147483647==Integer.MAX_VALUE

The same goes for `#`. It's also just a normal variable 
(not a keyword). Unfortunately it's not possible to
build empty transducer by hand, hence it's built into
the compiler. 

Interestingly, while `.` does not include `\0` is easy
to define your own variable that does, if necessary.
For example

    .0 = <0-2147483647>

This compiler employs one special optimisation technique.
Because `\0` is not considered to be a possible 'normal' symbol,
it becomes interpreted as a 'special' symbol instead.
Whenever you write

    mirror = 'reflect ':<0> [a-z] ' input' 
    
The `<0>` is interpreted as 'reflection' of input. Here
is example of how evaluation of such expression would look like

    
    mirror('reflect a input')='a'
    mirror('reflect b input')='b' 
    ...
    mirror('reflect z input')='z' 

This way instead of having to create 26 transitions labeled with
 `[a-a]:a`, `[b-b]:b`, ... `[c-c]:c`, compiler creates only one transition
 `[a-z]:<0>` that get's interpreted in a special way during automaton
 evaluation. The type system is aware of this feature and in fact,
 can very efficiently test such reflected ranges. 
 
     mirror :: # -> [a-z] // is true
     mirror :: # -> [a-b] // is false
     mirror :: # -> 'a'|'b'|...|'z' // is true
     mirror :: # -> . // is true
     mirror :: # -> 'a' // is false
     
This feature is very useful for writing parts of
regular expressions that leave most of input intact and rewrite
only some interesting places. For instance here is how to implement
a replace-all function

    replace_abc_with_x = ('':<0> . | 'abc':'x' 1 )*

Not that we have to write `'':<0> .` instead of `.:<0>` because
the mirror symbol `<0>` reflects input that appears after it (this follows directly from
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

    tricky_regex = ('a':'y' 2 | 'a':'x' 3)('b':'y' 2 | 'b':'x' 3)
    
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

You can call external functions

    automaton_built_from_java = externalFuncName!('arg1', 'arg2', 'arg3')

Alternatively argument pairs are allowed:

    automaton_built_from_java_with_labels = externalFuncName!('arg1':'label1', 'arg2':'label2', 'arg3':'label3')
    
Those correspond respectively to text and informant (from the theory of inductive inference).
The notation `'input'` is a syntactic sugar for `'input':''`, whereas `'input':#` stands for negative
example (the input does is not accepted by the automaton). You can for instance use machine learning to 
automatically build automata from examples

    learned_from_text = rpni!('a', 'aa':#, 'aaa', 'aaaa':#)
    learned_from_informant = rpni_mealy!('a':'0', 'aa':'01', 'aaa':'010', 'aaaa':'0101')
    
There are the following learning functions available:

    rpni!   - provided by LearnLib
    rpni_edsm!   - provided by LearnLib
    rpni_mdl!   - provided by LearnLib
    rpni_mealy!   - provided by LearnLib
    ostia!   - provided by SolomonoffLib
    apti!    - provided by SolomonoffLib
    oftia!   - provided by SolomonoffLib
  
There are also some utility functions like

    import!('path/to/file.mealy')
    prefixTreeAcceptor!
    prefixTreeTransducer!
    importDOT!
    importATT!

    

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
                    CharStreams.fromFileName("some/file/path.mealy"),true);
    String output = compiled.run("function name","input string");


##### Maven

    <repositories>
        <repository>
            <id>solomonoff</id>
            <url>https://raw.github.com/aleksander-mendoza/SolomonoffLib/repository/</url>
        </repository>
    </repositories>
    
    <dependencies>
        <dependency>
            <groupId>solomonoff</groupId>
            <artifactId>solomonoff</artifactId>
            <version>1.2</version>
        </dependency>
    </dependencies>    

##### Gradle

    repositories {
        maven { url "https://raw.github.com/aleksander-mendoza/SolomonoffLib/repository/" }
    }
    
    dependencies {
        compile group: 'solomonoff', name: 'solomonoff', version:'1.2'
    }

## Mathematics and more
    
Detailed explanation can be found [here (glushkov construction)](https://arxiv.org/abs/2008.02239) and [here (multitape automata and better proofs)](https://arxiv.org/abs/2007.12940) and [here (tutorial explaining the implementation)](https://aleksander-mendoza.github.io/mealy_compiler.html). You can also see online demo [here (work in progress)](https://alagris.github.io/web/main.html)