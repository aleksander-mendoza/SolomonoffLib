use std::collections::HashMap;
use func_arg::Informant;
use int_seq::{A, IntSeq};
use std::collections::hash_map::Entry;
use std::borrow::Borrow;

/**Index type used by inference algorithms. Just one byte is more than enough. For
practical reasons, the number should usually be no greater than 20 or 30.
Inferring automata for larger alphabets would be impractical anyway.*/
pub type I = u8;

pub trait Alphabet {
    fn embed(&self,c: A) -> I;
    fn retrieve(&self,idx: I) -> A;
    fn len(&self)->usize;
}

pub struct IntEmbedding{
    to_original:Vec<A>,
    to_embedding:HashMap<A,I>
}

impl IntEmbedding{
    pub fn new(chars:Vec<A>)->Self{
        let to_embedding:HashMap<A,I> = chars.iter().cloned().enumerate().map(|(i, c)|(c as A, i as I)).collect();
        Self{to_original:chars,to_embedding}
    }

    pub fn for_informant<I,R:Borrow<(IntSeq,Option<IntSeq>)>>(informant:& mut I)->Self where I: Iterator<Item=R>{
        let mut to_embedding = HashMap::new();
        let mut max_idx = 0u8;
        let mut to_original = Vec::new();
        for i in informant{
            let (i,o) = i.borrow();
            for c in i.borrow(){
                match to_embedding.entry(c){
                    Entry::Occupied(_) => {}
                    Entry::Vacant(e) => {
                        e.insert(max_idx);
                        to_original.push(c);
                        max_idx+=1;
                    }
                }
            }
        }
        assert_eq!(to_original.len(),to_embedding.len());
        Self{to_original,to_embedding}
    }


    pub fn for_strings<I>(informant:& mut I)->Self where I: Iterator<Item=IntSeq>{
        let mut to_embedding = HashMap::new();
        let mut max_idx = 0u8;
        let mut to_original = Vec::new();
        for i in informant{
            for c in i.iter(){
                match to_embedding.entry(c){
                    Entry::Occupied(_) => {}
                    Entry::Vacant(e) => {
                        e.insert(max_idx);
                        to_original.push(c);
                        max_idx+=1;
                    }
                }
            }
        }
        assert_eq!(to_original.len(),to_embedding.len());
        Self{to_original,to_embedding}
    }
}

impl Alphabet for IntEmbedding{
    fn embed(&self,c: A) -> I {
        *self.to_embedding.get(&c).unwrap()
    }

    fn retrieve(&self,idx: I) -> A {
        self.to_original[idx as usize]
    }

    fn len(&self) -> usize {
        self.to_original.len()
    }
}