use std::fs::File;
use std::process::{Command, Stdio, ChildStdout};
use std::io::{BufReader, Lines, BufRead};
use std::iter::{FilterMap, Map};
use int_seq::IntSeq;
use int_embedding::{IntEmbedding, Alphabet};
use learn::ostia_compress::{PTT, PrefixTreeTransducer, insert_all};

pub struct DatasetIter {}

pub fn parse_line(line: String) -> (IntSeq, Option<IntSeq>) {
    line.split_once('\t').map(|(l, r)| (IntSeq::from(l), Some(IntSeq::from(r)))).unwrap_or_else(|| (IntSeq::from(line.as_str()), None))
}

pub fn iter_lines<'a, T: BufRead>(reader: Lines<T>) -> Map<FilterMap<Lines<T>, fn(std::io::Result<String>) -> Option<String>>, fn(String) -> (IntSeq, Option<IntSeq>)> {
    reader.filter_map(Result::ok as fn(std::io::Result<String>) -> Option<String>).map(parse_line as fn(String) -> (IntSeq, Option<_>))
}

pub fn iter_executable<'a>(path: &String, interpreter: &String) -> Map<FilterMap<Lines<BufReader<ChildStdout>>, fn(std::io::Result<String>) -> Option<String>>, fn(String) -> (IntSeq, Option<IntSeq>)> {
    let cmd = Command::new(interpreter)
        .arg(path)
        .stdout(Stdio::piped())
        .spawn()
        .expect("failed to execute process");
    let r = BufReader::new(cmd.stdout.unwrap());
    iter_lines(r.lines())
}

pub fn iter_file<'a>(path: &String) -> std::io::Result<Map<FilterMap<Lines<BufReader<File>>, fn(std::io::Result<String>) -> Option<String>>, fn(String) -> (IntSeq, Option<IntSeq>)>> {
    let file = File::open(path)?;
    let r = BufReader::new(file);
    Ok(iter_lines(r.lines()))
}

pub fn infer_alph<'a>(path: &String) -> std::io::Result<IntEmbedding> {
    if path.ends_with(".py") {
        Ok(IntEmbedding::for_informant(&mut iter_executable(path, &String::from("python3"))))
    } else if path.ends_with(".sh") {
        Ok(IntEmbedding::for_informant(&mut iter_executable(path, &String::from("bash"))))
    } else {
        iter_file(path).map(|mut e| IntEmbedding::for_informant(&mut e))
    }
}

pub fn insert_from_dataset<A: PrefixTreeTransducer>(ptt: &mut A, path: &String) -> std::io::Result<()> {
    if path.ends_with(".py") {
        insert_all(ptt, &mut iter_executable(path, &String::from("python3")));
    } else if path.ends_with(".sh") {
        insert_all(ptt, &mut iter_executable(path, &String::from("bash")));
    } else {
        insert_all(ptt, &mut iter_file(path)?);
    }
    Ok(())
}