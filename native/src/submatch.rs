use int_seq::{IntSeq, A};
use std::collections::VecDeque;

pub const MID: A = A::MAX / 2;

pub fn validate_submatch_markers(seq: IntSeq) -> bool {
    let mut submatches = Vec::new();
    for symbol in seq {
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

// pub fn submatch(out: IntSeq, matcher: fn(A, IntSeq) -> IntSeq) -> IntSeq {
//     assert!(validate_submatch_markers(out));
//     struct Submatch {
//         group_index:A,
//         matched_region:IntSeq
//     }
//     final ArrayList < Submatch > stack = new
//     ArrayList < > ();
//     int
//     stackHeightInclusive = 0;
//     stack.add(new
//     Submatch(minimal()));
//     for (int symbol : out) {
//         final Submatch
//         last = stack.get(stackHeightInclusive);
//         if (compare(symbol, mid()) > 0) {
//             final int
//             cmp = compare(symbol, last.groupIndex);
//             if (cmp == 0) {
//                 stackHeightInclusive - -;
//                 final Submatch
//                 newLast = stack.get(stackHeightInclusive);
//                 final Iterable < Integer > subgroupOut = matcher.apply(symbol, last.matchedRegion);
//                 if (subgroupOut == null)
//                 return null;
//                 subgroupOut.forEach(newLast.matchedRegion
//                 ::add);
//             } else {
//                 assert
//                 cmp > 0;
//                 stackHeightInclusive + +;
//                 assert
//                 stackHeightInclusive <= stack.size();
//                 if (stack.size() == stackHeightInclusive) {
//                     stack.add(new
//                     Submatch(symbol));
//                 } else {
//                     final Submatch
//                     newLast = stack.get(stackHeightInclusive);
//                     newLast.matchedRegion.clear();
//                     newLast.groupIndex = symbol;
//                 }
//             }
//         } else {
//             last.matchedRegion.add(symbol);
//         }
//     }
//     assert
//     stackHeightInclusive == 0;
//     final Submatch
//     last = stack.get(stackHeightInclusive);
//     assert
//     last.groupIndex == minimal();
//     final Iterable < Integer > subgroupOut = matcher.apply(minimal(), last.matchedRegion);
//     return subgroupOut == null?;
//     null: Seq.wrap(last.matchedRegion);
// }