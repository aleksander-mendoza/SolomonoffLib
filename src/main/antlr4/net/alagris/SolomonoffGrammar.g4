grammar SolomonoffGrammar;



////////////////////////////
////// structural features of language (function and type declarations)
////////////////////////////
start
:
	funcs EOF
;

repl
:
	statement EOF
;

funcs
:
	funcs statement
	| 
;

statement: 
	exponential='!!'? ID '=' mealy_union  # FuncDef
	| ID ('<:'|'⊂') in = mealy_union (type=('&&'|'⨯'|'->'|'→') out = mealy_union)?   # TypeJudgement
	| '@'ID '='  pipeline   # HoarePipeline
	| 'include' StringLiteral ('promise' ID)? # IncludeFile
	| 'wait' ID # WaitForFile
;


pipeline :
    pipeline tran=mealy_union ('{' hoare=mealy_union '}' | ';') # PipelineMealy
    | pipeline '@' ID '!' '(' informant ')' ('{' hoare=mealy_union '}' | ';') #PipelineExternal
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
	 (mealy_Kleene_closure Weight? '∙'?)* mealy_Kleene_closure Weight?  # MealyConcat
;

mealy_Kleene_closure
:
	mealy_atomic (Weight? (star='*' | plus='+' | optional='?'))? # MealyKleeneClosure
;


mealy_atomic
:
	colon=':'? StringLiteral # MealyAtomicLiteral
	| Range # MealyAtomicRange
	| CodepointRange # MealyAtomicCodepointRange
	| colon=':'? Codepoint # MealyAtomicCodepoint
	| exponential='!!'? ID # MealyAtomicVarID
	| ID '!' '(' informant ')' # MealyAtomicExternal
	| '(' mealy_union ')' # MealyAtomicNested
	| ID '[' (mealy_union (',' mealy_union)*)? ']' #MealyAtomicExternalOperation
;

informant : ((StringLiteral (':' (StringLiteral | ID) )? ) (',' StringLiteral (':' (StringLiteral | ID) )? )*)?
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
	[#.a-zA-Zα-ωΑ-Ω∅_] [#.a-zA-Zα-ωΑ-Ω∅_0-9]*
;

Codepoint
:
	'<' (([0-9]+' ')*[0-9]+)? '>'
;

CodepointRange
:
	'<' [0-9]+'-'[0-9]+ '>'
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