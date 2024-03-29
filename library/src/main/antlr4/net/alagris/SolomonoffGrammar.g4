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
	nonfunctional='nonfunc'? exponential='!!'? ID '=' mealy_union  # FuncDef
	| ID ('<:'|'⊂') in = mealy_union (type=('&&'|'⨯'|'->'|'→') out = mealy_union)?   # TypeJudgement
	| '@' ID '='  pipeline_or   # PipelineDef
;

////////////////////////////
////// lazy pipelines
////////////////////////////

pipeline_or :
    (pipeline_compose '||')* pipeline_compose #PipelineOr
;

pipeline_compose:
    (pipeline_atomic ';')* pipeline_atomic #PipelineCompose
;
pipeline_atomic:
    nonfunctional='nonfunc'? tran=mealy_union  # PipelineMealy
    | runtime='runtime'? 'assert' assertion=mealy_union # PipelineAssertion
    | '@' ID func_arg #PipelineExternal
    | '@' ID #PipelineReuse
    | '@(' pipeline_or ')' # PipelineNested
    | '{' (Num '->' pipeline_or)+  '}' # PipelineSubmatch
;

////////////////////////////
////// regular expressions with output (product) and weights
////////////////////////////
mealy_union
:
	(weights mealy_concat bar='|') * weights mealy_concat # MealyUnion
;

mealy_concat
:
	 (mealy_Kleene_closure weights '∙'?)* mealy_Kleene_closure weights  # MealyConcat
;

mealy_Kleene_closure
:
	mealy_atomic (weights (star='*' | plus='+' | optional='?'))? # MealyKleeneClosure
;


mealy_atomic
:
	colon=':'? StringLiteral # MealyAtomicLiteral
	| Range # MealyAtomicRange
	| CodepointRange # MealyAtomicCodepointRange
	| colon=':'? Codepoint # MealyAtomicCodepoint
	| exponential='!!'? ID # MealyAtomicVarID
	| funcName=ID func_arg # MealyAtomicExternal
	| '(' mealy_union ')' # MealyAtomicNested
	| Num '{' mealy_union '}' # MealyAtomicSubmatchGroup
;

func_arg: ('&[' ID ']' | '![' mealy_union ']' | '!(' informant ')')+;

informant : ((StringLiteral (':' (StringLiteral | ID | Range) )? ) (',' StringLiteral (':' (StringLiteral | ID | Range) )? )*)?
;

weights: Num*;

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

Num
:
	'-'? [0-9]+
;

Range
:
	'[' ( ('\\' . | ~('['|']'|'\\'|'-'))'-'('\\' . | ~('['|']'|'\\'|'-')) | ('\\' . | ~('['|']'|'\\'|'-')) )* ']'
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
	'<['([0-9]+'-'[0-9]+|[0-9]+) (' '+ ([0-9]+'-'[0-9]+|[0-9]+) )* ']>'
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