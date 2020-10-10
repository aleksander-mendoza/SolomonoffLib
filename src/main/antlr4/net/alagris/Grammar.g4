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
	funcs exponential='!!'? ID '=' mealy_union  # FuncDef
	| funcs ID ('<:'|'⊂') in = mealy_union type=('&&'|'⨯'|'->'|'→') out = mealy_union   # TypeJudgement
	| funcs '@'ID '='  pipeline   # HoarePipeline
	| # EndFuncs
;


pipeline :
    pipeline tran=mealy_union ('{' hoare=mealy_union '}' | ';') # PipelineMealy
    | pipeline '@' ID '!' '(' informant? ')' ('{' hoare=mealy_union '}' | ';') #PipelineExternal
    | pipeline '@' ID ';' #PipelineNested
    | ('{' hoare=mealy_union '}')? # PipelineBegin
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
	mealy_concat '∙'? mealy_Kleene_closure Weight?  # MealyMoreConcat
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
	| exponential='!!'? ID # MealyAtomicVarID
	| ID '!' '(' informant? ')' # MealyAtomicExternal
	| '(' mealy_union ')' # MealyAtomicNested
;

informant : (StringLiteral (':' (StringLiteral | ID) )? ) (',' StringLiteral (':' (StringLiteral | ID) )? )*
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