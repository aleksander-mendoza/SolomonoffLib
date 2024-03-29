use int_seq::IntSeq;
use parser_state::ParserState;
use compilation_error::CompErr;
use std::alloc::Global;
use repl::Repl;
use ghost::Ghost;
use exact_size_chars::ExactSizeChars;
use logger::Logger;

pub enum ReplArg {
    Value(String),
    KeyValue(String, String),
}

impl ReplArg {
    pub fn get_value(self) -> String {
        match self {
            ReplArg::Value(v) => v,
            ReplArg::KeyValue(_, v) => v
        }
    }
    pub fn get_key(self) -> String {
        match self {
            ReplArg::Value(v) => v,
            ReplArg::KeyValue(k, _) => k
        }
    }
}

pub fn args_to_value_value(args: Vec<ReplArg>, arg1: &'static str, arg2: &'static str) -> Result<(String, String), CompErr> {
    if args.len() == 2 {
        let mut i = args.into_iter();
        let first = i.next().unwrap();
        let second = i.next().unwrap();
        if let ReplArg::Value(v1) = first {
            if let ReplArg::Value(v2) = second {
                return Ok((v1, v2));
            }
        }
    }
    Err(CompErr::IncorrectCommandArguments(format!("Expected {} and {}", arg1, arg2)))
}


pub fn args_to_value(args: Vec<ReplArg>, arg1: &'static str) -> Result<String, CompErr> {
    if args.len() == 1 {
        let mut i = args.into_iter();
        let first = i.next().unwrap();
        if let ReplArg::Value(v) = first {
            return Ok(v);
        }
    }
    Err(CompErr::IncorrectCommandArguments(format!("Expected {}", arg1)))
}


pub fn args_to_optional_value(args: Vec<ReplArg>, arg1: &'static str) -> Result<Option<String>, CompErr> {
    if args.len() == 1 {
        let mut i = args.into_iter();
        let first = i.next().unwrap();
        if let ReplArg::Value(v) = first {
            return Ok(Some(v));
        }
    } else if args.len() == 0 {
        return Ok(None);
    }
    Err(CompErr::IncorrectCommandArguments(format!("Expected {} or nothing", arg1)))
}

pub struct ReplCommand<L:Logger> {
    pub description: &'static str,
    pub f: fn(log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost) -> Result<Option<String>, CompErr>,
}

pub fn repl_print_help<L:Logger>() -> ReplCommand<L> {
    ReplCommand {
        description: "Prints help",
        f: |log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost| {
            fn print_cmd<L:Logger>(repl: &Repl<L>, cmd: &String) -> Option<String> {
                repl.get_cmd(cmd).map(|c| format!("{}\n\t{}", cmd, c.description))
            }
            if let Some(cmd) = args_to_optional_value(args, "CMD")? {
                Ok(Some(print_cmd(repl, &cmd).unwrap_or(String::from("No such command!"))))
            } else {
                let mut s = String::new();
                for (name, cmd) in repl.iter_cmds() {
                    s += &format!("{}\n\t{}\n", name, cmd.description);
                }
                Ok(Some(s))
            }
        },
    }
}

pub fn repl_eval<L:Logger>() -> ReplCommand<L> {
    ReplCommand {
        description: "Evaluates a transducer",
        f: |log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost| {
            let (name, input) = args_to_value_value(args, "ID", "INPUT")?;
            match repl.eval(ghost, &name, ExactSizeChars::from(&input)) {
                Ok(out) => Ok(Some(out.map(|v| v.to_string()).unwrap_or(String::from("No match!")))),
                Err(()) => Err(CompErr::NonexistentTransducer(name))
            }
        },
    }
}

pub fn repl_ls<L:Logger>() -> ReplCommand<L> {
    ReplCommand {
        description: "Lists all defined transducers",
        f: |log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost| {
            let mut vars:Vec<&str> = repl.state().iter_variables().map(|(var,..)|var.as_str()).collect();
            vars.sort();
            Ok(Some(format!("[{}]",vars.join(", "))))
        },
    }
}


pub fn repl_funcs<L:Logger>() -> ReplCommand<L> {
    ReplCommand {
        description: "Lists all external functions",
        f: |log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost| {
            let mut i = repl.state().iter_functions();
            Ok(Some(if let Some(first) = i.next() {
                let mut s = format!("[{}", first);
                for var in i {
                    s += ", ";
                    s += var;
                }
                s += "]";
                s
            } else {
                String::from("[]")
            }))
        },
    }
}

pub fn repl_unset<L:Logger>() -> ReplCommand<L> {
    ReplCommand {
        description: "Removes a previously defined transducer",
        f: |log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost| {
            let name = args_to_value(args, "ID")?;
            if !repl.state_mut().delete_variable(name.clone(), ghost) {
                Err(CompErr::NonexistentTransducer(name))
            } else {
                Ok(None)
            }
        },
    }
}

pub fn repl_unset_all<L:Logger>() -> ReplCommand<L> {
    ReplCommand {
        description: "Removes all defined transducers",
        f: |log:&mut L, debug:&mut L, args: Vec<ReplArg>, repl: &mut Repl<L>, ghost: &Ghost| {
            let name = args_to_value(args, "ID")?;
            repl.state_mut().delete_all(ghost);
            Ok(None)
        },
    }
}
