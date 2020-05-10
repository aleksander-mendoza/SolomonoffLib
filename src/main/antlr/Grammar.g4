grammar Grammar;

/**
  * The simplest way to do it is by creating 3 languages:
  * - language of functions
  * - language of Mealy expressions
  * - language of FSA expressions
  * 
  * 
  * 
  */
start
:
	funcs EOF
;

funcs
:
	funcs func_def ';' # MoreFuncs
	| # EndFuncs
;

func_def
:
	ID '(' params ')' '=' mealy_union
;

params
:
	params ID # MoreParams
	| # EndParams
;

mealy_union
:
	Weight? mealy_concat # EndUnion
	| Weight? mealy_concat '|' mealy_union # MoreUnion
;

mealy_concat
:
	mealy_Kleene_closure Weight? mealy_concat # MoreConcat
	| mealy_Kleene_closure Weight? # EndConcat
;

mealy_Kleene_closure
:
	mealy_prod Weight? '*' # KleeneClosure
	| mealy_prod # NoKleeneClosure
;

mealy_prod
:
	mealy_atomic ':' StringLiteral # Product
	| mealy_atomic # EpsilonProduct
;

mealy_atomic
:
	StringLiteral # AtomicLiteral
	| Range  #AtomicRange
	| ID # AtomicVarID
	| '(' mealy_union ')' # AtomicNested
;

Weight: '-'? [0-9]+;

Range: '[' '\\'? . '-' '\\'?. ']';

ID
:
	[a-zA-Z_] [a-zA-Z_0-9]*
;

StringLiteral
:
	UnterminatedStringLiteral '"'
;

UnterminatedStringLiteral
:
	'"'
	(
		~["\\\r\n]
		| '\\'
		(
			.
			| EOF
		)
	)*
;

WS
:
	(
		' '
		| '\t'
		| '\n'
	)+ -> channel ( HIDDEN )
;