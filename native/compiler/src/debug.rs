use std::fmt::{Debug, Formatter};
use g::G;
use std::collections::HashMap;
use n::N;
use e::{E, FullEdge};
use p::{P, PartialEdge};
use ghost::Ghost;

pub struct GDebug<'a,'b:'a>{
    g:&'a G,
    ghost:&'b Ghost
}
impl G{
    pub fn debug<'a,'b:'a>(&'a self, ghost: &'b Ghost) ->GDebug<'a,'b> {
        GDebug{g:self,ghost}
    }
}
impl <'a,'b:'a> Debug for GDebug<'a,'b> {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        let mut g = f.debug_struct("G");
        let s1: Vec<String> = self.g.incoming().iter().map(|(e, v)| format!("{:?}->{:?}", e, v)).collect();
        g.field(&"in", &s1);
        let s2: Vec<String> = self.g.outgoing().iter().map(|(v, p)| format!("{:?}->{:?}", v, p)).collect();
        g.field(&"out", &s2);
        g.field(&"eps", self.g.epsilon());
        let map: HashMap<*mut N, &N> = self.g.collect_whole_graph(self.ghost).iter().map(|ptr| (*ptr, unsafe { &**ptr })).collect();
        g.field(&"graph", &map);
        g.finish()
    }
}

impl Debug for N {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        let mut m = f.debug_map();
        m.entry(&"meta", self.get_meta());
        for (e, v) in self.get_outgoing() {
            m.entry(e, v);
        }
        m.finish()
    }
}

impl Debug for E {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "[{}-{}]{:?}", self.from_exclusive(), self.to_inclusive(), self.partial())
    }
}

impl Debug for P {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, ":{} {:?}", self.weight(), self.output())
    }
}
