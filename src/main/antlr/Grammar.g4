grammar Grammar;

start
:
	funcs EOF
;

funcs
:
	det = 'det'? ID '=' mealy_union ';' funcs # FuncDef
	| ID ':' type ';' funcs # TypeDef
	| # EndFuncs
;

type
:
	mealy_union # TypeLanguage
	| mealy_union '->' mealy_union # TypeRelation
;

atomic_type
:
	'(' type ')' # NestedType
	| ID # TypeVar
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
	| Range # AtomicRange
	| Codepoint # AtomicCodepoint
	| ID # AtomicVarID
	| '(' mealy_union ')' # AtomicNested
;

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