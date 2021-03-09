use p::P;

#[derive(Clone)]
pub struct E {
    from_exclusive: u32,
    to_inclusive: u32,
    partial: P,
}

impl E {
    pub fn from_exclusive(&self) -> u32 {
        self.from_exclusive
    }
    pub fn to_inclusive(&self) -> u32 {
        self.to_inclusive
    }
    pub fn partial(&self) -> &P {
        &self.partial
    }
    pub fn partial_mut(&mut self) -> &mut P {
        &mut self.partial
    }
    pub fn new(from_exclusive: u32, to_inclusive: u32, partial: P) -> Self {
        E { from_exclusive, to_inclusive, partial }
    }
    pub fn new_neutral(from_exclusive: u32, to_inclusive: u32) -> Self {
        E::new(from_exclusive, to_inclusive, P::neutral())
    }
    pub fn new_neutral_from_symbol(symbol: u32) -> Self {
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