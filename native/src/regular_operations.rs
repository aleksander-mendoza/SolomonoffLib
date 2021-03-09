use g::G;
use n::N;
use ghost::Ghost;
use p::P;
use compilation_error::CompErr;
use compilation_error::CompErr::KleeneNondeterminism;
use v::V;

impl G {
    pub fn epsilon_union<'a>(pos: V, lhs: &'a Option<P>, rhs: &'a Option<P>) -> Result<&'a Option<P>, CompErr> {
        match lhs {
            None => match rhs {
                None => Ok(lhs),
                Some(_) => Ok(rhs)
            }
            Some(l) => match rhs {
                None => Ok(lhs),
                Some(r) => if l.weight() > r.weight() {
                    Ok(lhs)
                } else if l.weight() < r.weight() {
                    Ok(rhs)
                } else if l.output() == r.output() {
                    Ok(rhs)
                } else {
                    Err(KleeneNondeterminism(pos,r.clone()))
                }
            }
        }
    }
    pub fn epsilon_kleene(pos: V, eps: &Option<P>) -> Result<&Option<P>, CompErr> {
        if let Some(eps) = eps {
            if eps.is_neutral() {
                return Err(KleeneNondeterminism(pos,eps.clone()));
            }
        }
        Ok(eps)
    }
    pub fn union(mut self, mut rhs: G, pos: V, ghost: &Ghost) -> Result<G, CompErr> {
        match G::epsilon_union(pos, self.epsilon(), rhs.epsilon()) {
            Ok(eps) => {
                let eps = eps.clone();
                self.incoming_mut().extend(rhs.incoming().iter().cloned());
                for (&n,e) in rhs.outgoing(){
                    self.outgoing_mut().insert(n,e.clone());
                }
                self.set_epsilon(eps);
                Ok(self)
            }
            Err(err) => {
                self.delete(ghost);
                rhs.delete(ghost);
                Err(err)
            }
        }
    }

    pub fn concat(mut self, mut rhs: G, ghost: &Ghost) -> G {
        if self.is_empty() {
            rhs.delete(ghost);
            return self;
        }
        if rhs.is_empty() {
            self.delete(ghost);
            return rhs;
        }

        for (fin_v, fin_e) in self.outgoing() {
            for (init_e, init_v) in rhs.incoming() {
                N::push(*fin_v, (fin_e.left_action(&init_e), *init_v),ghost);
            }
        }
        if let (Some(lhs_eps),incoming) = self.epsilon_and_incoming() {
            for (init_e, init_v) in rhs.incoming() {
                incoming.push((lhs_eps.left_action(&init_e), *init_v));
            }
        }
        if let (Some(rhs_eps),outgoing) = rhs.epsilon_and_outgoing() {
            for (fin_v, fin_e) in self.outgoing() {
                outgoing.insert(*fin_v, fin_e.multiply(rhs_eps));
            }
        }

        if let Some(lhs_eps) = self.epsilon() {
            self.set_epsilon(rhs.epsilon().as_ref().map(|rhs_eps| lhs_eps.multiply(&rhs_eps)));
        }//else lhs_eps is None

        self.set_outgoing(rhs.rip_out_outgoing());
        self
    }

    pub fn kleene_optional(mut self, pos: V, ghost: &Ghost) -> Result<G, CompErr> {
        match G::epsilon_kleene(pos, self.epsilon()) {
            Ok(_) => {
                self.set_epsilon(Some(P::neutral()));
                Ok(self)
            }
            Err(err) => {
                self.delete(ghost);
                Err(err)
            }
        }
    }

    pub fn kleene(mut self, pos: V, ghost: &Ghost) -> Result<G, CompErr> {
        for (fin_v, fin_e) in self.outgoing() {
            for (init_e, init_v) in self.incoming() {
                N::outgoing_mut(*fin_v, ghost).push((fin_e.left_action(&init_e), *init_v))
            }
        }
        self.kleene_optional(pos, ghost)
    }

    pub fn kleene_semigroup(mut self, pos:V,ghost: &Ghost) -> Result<G,CompErr> {
        for (fin_v, fin_e) in self.outgoing() {
            for (init_e, init_v) in self.incoming() {
                N::outgoing_mut(*fin_v, ghost).push((fin_e.left_action(&init_e), *init_v))
            }
        }
        match self.epsilon() {
            Some(eps) => {
                if eps.is_neutral() {
                    Ok(self)
                } else {
                    let eps = eps.clone();
                    self.delete(ghost);
                    Err(KleeneNondeterminism(pos,eps))
                }
            }
            None => Ok(self)
        }
    }

    /**
     * Performs right action on all final edges and epsilon.
     */
    pub fn right_action_on_graph(mut self, edge: &P) -> G {
        self.outgoing_mut().iter_mut().for_each(|e| e.1.multiply_in_place(edge));
        self.set_epsilon(self.epsilon().as_ref().map(|eps| eps.multiply(edge)));
        self
    }
}

impl P {
    pub fn left_action_on_graph(self: &P, mut graph: G) -> G {
        graph.incoming_mut().iter_mut().for_each(|e| e.0 = self.left_action(&e.0));
        graph.set_epsilon(graph.epsilon().as_ref().map(|eps| self.multiply(eps)));
        graph
    }
}
