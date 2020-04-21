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
	funcs func_def ';'  # MoreFuncs
	| # EndFuncs
;

func_def
:
	ID '(' params ')' '=' mealy_union
;

params
:
	params ID  # MoreParams
	| # EndParams
;

mealy_union
:
	mealy_concat # EndUnion
	| mealy_concat '|' mealy_union # MoreUnion
;

mealy_concat
:
	mealy_Kleene_closure mealy_concat # MoreConcat
	| mealy_Kleene_closure #EndConcat
;

mealy_Kleene_closure
:
	'(' mealy_atomic ')' '*' #KleeneClosure
	| mealy_atomic #NoKleeneClosure
;

mealy_atomic
:
	fsa ':' StringLiteral # AtomicFsa
	| ID # AtomicVarID
	| '(' mealy_union ')' # AtomicNested
;

fsa
:
	StringLiteral # FsaLiteral
	| '(' fsa_union ')' # FsaNested
;

fsa_union
:
	fsa_concat '|' fsa_union #FsaMoreUnion
	| fsa_concat #FsaEndUnion
;

fsa_concat
:
	fsa_Kleene_cosure fsa_concat #FsaMoreConcat
	| fsa_Kleene_cosure #FsaEndConcat
;

fsa_Kleene_cosure
:
	fsa '*' #FsaKleeneClosure
	| fsa #FsaNoKleeneClosure
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