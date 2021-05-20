use e::{E, FullEdge};
use int_seq::{A, IntSeq, EPSILON};
use string_interner::StringInterner;

pub type W = i32;

pub trait PartialEdge {
    fn weight(&self) -> W;
    fn output(&self) -> &IntSeq;
    fn destruct(self) -> (W, IntSeq);
}

pub fn is_neutral<P: PartialEdge>(p: &P) -> bool {
    p.weight() == 0 && p.output().is_empty()
}

#[derive(Clone)]
pub struct P {
    weight: W,
    output: IntSeq,
}

impl PartialEdge for P {
    fn weight(&self) -> W {
        self.weight
    }
    fn output(&self) -> &IntSeq {
        &self.output
    }
    fn destruct(self) -> (W, IntSeq) {
        let P { weight, output } = self;
        (weight, output)
    }
}

impl P {
    pub fn neutral() -> P {
        P { weight: 0, output: EPSILON }
    }
    pub fn new(weight: W, output: IntSeq) -> P {
        P { weight, output }
    }
    pub fn multiply(&self, rhs: &P) -> P {
        P { weight: self.weight + rhs.weight, output: self.output() + rhs.output() }
    }
    pub fn multiply_in_place(&mut self, rhs: &P) {
        self.weight += rhs.weight;
        self.output += rhs.output();
    }
    pub fn left_action(&self, e: &E) -> E {
        E::new(e.from_exclusive(), e.to_inclusive(), self.multiply(e.partial()))
    }

}