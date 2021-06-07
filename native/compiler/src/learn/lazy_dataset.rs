use std::fs::File;
use std::process::{Command, Stdio, ChildStdout};
use std::io::{BufReader, Lines, BufRead};
use std::iter::{FilterMap, Map};
use int_seq::IntSeq;

// pub fn parse_line(line:String)->(IntSeq,Option<IntSeq>){
//     line.split_once('\t').map(|(l, r)| (IntSeq::from(l), Some(IntSeq::from(r)))).unwrap_or_else(||(IntSeq::from(line.as_str()),None))
// }                                                   //  Map<FilterMap<Lines<BufReader<T>>, fn(std::io::Result<String>) -> Option<String>>, fn(String) -> (IntSeq, Option<IntSeq>)>
// pub fn iter_lines<'a,T:BufRead>(reader:BufReader<T>) -> Map<FilterMap<Lines<BufReader<T>>, fn(std::io::Result<String>) -> Option<String>>, fn(String) -> (IntSeq, Option<IntSeq>)> {
//     reader.lines().filter_map(Result::ok).map(parse_line)
// }
// pub fn iter_executable<'a>(path:&String, interpreter:&String) -> FilterMap<FilterMap<Lines<BufReader<ChildStdout>>, fn(std::io::Result<String>) -> Option<String>>, fn(String) -> (IntSeq,Option<IntSeq>)> {
//     let cmd = Command::new(interpreter)
//         .arg(path)
//         .stdout(Stdio::piped())
//         .spawn()
//         .expect("failed to execute process");
//     let r = BufReader::new(cmd.stdout.unwrap());
//     iter_lines(r)
// }
// pub fn iter_file<'a>(path:&String) -> std::io::Result<FilterMap<FilterMap<Lines<BufReader<ChildStdout>>, fn(std::io::Result<String>) -> Option<String>>, fn(String) -> (IntSeq,Option<IntSeq>)>> {
//     let file = File::open(path)?;
//     let r = BufReader::new(file);
//     Ok(iter_lines(r))
// }
// pub fn iter_auto<'a>(path:&String) -> std::io::Result<FilterMap<FilterMap<Lines<BufReader<ChildStdout>>, fn(std::io::Result<String>) -> Option<String>>, fn(String) -> Option<(IntSeq,IntSeq)>>> {
//     if path.ends_with(".py"){
//         Ok(iter_executable(path,"python3".into()))
//     }else if path.ends_with(".sh"){
//         Ok(iter_executable(path,"bash".into()))
//     }else{
//         iter_file(path)
//     }
// }
