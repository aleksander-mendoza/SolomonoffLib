use ranged_graph::{RangedGraph, Trans, NonSink};
use std::fmt::{Formatter, Debug};
use int_seq::A;
use p::PartialEdge;

impl<Tr: Trans> RangedGraph<Tr> {
    pub fn serialise_symbol(s: A, fmt: &mut Formatter<'_>) -> std::fmt::Result {
        if let Some(c) = char::from_u32(s) {
            if c.is_ascii_graphic(){
                write!(fmt, "{}", c)
            }else {
                match c {
                    '\n' => write!(fmt, "\\n"),
                    '\r' => write!(fmt, "\\r"),
                    '\0' => write!(fmt, "\\0"),
                    '\t' => write!(fmt, "\\t"),
                    _ => write!(fmt, "{}", s)
                }
            }
        }else{
            write!(fmt, "{}", s)
        }
    }
    pub fn serialise_inclusive_range(from_inclusive: A, to_inclusive: A, fmt: &mut Formatter<'_>) -> std::fmt::Result {
        assert!(from_inclusive <= to_inclusive);
        if from_inclusive == to_inclusive {
            Self::serialise_symbol(from_inclusive,fmt)
        } else {
            Self::serialise_symbol(from_inclusive,fmt);
            write!(fmt, "-");
            Self::serialise_symbol(to_inclusive,fmt)
        }
    }
    pub fn serialise_exclusive_range(from_exclusive: A, to_inclusive: A, fmt: &mut Formatter<'_>) -> std::fmt::Result {
        Self::serialise_inclusive_range(from_exclusive+1,to_inclusive,fmt)
    }

    /**Serializes automaton to AT&T automaton format*/
    pub fn serialise_att(&self, fmt: &mut Formatter<'_>) -> std::fmt::Result {
        for ((state, transitions), accepting) in self.graph().iter().enumerate().zip(self.accepting()) {
            let mut from_inclusive = 0;
            for range in transitions.iter() {
                let to_inclusive = range.input();
                for edge in range.edges() {
                    if let Some(target) = edge.target() {
                        write!(fmt, "{} {} ", state, target);
                        Self::serialise_exclusive_range(from_inclusive, to_inclusive, fmt);
                        write!(fmt, " {} {}\n", edge.weight(), edge.output());
                    }
                }
                from_inclusive = to_inclusive;
            }
            if let Some(fin) = accepting {
                write!(fmt, "{} {} {}\n", state, fin.weight(), fin.output());
            }
        }
        Ok(())
    }
}
impl <Tr:Trans> Debug for RangedGraph<Tr>{
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        self.serialise_att(f)
    }
}