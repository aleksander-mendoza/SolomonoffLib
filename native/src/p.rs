use e::E;

#[derive(Clone)]
pub struct P {
    weight: u32,
    output: Vec<u32>,
}

fn concat_vec<X: Copy>(v1: &Vec<X>, v2: &Vec<X>) -> Vec<X> {
    let mut v3 = Vec::with_capacity(v1.len() + v2.len());
    v3.extend(v1);
    v3.extend(v2);
    v3
}

impl P {
    pub fn is_neutral(&self)-> bool{
        self.weight==0&&self.output.is_empty()
    }
    pub fn weight(&self)-> u32{
        self.weight
    }
    pub fn output(&self)-> &Vec<u32>{
        &self.output
    }
    pub fn neutral() -> P {
        P { weight: 0, output: vec![] }
    }
    pub fn new(weight: u32, output: Vec<u32>) -> P {
        P { weight, output }
    }
    pub fn multiply(&self, rhs: &P) -> P {
        P { weight: self.weight + rhs.weight, output: concat_vec(self.output(), rhs.output()) }
    }
    pub fn multiply_in_place(&mut self, rhs: &P) {
        self.weight += rhs.weight;
        self.output.extend(rhs.output());
    }
    pub fn left_action(&self, e: &E) -> E {
        E::new(e.from_exclusive(), e.to_inclusive(), self.multiply(e.partial()))
    }
}