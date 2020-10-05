# Solomonoff - transducer compiler with inductive inference

## About

**This project focuses on research in the field of automata theory and inductive inference**. While many existing libraries already provide support for general purpose automata and implement various related algorithms, this project takes a slightly different approach. The primary tool for working with the library, si through doman specific language. Most of the things can be done without writing even a single line of Java code. 

**Compilation of regular expressions is very efficient** thanks to Glshkov's construction. Hence all operations of concatenation, union, Kleene closure (including `*`, `+`, `?`) are constant-time operations. Moreover, the automata will have only as many states as there are symbols in regular expression. 

**All automata are nondeterministic functional** subsequential  weighted transducers. 
The primary semiring of weights is arctic lexicographic semiring (more options will come in the future). Compiler always enforces functionality (that is, at most one output can be printed for each input) through an efficient transducer squaring algorithm (time complexity is quadratic). 

**All regular expressions are strongly typed**. The type system is polymorpic and (unlike in most Turing-complete languages), the typechecking is not done through unification algorithm, but language inclusion is checked. All types are regular expressions themselves as well, although it is required that they are deterministic (that is, the automaton produced with Glushkov's construction is deterministic). This way, language inclusion can be checked by performing product of automata (quadratic time-complexity). In some places (explained below) also nondeterministic language inclusion is checked, but unfortunately it can only be done with subset construction (exponential time complexity). Hence, user is advised to use nondeterministic typechecking only when necessary. 

**All automata are very small** thanks to <ins>nondeterministic pseudo-minimisation</ins>! Unlike most other libraries that implement minimisation through construction of minimal DFA, here we actually perform pseduo-minimisation  on nondeterministic transducers. The algorithm uses heuristics inspired by Brzozowski's construction and Kameda-Weiner's NFA minimisation. Unfortunately performing full minimisation on NFA is a hard problem which requires exponential complexity. Our algorithm is linear on average (and quadratic in very unlikely pessimistic case) and attempts to reduce size of automaton as much as possible, without actually searching for the smallest one. Note that not always finding the smallest nondeterministic transducer, should not be a problem, because the NFA are often much smaller than even the smallest DFA. Moreover, thanks to glushkov's construction, all compiled automata are often in practice smaller than minimal DFA even without pseudo-minimisating them.

**Evaluation is very efficient** thanks to guarantees of lexicographic weights. In pessimistic case evaluation is quadratic, but after performing pseudo-minimisation, it is in practice often close to being linear (optimistic case being deterministic automata, whose evaluation has linear time complexity).  

**Transducer composition is lazy** because otherwise it would pose the danger of exponentially exploding size of automata (composition of two transducers is quadratic, but if composition is used x times in a regular expression then, that would lead to 2^x states in worst case).
However, making composition lazy has some advantages - this compiler allows for invoking external functions written in Java. Hence you can ad-hoc mix regular expressions with custom Java functions. 

**External functions** can be called to add even more features. For instance, instead of making `import some.other.module` a keyword (like in most other languages), here `import!('some/other/module.mealy')` is an external function. It's possible to read automata in various formats such as AT\&T, DOT and compressed binary.

**Inductive inference/machine learning** can be extensively and easily used. At the moment all inference functions are provided by LearnLib (Solomonoff is compatible with LearnLib and AutomataLib). More algorithms, specific to transducers will be added soon.

**Solomonoff implementation is small and generic**. It takes up roughly 10-15 classes and many algrithms are written in a clean reusable manner. 

The primary philosophy used in implementing this library is the top-down approach and features are added conservatively in a well thought-through manner. No features will be added ad-hoc. Everything is meant to fit well together and follow some greater design strategy. For comparision, consider the difference between OpenFst and Solomonoff.

-  OpenFst has `Matcher` that was meant to compactify ranges. In Solomonoff all transitions are ranged and follow the theory of (S,k)-automata. They are well integrated with regular expressions and Glushkov's construction. They allow for more efficient squaring and subset construction. Instead of being an ad-hoc feature, they are well integrated everywhere
-  OpenFst has no built-in support for regular expression and it was added only later in form of Thrax grammars, that aren't much more than another API for calling library functions. In Solomonoff the regular expressions **are** the library. Instead of having separate procedures for union, concatenation and Kleene closure, there is only one procedure that takes arbitrary regular expression and compiles it in batch. This way everything works much faster, doesn't lead to introduction of any &epsilon;-transitions (that's right! In Solomonoff, &epsilon;-transitions aren't even implemented, because they were never needed thanks to Glushkov's construction). This leads to significant differences in performance. Compiling a dictionary txt file that has several thousands of entries, takes hours for OpenFst but only 2-5 seconds for Solomonoff. That comes out-of-the-box without  hand-optimising any code.

 


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
being embedded in the vernacular language of functions. 
    
    //You can use line comments
    /* and
    multiline
    comments
    */
        
    custom_alphabet = [a-z]
    binary_alphabet = [0-1]
    
    function1 <: binary_alphabet* && binary_alphabet*
    function1 = '01':'011' | '':'10'
    
    function2 = 'functions can be reused ' function1 ' like this'
    // It's actually more of a variable than a function at the moment 
    
All automata are always guaranteed to be functional (at most one output is generated for every input).

The symbol `<:` stands for ordering on types. For instance in Java you have `ArrayList<X> <: List<X> <: Collection<X> <: Object`. It is a characterisitc feature of polymorphic type systems. In set-theoretic view one could see it as one type being a subset of another. Hence alternative notation allowed in Solomonoff is

    function1 ⊂ binary_alphabet* && binary_alphabet*

The symbol `&&` represents pairs. It's analogical to set-theoritic Cartesian product. Hence alternative notation is allowed here as well

    function1 ⊂ binary_alphabet* ⨯ binary_alphabet*
    
For example, the string `'a'` is an elements of formal language `{'a'}`, but writing those braces is cumbersome. The string `'01010101'` is an element of set `[0-1]*`. Moreover every string is an element of `.*`.

The dot `.` stands for all possible symbols (except for `\0` which is not considered to be
a "possible" symbol). Hence dot is the alphabet ∑ and `\0` is assumed to be 
"some symbol outside of alphabet", which often comes in handy. 

The type `.*` is the top type. Every string belongs to it. Conversely
there is also the bottom type, denoted with `#`. No string belongs to `#` (not even `\0`).
Notice that strings containing `\0` live completely outside of this type hierarchy.   
    
    function2 = 'this function has no type, hence it defaults to .* ⨯ .*'
    
    function3 <: .* && .*
    function3 = .* : 'ala'
    
    function4 <: [a-z] && .*
    function4 = . : 'test'
 
 It's possible to assign multiple types to a single function 
 (hence you can observe that the type system is polymorphic).
        
    
    multiple_types <: .* && .*
    multiple_types <: 'abc' && .*
    multiple_types <: [a-z] 'bc' && .*
    multiple_types <: [b-x][b-x][b-x] && .*
    multiple_types <: [b-x][b-x][b-x] && ''
    multiple_types = [a-z][a-x][b-z]
    
    //There exists a lattice of types. 

Apart from `&&` there is also one more type. 

    f <: [a-z] -> .*
    
The symbol `->` stands for function type. Alternative pretty notation is

    f <: [a-z] → .*
    
The most important difference between `&&` and `->` is in polymorphic variance. In order to understand it better you should remember that `&&` and `->` are not really types by themselves, but rather type constructors. Just like `|` takes two arguments and performs their union, the same holds here. `->` takes two arguments, which are types, and returns a third type (higher-order functions are not allowed, unfortunately). Consider a Java-like example of `Pair<Cat,Dog> <: Pair<Animal,Animal>` and `Function<Animal,Dog> <: Function<Cat,Animal>`. In order words, `&&` is covariant on both parameters, whereas `->` is contravariant on th left parameter and convariant on the right one. **Note:** typechecking `&&` is quadratic, whereas typechecking `->` is an exponential operation. However most of the time it should not be any problem.

Notice that if `f` is of type `A -> B` then any
string accepted by `A` is also accepted by `f` but the opposite doesn't hold.
Not all strings accepted by `f` are necessarily accepted by `A`.
When it comes to output, the opposite holds. Every string returned/printed
by `f` is accepted by `B` but not every string accepted by `B` can be
 printed by `f`. 

You can reuse functions as types for others. It's a very powerful feature.

    x = 'abc':'01f' 1 | 're':'2' 2
    y <: x -> .*
    y = 'abc':'43' | 're':'kk'
    //everything that is accepted by x must
    //also be accepted by y
    
    z <: 'abc' -> x
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

    plain_regex <: # -> ''
    plain_regex = 'abc' | 'red'*
    
The symbol `#` stands for empty type (`Void` in Java/Haskell). No string belongs to `#` (not even `\0`).

Another interesting property is that you can easily check whether automaton
is total (that is, every string is mapped to some other string and no input is rejected)

    total <: .* -> 'a'
    total = .* : 'a'

In order to check if automaton is empty (that is, all inputs are rejected) you
can assert the following

    empty <: .* && #
    empty = .* : 'a' #
    
or
    
    empty <: # && .*
    empty = .* : 'a' #
    
because Cartesian product of any set with an empty set still yields empty set. 
        
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
 
     mirror <: # -> [a-z] // is true
     mirror <: # -> [a-b] // is false
     mirror <: # -> 'a'|'b'|...|'z' // is true
     mirror <: # -> . // is true
     mirror <: # -> 'a' // is false
     
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
    
## Composition

While Glushkov's construction has no means of supporting transducer composition,
Solomonoff provides this feature in a lazy manner. In a sense, composition becomes
a special language in itself. Solomonoff also borrows from Hoare logic to allow for
formally verified correctness of all compositions.

In order to define a composed function, you have to prefix its name with `@`.

    @f = 'a':'b';
    
Unlike in normal functions, the composed functions have to be finalized with `;`.
This syntax was inspired by Hoare triples. Here `'a':'b'` becomes a statement with
side-effects. You might alternatively think of it as if it was

    String f(String input){
        input = input.replace("a","b");
        return input;    
    }
    
There is one string that is mutated by consecutive transducers. A more complex example might be

    @f = 'a':'b' ; 'b' : 'c ;

which would roughly have the same effect as

    String f(String input){
        input = input.replace("a","b");
        input = input.replace("b","c");
        return input;    
    } 
    
Now the best part is use of assertions similar to Hoare pre- and postconditions.

    @f = 'a':'b' {'b'} 'b' : 'c ;

You might think of it in a sense as

    String f(String input){
        input = input.replace("a","b");
        assert input.belongsTo("b") ;
        input = input.replace("b","c");
        return input;    
    } 

You can use even more complex expressions like

    @f = 'a':'b' | 'h':'i' 
         {'b'|'i'} 
         'b':'c' | 'i':'j' 
         {'c'|'j'} 
         'c':'d' | 'j':'k';

Note that while the evaluation of transducers is lazy, the Hoare assertions
are fully checked at compile-time and then erased. Therefore they don't
add any performance penalty at runtime. Note that the above example translates exactly to

    statement1 <: # -> 'b'|'i'
    statement1 = 'a':'b' | 'h':'i'
    
    statement2 <: 'b'|'i' ->  'c'|'j'
    statement2 = 'b':'c' | 'i':'j'
    
    statement3 <: 'c'|'j' -> .*
    statement3 = 'c':'d' | 'j':'k'
    
    @f =  statement1 ; statement2 ; statement3

The entire syntax of Hoare-triples is a essentially not much more than syntactic
sugar. Note that it's possible to not only add Hoare assertions but also Hoare
preconditions and postconditions.


    @f = {'a' | 'h'} //precondition
         'a':'b' | 'h':'i' 
         {'b'|'i'}  //assertion
         'b':'c' | 'i':'j' 
         {'c'|'j'} //assertion
         'c':'d' | 'j':'k'
         {'d' | 'k'} //postcondition
         
         
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