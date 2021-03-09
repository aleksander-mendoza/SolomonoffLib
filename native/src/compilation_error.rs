use v::V;
use p::P;

pub enum CompErr{
    DuplicateFunction(/*first definition*/V,/*second definition*/V,/*name*/String),
    Parse(/*position*/V,/*message*/String),
    PipelineSizeMismatch(/*position*/V,/*expected*/usize,/*actual*/usize),
    Typecheck(/*func position*/V,/*type position*/V,/*name*/String),
    Nondeterminism(/*nondeterministicStatePos1*/V,/*nondeterministicStatePos2*/V,/*name*/String),
    KleeneNondeterminism(/*position*/V,/*epsilon*/P),
    WeightConflictingToThirdState(/*position*/V),
    WeightConflictingFinal(/*position*/V),
    UndefinedExternalFunc(/*position*/V,/*name*/String),
    AmbiguousDictionary(/*position*/V,/*input*/Vec<u32>,/*output1*/Vec<u32>,/*output2*/Vec<u32>)
}