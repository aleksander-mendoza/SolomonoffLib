grammar Grammar;

/**
  * The simplest way to do it is by creating 3 languages:
  * - language of functions
  * - language of Mealy expressions
  * - language of FSA expressions
  * 
  * 
  * 
  */
start
:
	funcs EOF
;

funcs
:
	funcs ID '(' params ')' '=' mealy_union ';' # FuncDef
	| funcs ID '=' Alph ';' # AlphDef
	| funcs ID ':' type ';' # TypeDef
	| funcs 'struct' ID '{' struct_def '}' # StructDef
	| # EndFuncs
;

struct_def
:
	(m='main')? id=ID ':' alph=ID # StructDefAlph
	| (m='main')? id=ID ':' alph=ID '*' # StructDefLang
	| struct_def ',' (m='main')? id=ID ':' alph=ID # StructDefAlphMore
	| struct_def ',' (m='main')? id=ID ':' alph=ID '*' # StructDefLangMore
;

type
:
	atomic_type # TypeAtomic
	| atomic_type '->' type # TypeFunc
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
	| mealy_atomic ':' '{' struct_literal_impl '}' # ProductStruct
	| mealy_atomic # EpsilonProduct
;

mealy_atomic
:
	StringLiteral # AtomicLiteral
	| Range # AtomicRange
	| ID # AtomicVarID
	| '(' mealy_union ')' # AtomicNested
	| '{' mealy_union '}' # AtomicStructLiteral
	| '{' struct_impl '}' # AtomicStruct
;

struct_impl
:
	ID '=' mealy_union # StructImpl
	| struct_impl ',' ID '=' mealy_union # StructImplMore
;

struct_literal_impl
:
	ID '=' StringLiteral # StructLiteralImpl
	| struct_literal_impl ',' ID '=' StringLiteral # StructLiteralImplMore
;

Weight
:
	'-'? [0-9]+
;

Range
:
	'[' '\\'? . '-' '\\'? . ']'
;

Alph
:
	UnterminatedAlph ']'
;

UnterminatedAlph
:
	'['
	(
		~[\]\\\r\n]
		| '\\'
		(
			.
			| EOF
		)
	)*
;

ID
:
	[a-zA-Z_] [a-zA-Z_0-9]*
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