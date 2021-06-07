use std::collections::HashMap;
use g::G;
use v::V;
use compilation_error::CompErr;
use ghost::Ghost;
use std::collections::hash_map::Entry;
use func_arg::FuncArgs;
use external_functions::ostia_compress;

pub struct ParserState {
    variables: HashMap<String, (V, bool, G)>,
    external_functions: HashMap<String, fn(&Ghost, V, FuncArgs) -> Result<G, CompErr>>,
}

impl ParserState {
    pub fn external_function(&self, func_name: String, ghost: &Ghost, pos: V, args: FuncArgs) -> Result<G, CompErr> {
        if let Some(f) = self.external_functions.get(&func_name) {
            f(ghost, pos, args)
        } else {
            Err(CompErr::UndefinedExternalFunc(pos, func_name))
        }
    }
    pub fn introduce_variable(&mut self, name: String, pos: V, always_copy: bool, g: G) -> Result<(), CompErr> {
        if let Some((prev, _, _)) = self.variables.insert(name.clone(), (pos.clone(), always_copy, g)) {
            Err(CompErr::DuplicateFunction(prev, pos, name))
        } else {
            Ok(())
        }
    }
    pub fn consume_variable(&mut self, name: String, ghost: &Ghost) -> Option<(V, bool, G)> {
        match self.variables.entry(name) {
            Entry::Occupied(e) => Some(if e.get().1 {
                let (pos, always_copy, g) = e.get();
                (pos.clone(), *always_copy, g.clone(ghost))
            } else {
                e.remove()
            }),
            Entry::Vacant(_) => None
        }
    }
    pub fn copy_variable(&mut self, name: &String, ghost: &Ghost) -> Option<(V, bool, G)> {
        self.variables.get(name).map(|(pos, always_copy, g)| (pos.clone(), *always_copy, g.clone(ghost)))
    }
    pub fn borrow_variable(&self, name: &String) -> Option<&(V, bool, G)> {
        self.variables.get(name)
    }
    pub fn delete_all(&mut self, ghost: &Ghost) {
        for (_, _, g) in self.variables.values_mut() {
            g.delete(ghost);
        }
        self.variables.clear()
    }
    pub fn new() -> Self {
        Self { variables: HashMap::new(), external_functions: HashMap::new() }
    }

    pub fn add_ell_external_functions(&mut self) {
        self.external_functions.insert(String::from("ostiaCompress"), ostia_compress);
    }

    pub fn new_with_standard_library() -> Self {
        let mut me = Self::new();
        me.add_ell_external_functions();
        me
    }
}


