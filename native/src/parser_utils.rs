use lalrpop_util::ParseError;
use compilation_error::CompErr;
use g::G;
use p::P;
use int_seq::IntSeq;
use ranged_serializers::{unescape_character, unescape_u32};
use v::V;
use ghost::Ghost;
use core::str::next_code_point;

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

pub fn parse_range<L, T>(s: &str,pos:V, ghost:&Ghost) -> Result<G, ParseError<L, T, CompErr>> {
    let mut ranges = Vec::<(u32, u32)>::new();
    let codepoints: Vec<char> = s[1..s.len()].chars().collect();
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
        let from = from as u32;
        let to = to as u32;
        if from == 0 {
            return Err(ParseError::User { error: CompErr::Parse(pos, String::from("Null symbol \\0 is not allowed on input")) });
        }
        ranges.push((from - 1, to));
    }
    Ok(G::new_from_ranges(ranges.iter().cloned(), pos, ghost))
}

pub struct StringLiteralIter<'a>{
    s:std::slice::Iter<'a, u8>
}
const BACKSLASH:u32 = '\\' as u32;
impl <'a> Iterator for StringLiteralIter<'a>{
    type Item = u32;

    fn next(&mut self) -> Option<Self::Item> {
        let c = next_code_point(&mut self.s);
        match c{
            None => None,
            Some(c) => if c==BACKSLASH{
                next_code_point(&mut self.s).map(unescape_u32)
            }else{
                Some(c)
            }
        }
    }
}

pub fn parse_literal(s:&str)->StringLiteralIter{
    StringLiteralIter{s:s.as_bytes().iter()}
}