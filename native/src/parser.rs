use ghost::Ghost;
use int_seq::IntSeq;
lalrpop_mod!(pub solomonoff); // synthesized by LALRPOP
use regular_operations;
use g::G;

#[cfg(test)]
mod tests {
    // Note this useful idiom: importing names from outer (for mod tests) scope.
    use super::*;
    use ranged_optimisation::optimise_graph;
    use int_seq::A;
    use compilation_error::CompErr;

    #[test]
    fn test_1() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,"'aa'").unwrap().unwrap();
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

    #[test]
    fn test_2() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,"'aa'|'bc'").unwrap().unwrap();
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

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("bc"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from(""));

            g.delete(ghost)
        });
    }


    #[test]
    fn test_3() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,"'aa' 'bc' :'de'").unwrap().unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aa"));
            assert!(y.is_none());
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aabc"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("de"));
            g.delete(ghost)
        });
    }


    #[test]
    fn test_4() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,"'aa':'xx' | 'bc' :'de'").unwrap().unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aa"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("xx"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("bc"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("de"));
            g.delete(ghost)
        });
    }

    #[test]
    fn test_5() {
        Ghost::with_mock(|ghost| {
            let mut g:G = solomonoff::MealyUnionParser::new().parse(ghost,"('aa':'xx')+ | ('bc' :'de')*").unwrap().unwrap();
            let r = optimise_graph(&g, ghost);
            let mut state_to_index = r.make_state_to_index_table();
            let mut output_buffer = Vec::<A>::with_capacity(256);
            unsafe{output_buffer.set_len(256)};
            println!("{:?}",r);
            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("aaaa"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("xxxx"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from("bcbcbc"));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from("dedede"));

            let y = r.evaluate_tabular(&mut state_to_index,output_buffer.as_mut_slice(),&IntSeq::from(""));
            assert!(y.is_some());
            let y:String = unsafe{y.unwrap().iter().map(|&x|char::from_u32_unchecked(x)).collect()};
            assert_eq!(y,String::from(""));

            g.delete(ghost)
        });
    }
}