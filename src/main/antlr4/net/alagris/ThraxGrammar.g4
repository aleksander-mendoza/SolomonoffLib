grammar ThraxGrammar;

////////////////////////////
////// structural features of language (function and type declarations)
////////////////////////////

start
:
	stmt_list
;

stmt_list
:
/*empty */
	| stmt stmt_list
;

stmt
:
	'export'? ID '=' fst_with_weight ';' # StmtVarDef
	| 'return' fst_with_weight ';' # StmtReturn
	| 'import' StringLiteral 'as' ID ';' # StmtImport
	| 'func' ID '[' func_arguments ']' '{' stmt_list '}' # StmtFunc
;

func_arguments
:
	(
		(
			ID ','
		)* ID
	)?
;

fst_with_weight
:
	fst_with_output AStringLiteral # FstWithWeight
	| fst_with_output # FstWithoutWeight
;

fst_with_output
:
	lhs=union_fst ':' rhs=union_fst # FstWithOutput
	| union_fst # FstWithoutOutput
;

// Union: L|R

union_fst
:
	composition_fst '|' union_fst # FstWithUnion
	| composition_fst # FstWithoutUnion
;

// Composition: A @ B

composition_fst
:
	composition_fst '@' difference_fst # FstWithComposition
	| difference_fst # FstWithoutComposition
;

// Difference: L-R

difference_fst
:
	difference_fst '-' concat_fst # FstWithDiff
	| concat_fst # FstWithoutDiff
;

// Concatenation: L R

concat_fst
:
	repetition_fst concat_fst #FstWithConcat
	| repetition_fst #FstWithoutConcat
;

// Repetition: R*, R+, R?, R{d,d}, R{d}

repetition_fst
:
	atomic_obj closure=('*'|'+'|'?')?  #FstWithKleene
	| atomic_obj ('{' from=Number ',' to=Number '}' | '{' times=Number '}')  #FstWithRange
;

atomic_obj
:
	StringLiteral #SQuoteString
	| DStringLiteral 
	(
		'.'
		(
			'byte'
			| 'utf8'
			| ID
			(
				'(' funccall_arguments ')'
			)?
		)
	)? #DQuoteString
	| ID #Var
	| ID '[' funccall_arguments ']' #FuncCall
	| '(' fst_with_weight ')' #Nested
;

funccall_arguments
:
	(('byte'| 'utf8'| fst_with_weight)(',' ('byte'| 'utf8'| fst_with_weight))*)?
;

////////////////////////////
////// terminal tokens
////////////////////////////

Comment
:
	'#' ~( '\r' | '\n' )* -> channel ( HIDDEN )
;

Number
:
	[0-9]+
;

ID
:
	[a-zA-Z_] [a-zA-Z_0-9]*
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

DStringLiteral
:
	UnterminatedDStringLiteral '"'
;

UnterminatedDStringLiteral
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

AStringLiteral
:
	'<'
	(
		~[>\\\r\n]
		| '\\'
		(
			.
			| EOF
		)
	)* '>'
;

WS
:
	(
		' '
		| '\t'
		| '\n'
	)+ -> channel ( HIDDEN )
;