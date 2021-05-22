use lalrpop_util::ParseError;
use compilation_error::CompErr;
use g::G;
use p::P;
use int_seq::IntSeq;

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