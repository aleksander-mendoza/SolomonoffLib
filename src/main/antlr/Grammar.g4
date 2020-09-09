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
	| funcs ID '::=' plain_union  # TypeDef
	| funcs ID '::' in = plain_union '->' out = plain_union   # TypeJudgement
	| # EndFuncs
;


////////////////////////////
////// regular expressions with output (product) and weights
////////////////////////////
mealy_union
:
	Weight? mealy_concat # MealyEndUnion
	| Weight? mealy_concat '|' mealy_union # MealyMoreUnion
;

mealy_concat
:
	mealy_Kleene_closure Weight? mealy_concat # MealyMoreConcat
	| mealy_Kleene_closure Weight? # MealyEndConcat
;

mealy_Kleene_closure
:
	mealy_prod Weight? '*' # MealyKleeneClosure
	| mealy_prod # MealyNoKleeneClosure
;

mealy_prod
:
	mealy_atomic ':' StringLiteral # MealyProduct
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
////// duplicated the same rules as above but for 
////// plain regex (without weights/products) 
////////////////////////////

plain_union
:
	plain_concat # PlainEndUnion
	| plain_concat '|' plain_union # PlainMoreUnion
;

plain_concat
:
	plain_Kleene_closure plain_concat # PlainMoreConcat
	| plain_Kleene_closure # PlainEndConcat
;

plain_Kleene_closure
:
	plain_atomic '*' # PlainKleeneClosure
	| plain_atomic # PlainNoKleeneClosure
;

plain_atomic
:
	StringLiteral # PlainAtomicLiteral
	| Range # PlainAtomicRange
	| Codepoint # PlainAtomicCodepoint
	| ID # PlainAtomicVarID
	| '(' plain_union ')' # PlainAtomicNested
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