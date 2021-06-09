use std::collections::HashMap;
use g::G;
use v::V;
use compilation_error::CompErr;
use ghost::Ghost;
use std::collections::hash_map::{Entry, Iter, IterMut};
use func_arg::FuncArgs;
use external_functions::{ostia_compress, ostia_compress_file};
use ranged_graph::{RangedGraph, Transition};
use std::borrow::Borrow;
use logger::Logger;

pub struct ParserState<L:Logger> {
    variables: HashMap<String, (V, bool, G, Option<RangedGraph<Transition>>)>,
    external_functions: HashMap<String, fn(&Ghost, V, &L, FuncArgs) -> Result<G, CompErr>>,
}

impl<L:Logger> ParserState<L> {
    pub fn iter_variables(&self) -> Iter<'_, String, (V, bool, G, Option<RangedGraph<Transition>>)> {
        self.variables.iter()
    }
    pub fn iter_mut_variables(&mut self) -> IterMut<'_, String, (V, bool, G, Option<RangedGraph<Transition>>)> {
        self.variables.iter_mut()
    }
    pub fn external_function(&self, func_name: String, ghost: &Ghost,  pos: V, logger:&L, args: FuncArgs) -> Result<G, CompErr> {
        if let Some(f) = self.external_functions.get(&func_name) {
            f(ghost, pos,logger, args)
        } else {
            Err(CompErr::UndefinedExternalFunc(pos, func_name))
        }
    }
    pub fn introduce_variable(&mut self, name: String, pos: V, always_copy: bool, g: G) -> Result<(), CompErr> {
        if let Some((prev, _, _, _)) = self.variables.insert(name.clone(), (pos.clone(), always_copy, g, None)) {
            Err(CompErr::DuplicateFunction(prev, pos, name))
        } else {
            Ok(())
        }
    }
    pub fn consume_variable(&mut self, name: String, ghost: &Ghost) -> Option<(V, bool, G)> {
        match self.variables.entry(name) {
            Entry::Occupied(e) => Some(if e.get().1 {
                let (pos, always_copy, g, _) = e.get();
                (pos.clone(), *always_copy, g.clone(ghost))
            } else {
                let (pos,always_copy, g, _) = e.remove();
                (pos,always_copy,g)
            }),
            Entry::Vacant(_) => None
        }
    }
    pub fn delete_variable(&mut self, name: String, ghost: &Ghost) -> bool {
        match self.variables.entry(name) {
            Entry::Occupied(e) => {
                let (pos,always_copy, mut g, _) = e.remove();
                g.delete(ghost);
                true
            },
            Entry::Vacant(_) => false
        }
    }
    pub fn copy_variable(&mut self, name: &String, ghost: &Ghost) -> Option<(V, bool, G)> {
        self.variables.get(name).map(|(pos, always_copy, g, _)| (pos.clone(), *always_copy, g.clone(ghost)))
    }
    pub fn borrow_variable(&self, name: &String) -> Option<(V, bool, &G)> {
        self.variables.get(name).map(|(pos,always_copy, g,_)|(pos.clone(),always_copy.clone(),g))
    }
    pub fn optimise(&mut self, name: &String, ghost:&Ghost) -> Option<&RangedGraph<Transition>> {
        self.variables.get_mut(name).map(|(_,_, g,r)|{
            if let Some(r) = r{
                r
            }else{
                r.insert(g.optimise_graph(ghost))
            }
        } as &RangedGraph<Transition>)
    }
    pub fn get_optimised(&self, name: &String) -> Option<&RangedGraph<Transition>> {
        self.variables.get(name).and_then(|(_,_,_,r)| r.as_ref())
    }
    pub fn delete_all(&mut self, ghost: &Ghost) {
        for (_, _, g, _) in self.variables.values_mut() {
            g.delete(ghost);
        }
        self.variables.clear()
    }
    pub fn new() -> Self {
        Self { variables: HashMap::new(), external_functions: HashMap::new() }
    }

    pub fn add_ell_external_functions(&mut self) {
        self.external_functions.insert(String::from("ostiaCompress"), ostia_compress);
        self.external_functions.insert(String::from("activeLearningFromDataset"), ostia_compress_file);
    }

    pub fn new_with_standard_library() -> Self {
        let mut me = Self::new();
        me.add_ell_external_functions();
        me
    }
}


