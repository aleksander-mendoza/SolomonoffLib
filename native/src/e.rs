use p::{P, PartialEdge, W};
use int_seq::{IntSeq, A};

pub trait FullEdge{
    fn from_exclusive(&self) -> A;
    fn to_inclusive(&self) -> A;
}

#[derive(Clone)]
pub struct E {
    from_exclusive: A,
    to_inclusive: A,
    partial: P,
}

impl FullEdge for E {
    fn from_exclusive(&self) -> A {
        self.from_exclusive
    }
    fn to_inclusive(&self) -> A {
        self.to_inclusive
    }
}

impl PartialEdge for E {
    fn weight(&self) -> W {
        self.partial.weight()
    }
    fn output(&self) -> &IntSeq {
        &self.partial.output()
    }
}

impl E{
    pub fn partial(&self) -> &P {
        &self.partial
    }
    pub fn partial_mut(&mut self) -> &mut P {
        &mut self.partial
    }
    pub fn new(from_exclusive: A, to_inclusive: A, partial: P) -> Self {
        E { from_exclusive, to_inclusive, partial }
    }
    pub fn new_neutral(from_exclusive: A, to_inclusive: A) -> Self {
        E::new(from_exclusive, to_inclusive, P::neutral())
    }
    pub fn new_neutral_from_symbol(symbol: A) -> Self {
        assert!(symbol > 0);
        E::new_neutral(symbol - 1, symbol)
    }
    pub fn right_action(&self, p: &P) -> E {
        E::new(self.from_exclusive, self.to_inclusive, self.partial.multiply(p))
    }
}

impl PartialEq for E {
    fn eq(&self, other: &Self) -> bool {
        self as *const E == other as *const E
    }
}

impl Eq for E {}