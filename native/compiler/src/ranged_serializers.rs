use ranged_graph::{RangedGraph, Trans, NonSink};
use std::fmt::{Formatter, Debug};
use int_seq::{A, REFLECT, increment};
use p::PartialEdge;

const B: char = 7 as char;
const F: char = 12 as char;

const N32: u32 = 'n' as u32;
const R32: u32 = 'r' as u32;
const NULL32: u32 = '0' as u32;
const T32: u32 = 't' as u32;
const B32: u32 = 'b' as u32;
const F32: u32 = 'f' as u32;


const N8: u8 = 'n' as u8;
const R8: u8 = 'r' as u8;
const NULL8: u8 = '0' as u8;
const T8: u8 = 't' as u8;
const B8: u8 = 'b' as u8;
const F8: u8 = 'f' as u8;

pub fn unescape_a(c: A) -> A {
    unescape_character(c)
}

pub fn unescape_u32(c: u32) -> u32 {
    match c {
        N32 => '\n' as u32,
        R32 => '\r' as u32,
        NULL32 => '\0' as u32,
        T32 => '\t' as u32,
        B32 => 7,
        F32 => 12,
        c => c
    }
}

pub fn unescape_u8(c: u8) -> u8 {
    match c {
        N8 => '\n' as u8,
        R8 => '\r' as u8,
        NULL8 => '\0' as u8,
        T8 => '\t' as u8,
        B8 => 7,
        F8 => 12,
        c => c
    }
}

pub fn unescape_character(c: char) -> char {
    match c {
        'n' => '\n',
        'r' => '\r',
        '0' => '\0',
        't' => '\t',
        'b' => B,
        'f' => F,
        c => c
    }
}

pub fn escape_symbol(c: A, fmt: &mut Formatter<'_>) -> std::fmt::Result {
    match c {
        '\n' => write!(fmt, "\\n"),
        '\r' => write!(fmt, "\\r"),
        '\0' => write!(fmt, "\\0"),
        '\t' => write!(fmt, "\\t"),
        B => write!(fmt, "\\b"),
        F => write!(fmt, "\\b"),
        _ => write!(fmt, "{}", c)
    }
}

pub fn serialise_literal<I: Iterator<Item=A>>(mut i: I, fmt: &mut Formatter<'_>) -> std::fmt::Result {
    fn is_graphic(a: A) -> bool {
        a.is_ascii_graphic() || a == ' ' || a == '\n' || a == '\r' || a == '\r' || a == '\0' || a == '\t' || a == B || a == F
    }
    if let Some(a) = i.next() {
        let mut is_current_graphic = is_graphic(a);
        if is_current_graphic {
            write!(fmt, "'")?;
            escape_symbol(a, fmt)?;
        } else {
            write!(fmt, "<{}", a as u32)?;
        }
        for c in i {
            if is_current_graphic {
                if is_graphic(c) {
                    escape_symbol(c, fmt)?;
                } else {
                    is_current_graphic = false;
                    write!(fmt, "'<{}", c as u32)?;
                }
            } else {
                if is_graphic(c) {
                    is_current_graphic = true;
                    write!(fmt, ">'")?;
                    escape_symbol(c, fmt)?;
                } else {
                    write!(fmt, " {}", c as u32)?;
                }
            }
        }
        if is_current_graphic {
            write!(fmt, "'")
        } else {
            write!(fmt, ">")
        }
    } else {
        write!(fmt, "''")
    }
}

pub fn serialise_inclusive_range(from_inclusive: A, to_inclusive: A, fmt: &mut Formatter<'_>) -> std::fmt::Result {
    assert!(from_inclusive <= to_inclusive);
    if from_inclusive == to_inclusive {
        escape_symbol(from_inclusive, fmt)
    } else {
        escape_symbol(from_inclusive, fmt)?;
        write!(fmt, "-")?;
        escape_symbol(to_inclusive, fmt)
    }
}

pub fn serialise_exclusive_range(from_exclusive: A, to_inclusive: A, fmt: &mut Formatter<'_>) -> std::fmt::Result {
    serialise_inclusive_range(increment(from_exclusive), to_inclusive, fmt)
}


impl<Tr: Trans> RangedGraph<Tr> {
    /**Serializes automaton to AT&T automaton format*/
    pub fn serialise_att(&self, fmt: &mut Formatter<'_>) -> std::fmt::Result {
        for ((state, transitions), accepting) in self.graph().iter().enumerate().zip(self.accepting()) {
            let mut from_inclusive = REFLECT;
            for range in transitions.iter() {
                let to_inclusive = range.input();
                for edge in range.edges() {
                    if let Some(target) = edge.target() {
                        write!(fmt, "{} {} ", state, target)?;
                        serialise_exclusive_range(from_inclusive, to_inclusive, fmt)?;
                        write!(fmt, " {} {}\n", edge.weight(), edge.output())?;
                    }
                }
                from_inclusive = to_inclusive;
            }
            if let Some(fin) = accepting {
                write!(fmt, "{} {} {}\n", state, fin.weight(), fin.output())?;
            }
        }
        Ok(())
    }
}

impl<Tr: Trans> Debug for RangedGraph<Tr> {
    fn fmt(&self, fmt: &mut Formatter<'_>) -> std::fmt::Result {
        self.serialise_att(fmt)
    }
}