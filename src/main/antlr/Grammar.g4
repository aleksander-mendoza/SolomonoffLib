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
	| funcs '@'ID '='  (mealy_union ';' | '{' mealy_union '}' | ID ';!'(StringLiteral (',' StringLiteral)*)? | '@' ID )+   # HoarePipeline
	| # EndFuncs
;


////////////////////////////
////// regular expressions with output (product) and weights
////////////////////////////
mealy_union
:
	(Weight? mealy_concat bar='|') * Weight? mealy_concat # MealyUnion
;

mealy_concat
:
	mealy_concat mealy_Kleene_closure Weight?  # MealyMoreConcat
	| mealy_Kleene_closure Weight? # MealyEndConcat
;

mealy_Kleene_closure
:
	mealy_prod Weight? (star='*' | plus='+' | optional='?') # MealyKleeneClosure
	| mealy_prod # MealyNoKleeneClosure
;

mealy_prod
:
	mealy_atomic colon=':' StringLiteral # MealyProduct
	| mealy_atomic colon=':' Codepoint # MealyProductCodepoints
	| mealy_atomic # MealyEpsilonProduct
;

mealy_atomic
:
	StringLiteral # MealyAtomicLiteral
	| Range # MealyAtomicRange
	| Codepoint # MealyAtomicCodepoint
	| ID # MealyAtomicVarID
	| ID '!' (StringLiteral (',' StringLiteral)*)? # MealyAtomicText
	| ID ':!' (StringLiteral':' StringLiteral ( ',' StringLiteral':'StringLiteral) * )? # MealyAtomicInformant
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


ID
:
	[#.a-zA-Z_] [#.a-zA-Z_0-9]*
;

Codepoint
:
	'<' ( ( ([0-9]+|[0-9]+'-'[0-9]+)' ')*([0-9]+|[0-9]+'-'[0-9]+) )? '>'
;


StringLiteral
:
	UnterminatedStringLiteral '\''
;

UnterminatedStringLiteral
:
	'\''
	(
		~['\\\r\n]
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