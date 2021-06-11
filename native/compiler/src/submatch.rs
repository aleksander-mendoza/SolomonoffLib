use int_seq::{IntSeq, A, REFLECT};

pub const MID: A = '\u{100000}'; // Unicode Supplementary Private Use Area-B

pub fn validate_submatch_markers(seq: &[A]) -> bool {
    let mut submatches = Vec::new();
    for &symbol in seq {
        if symbol > MID {
            match submatches.last() {
                None => submatches.push(symbol),
                Some(&last) =>
                    if symbol == last {
                        submatches.pop();
                    } else if symbol > last {
                        submatches.push(symbol);
                    } else {
                        return false;
                    }
            }
        }
    }
    true
}

pub fn submatch<F:Fn(A, &mut Vec<A>, &mut Vec<A>)->bool>(input: &[A], out:&mut Vec<A>, matcher: F) -> bool {
    assert!(validate_submatch_markers(input));
    assert!(out.is_empty());
    struct Submatch {
        group_index:A,
        matched_region:Vec<A>
    }
    let mut stack = Vec::with_capacity(16);
    stack.push( Submatch{group_index:REFLECT, matched_region:Vec::with_capacity(input.len())});
    for &symbol in input {
        let mut last = stack.last().unwrap().group_index;
        if symbol > MID {
            if symbol == last {
                let mut popped = stack.pop().unwrap();
                assert!(out.is_empty());
                let matched = matcher(symbol, &mut popped.matched_region,out);
                if !matched{
                    return false;
                }
                stack.last_mut().unwrap().matched_region.append(out)
            } else {
                assert!(symbol > last);
                stack.push(Submatch{group_index:symbol, matched_region:Vec::with_capacity(input.len())});
            }
        } else {
            stack.last_mut().unwrap().matched_region.push(symbol);
        }
    }
    assert_eq!(stack.len(),1);
    let mut last = stack.pop().unwrap();
    assert_eq!(last.group_index,REFLECT);
    matcher(REFLECT, &mut last.matched_region, out)
}