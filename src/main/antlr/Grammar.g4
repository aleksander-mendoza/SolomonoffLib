grammar Grammar;



////////////////////////////
////// structural features of language (function and type declarations)
////////////////////////////
start
:
	funcs EOF
;

funcs
:
	funcs ID '=' mealy_union  # FuncDef
	| funcs ID '::' in = mealy_union '->' out = mealy_union   # TypeJudgement
	| # EndFuncs
;


////////////////////////////
////// regular expressions with output (product) and weights
////////////////////////////
mealy_union
:
	Weight? mealy_concat # MealyEndUnion
	| mealy_union bar='|' Weight? mealy_concat   # MealyMoreUnion
;

mealy_concat
:
	mealy_concat mealy_Kleene_closure Weight?  # MealyMoreConcat
	| mealy_Kleene_closure Weight? # MealyEndConcat
;

mealy_Kleene_closure
:
	mealy_prod Weight? star='*' # MealyKleeneClosure
	| mealy_prod # MealyNoKleeneClosure
;

mealy_prod
:
	mealy_atomic colon=':' StringLiteral # MealyProduct
	| mealy_atomic # MealyEpsilonProduct
;

mealy_atomic
:
	StringLiteral # MealyAtomicLiteral
	| Range # MealyAtomicRange
	| Codepoint # MealyAtomicCodepoint
	| ID # MealyAtomicVarID
	| '(' mealy_union ')' # MealyAtomicNested
;

////////////////////////////
////// terminal tokens
////////////////////////////

Comment
:
	'//' ~( '\r' | '\n' )* -> channel ( HIDDEN )
;

MultilineComment
:
	'/*' .*? '*/' -> channel ( HIDDEN )
;

Weight
:
	'-'? [0-9]+
;

Range
:
	'[' '\\'? . '-' '\\'? . ']'
;

Codepoint
:
	'<' [1-9] [0-9]* '>'
	| '<' [1-9] [0-9]* '-' [1-9] [0-9]* '>'
;

ID
:
	[#.a-zA-Z_] [#.a-zA-Z_0-9]*
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