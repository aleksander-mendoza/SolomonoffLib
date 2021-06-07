use v::V;
use func_arg::{FuncArgs, FuncArg};
use compilation_error::CompErr;
use g::G;
use int_embedding::IntEmbedding;
use learn::ostia_compress::PTT;
use ghost::Ghost;
use learn::lazy_dataset;
use parser_state::ParserState;

pub fn ostia_compress(ghost:&Ghost, pos:V,args:FuncArgs)->Result<G,CompErr>{
    if args.len() == 1{
        if let FuncArg::Informant(i) = &args[0]{
            let alph = IntEmbedding::for_informant(&mut i.iter().map(|(a,b)|(a,b)));
            let mut ptt = PTT::new(alph);
            for (input, output) in i {
                if let Some(output) = output {
                    ptt.insert_positive(input,output);
                }
            }
            return Ok(ptt.ostia_compress().compile(pos, ghost));
        }
    }
    Err(CompErr::IncorrectFunctionArguments(pos, String::from("Expected a single informant argument")))
}

// pub fn ostia_compress_file(ghost:&Ghost, pos:V,args:FuncArgs)->Result<G,CompErr>{
//     if args.len() == 1{
//         if let FuncArg::Informant(i) = &args[0]{
//             if i.len() == 1 {
//                 let (path,_) = &i[0];
//                 let path = path.to_string();
//
//                 let alph = IntEmbedding::for_informant(&mut lazy_dataset::iter_auto(&path).map_err(|e|CompErr::Parse(pos,e.to_string()))?.map(|(a, b)| (a, b)));
//                 let mut ptt = PTT::new(alph);
//                 for (input, output) in i {
//                     if let Some(output) = output {
//                         ptt.insert_positive(input, output);
//                     }
//                 }
//                 return Ok(ptt.ostia_compress().compile(pos, ghost));
//             }
//         }
//     }
//     Err(CompErr::IncorrectFunctionArguments(pos, String::from("Expected a single file path argument")))
// }

