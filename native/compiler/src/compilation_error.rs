use v::V;
use p::P;
use lalrpop_util::ParseError;
use lalrpop_util::lexer::Token;
use int_seq::A;

#[derive(Debug, Clone, Eq, PartialEq)]
pub enum CompErr {
    DuplicateFunction(/*first definition*/V, /*second definition*/V, /*name*/String),
    DuplicatePipeline(/*first definition*/V, /*second definition*/V, /*name*/String),
    UndefinedFunction(/*position*/V, /*name*/String),
    UndefinedPipeline(/*position*/V, /*name*/String),
    DuplicateSubmtach(/*position*/V, /*submatch index*/A),
    SubmatchGroupOutOfBounds(/*position*/V, /*submatch index*/A),
    NonexistentTransducer(/*name*/String),
    Parse(/*position*/V, /*message*/String),
    PipelineSizeMismatch(/*position*/V, /*expected*/usize, /*actual*/usize),
    Typecheck(/*func position*/V, /*type position*/V, /*name*/String),
    Nondeterminism(/*nondeterministicStatePos1*/V, /*nondeterministicStatePos2*/V, /*name*/String),
    KleeneNondeterminism(/*position*/V, /*epsilon*/P),
    WeightConflictingToThirdState(/*position*/V),
    WeightConflictingFinal(/*position*/V),
    UndefinedExternalFunc(/*position*/V, /*name*/String),
    IllegalInformantIdOutput(/*position*/V, /*name*/String),
    NonEmptyInformantRangeOutput(/*position*/V),
    IncorrectFunctionArguments(/*position*/V, /*msg*/String),
    IncorrectCommandArguments( /*msg*/String),
    UnrecognisedCommand(/*cmd*/String),
    AmbiguousDictionary(/*position*/V, /*input*/Vec<u32>, /*output1*/Vec<u32>, /*output2*/Vec<u32>),
}
