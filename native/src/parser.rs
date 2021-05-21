use ghost::Ghost;
use int_seq::IntSeq;
lalrpop_mod!(pub solomonoff); // synthesized by LALRPOP



#[cfg(test)]
mod tests {
    // Note this useful idiom: importing names from outer (for mod tests) scope.
    use super::*;
    use ranged_optimisation::optimise_graph;
    use int_seq::A;

    #[test]
    fn test_1() {
        Ghost::with_mock(|ghost| {
            let mut g = solomonoff::MealyUnionParser::new().parse(ghost,"'aa'").unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("a"));
            assert!(y.is_none());
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aa"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from(""));
            g.delete(ghost)
        });
    }
    // #[test]
    // fn test_2() {
    //     Ghost::with_mock(|ghost| {
    //         let expr = solomonoff::MealyUnionParser::new().parse(ghost,"'a'").unwrap();
    //         assert_eq!("a", expr);
    //     });
    // }
}