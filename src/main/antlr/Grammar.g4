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
	| funcs ID ('<:'|'⊂') in = mealy_union type=('&&'|'⨯'|'->'|'→') out = mealy_union   # TypeJudgement
	| funcs '@'ID '='  pipeline   # HoarePipeline
	| # EndFuncs
;


pipeline :
    pipeline mealy_union ';' # PipelineMealy
    | pipeline '{' mealy_union '}' #PipelineHoare
    | pipeline '@' ID '!' '(' informant? ')' #PipelineExternal
    | pipeline '@' ID #PipelineNested
    | # PipelineBegin
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
	| ID # MealyAtomicVarID
	| ID '!' '(' informant? ')' # MealyAtomicExternal
	| '(' mealy_union ')' # MealyAtomicNested
;

informant :
   informant ',' in=StringLiteral #InformantEpsOutput
   | informant ',' in=StringLiteral ':' out=StringLiteral #InformantOutput
   | informant ',' in=StringLiteral ':' out=ID #InformantHole
   | in=StringLiteral #InformantBeginEpsOutput
   | in=StringLiteral ':' out=StringLiteral #InformantBeginOutput
   | in=StringLiteral ':' out=ID #InformantBeginHole
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