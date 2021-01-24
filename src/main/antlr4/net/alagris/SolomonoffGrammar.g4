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
	| pipeline_id '='  pipeline_compose   # PipelineDef
;

////////////////////////////
////// lazy pipelines
////////////////////////////

pipeline_id:
    Num? '@' ID
;

pipeline_compose:
    (pipeline_or ';')* pipeline_or #PipelineCompose
;
pipeline_or :
    (pipeline_and '||')* pipeline_and #PipelineOr
;
pipeline_and:
    (pipeline_atomic '&&')*  pipeline_atomic #PipelineAnd
;
pipeline_atomic:
    nonfunctional='nonfunc'? tran=mealy_union  # PipelineMealy
    | runtime='runtime'? 'assert' assertion=mealy_union # PipelineAssertion
    | '@' ID '!' '(' informant ')' #PipelineExternal
    | pipeline_id #PipelineReuse
    | Num? '@(' pipeline_compose ')' # PipelineNested
    | 'split' pipeline_compose 'on' Num? (inSepStr=StringLiteral|inSepCp=Codepoint) ':' (outSepStr=StringLiteral|outSepCp=Codepoint) #PipelineSplit
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
	| ID '!(' informant ')' # MealyAtomicExternal
	| '(' mealy_union ')' # MealyAtomicNested
	| ID  '![' (mealy_union (',' mealy_union)*)? ']' #MealyAtomicExternalOperation
;

informant : ((StringLiteral (':' (StringLiteral | ID) )? ) (',' StringLiteral (':' (StringLiteral | ID) )? )*)?
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
	'[' ( ('\\' . | ~('['|']'|'\\'|'-'))'-'('\\' . | ~('['|']'|'\\'|'-')) | ('\\' . | ~('['|']'|'\\'|'-')) )+ ']'
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