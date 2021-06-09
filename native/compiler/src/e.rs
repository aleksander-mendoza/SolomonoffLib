use int_seq::{A, IntSeq, REFLECT, decrement};
use p::{P, PartialEdge, W};

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
    fn destruct(self) -> (W, IntSeq) {
        self.partial.destruct()
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
        assert!(symbol > REFLECT);
        E::new_neutral(decrement(symbol), symbol)
    }
    pub fn new_from_symbol(symbol: A, partial:P) -> Self {
        assert!(symbol > REFLECT);
        E::new(decrement(symbol), symbol, partial)
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