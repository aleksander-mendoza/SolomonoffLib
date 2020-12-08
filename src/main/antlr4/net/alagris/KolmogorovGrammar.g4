grammar KolmogorovGrammar;



////////////////////////////
////// structural features of language (function and type declarations)
////////////////////////////
start
:
	funcs EOF
;

funcs
:
	(ID '=' mealy_union)* 
;


////////////////////////////
////// regular expressions with output (product) and weights
////////////////////////////

mealy_compose
:
	(mealy_diff compose='@') *  mealy_diff # MealyCompose
;

mealy_diff
:
	(mealy_union diff='-')? mealy_union # MealyDiff
;

mealy_union
:
	(mealy_concat bar='|') * mealy_concat # MealyUnion
;

mealy_concat
:
	(mealy_Kleene_closure '∙'?)* mealy_Kleene_closure  # MealyConcat
;

mealy_Kleene_closure
:
	mealy_atomic(star='*' | plus='+' | optional='?' | '^' power=Num | '^<'repeatLessThan=Num | ('^<='|'^≤') repeatLessEqThan=Num | | ) # MealyKleeneClosure
;


mealy_atomic
:
	StringLiteral 
	| Range 
	| CodepointRange
	| Codepoint 
	| ID 
	| ':' prod=fsa_atomic 
	| '(' nested=mealy_compose ')' 
;


fsa_diff
:
	(fsa_union diff='-')? fsa_union #FsaDiff
;

fsa_union
:
	(fsa_concat bar='|') * fsa_concat # FsaUnion
;

fsa_concat
:
	(fsa_Kleene_closure '∙'?)* fsa_Kleene_closure  # FsaConcat
;

fsa_Kleene_closure
:
	fsa_atomic (star='*' | plus='+' | optional='?' | '^' power=Num | ) # FsaKleeneClosure
;

fsa_atomic
:
	StringLiteral
	| Range 
	| CodepointRange 
	| Codepoint 
	| ID 
	| '(' nested=fsa_diff ')'
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

Num
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
	'\''
	(
		~['\\\r\n]
		| '\\'
		(
			.
			| EOF
		)
	)* '\''
;


WS
:
	(
		' '
		| '\t'
		| '\n'
	)+ -> channel ( HIDDEN )
;