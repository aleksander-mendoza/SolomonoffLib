use lalrpop_util::ParseError;
use compilation_error::CompErr;
use g::G;
use p::P;
use int_seq::{IntSeq, A, decrement, REFLECT};
use ranged_serializers::{unescape_character, unescape_u32, unescape_a};
use v::V;
use ghost::Ghost;
use core::str::next_code_point;
use std::str::{FromStr, Chars};
use std::convert::TryInto;
use nonmax::ParseIntError;


pub fn pe<'input, R, L, T>(error: Result<R, CompErr>) -> Result<R, ParseError<L, T, CompErr>> {
    error.map_err(|error| ParseError::User { error })
}

pub fn pre_w(w: (i32, G)) -> G {
    let (w, g) = w;
    if w == 0 { g } else { P::new(w, IntSeq::EPSILON).left_action_on_graph(g) }
}

pub fn post_w(w: (G, i32)) -> G {
    let (g, w) = w;
    if w == 0 { g } else { g.right_action_on_graph(&P::new(w, IntSeq::EPSILON)) }
}

pub fn parse_a(pos: V, original: &str) -> Result<(V, A), CompErr> {
    match u32::from_str(original).ok().and_then(|c|char::from_u32(c)) {
        Some(n) => Ok((pos, n)),
        _ => Err(CompErr::Parse(pos, String::from(format!("Integer value {} is not a valid unicode", original))))
    }
}

pub fn parse_char(pos: V, original: u32) -> Result<(V, char), CompErr> {
    match char::from_u32(original) {
        Some(n) => Ok((pos, n)),
        None => return Err(CompErr::Parse(pos.clone(), String::from(format!("Codepoint {} is not a valid utf-8 character", original))))
    }
}

pub fn parse_codepoints(s: &str, pos: V) -> Result<String, CompErr> {
    let mut s = s.split_whitespace();
    let mut str = String::with_capacity(s.size_hint().0);
    let r = s.try_fold((pos, str), |(pos, mut s), num| {
        let (pos, num) = parse_a(pos, num)?;
        s.push(num);
        Ok((pos, s))
    });
    r.map(|(pos, s)| s)
}

pub fn parse_codepoint_range(s: &str, pos: V, ghost: &Ghost) -> Result<G, CompErr> {
    let mut r = s.split_whitespace();
    let v = Vec::<(A, A)>::with_capacity(r.size_hint().0);
    let (pos,v) = r.try_fold((pos,v), |(pos,mut v),range| {
        let (pos,(from,to)) = if let Some((from, to)) = range.split_once('-') {
            let (pos, from) = parse_a(pos, from)?;
            let (pos, to) = parse_a(pos, to)?;
            (pos,(from, to))
        } else {
            let (pos, from) = parse_a(pos, range)?;

            (pos,(from, from))
        };
        if from==REFLECT{
            return Err(CompErr::Parse(pos,String::from("Null character not allowed on input")));
        }
        v.push((decrement(from),to));
        Ok((pos,v))
    })?;
    Ok(G::new_from_ranges(v.into_iter(), pos, ghost))
}

pub fn parse_range(s: &str, pos: V, ghost: &Ghost) -> Result<G, CompErr> {
    let mut ranges = Vec::<(A, A)>::new();
    let codepoints: Vec<char> = s.chars().collect();
    let mut i = 0;
    while i < codepoints.len() - 1 {
        let from = if codepoints[i] == '\\' {
            i += 1;
            unescape_character(codepoints[i])
        } else {
            codepoints[i]
        };
        let to = if codepoints[i + 1] == '-' {
            i += 2;
            if codepoints[i] == '\\' {
                i += 1;
                unescape_character(codepoints[i])
            } else {
                codepoints[i]
            }
        } else {
            from
        };
        i += 1;
        let from = from as A;
        let to = to as A;
        if from == REFLECT {
            return Err(CompErr::Parse(pos, String::from("Null symbol \\0 is not allowed on input")));
        }
        ranges.push((decrement(from), to));
    }
    Ok(G::new_from_ranges(ranges.into_iter(), pos, ghost))
}

pub struct StringLiteralIter<'a> {
    s: Chars<'a>
}

const BACKSLASH: A = '\\' as A;

impl<'a> Iterator for StringLiteralIter<'a> {
    type Item = A;

    fn next(&mut self) -> Option<Self::Item> {
        let c = self.s.next();
        match c {
            None => None,
            Some(c) => if c == BACKSLASH {
                self.s.next().map(unescape_a)
            } else {
                Some(c)
            }
        }
    }
}

pub fn parse_literal(s: &str) -> StringLiteralIter {
    StringLiteralIter { s: s.chars() }
}