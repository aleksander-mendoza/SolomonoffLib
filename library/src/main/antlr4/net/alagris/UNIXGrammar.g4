grammar UNIXGrammar;

regExp            :     branch ( '|' branch )* ;
branch            :     piece* ;
piece             :     atom quantifier? ;
quantifier        :     Quantifiers | '{'quantity'}' ;
quantity          :     quantRange | quantMin | quantExact ;
quantRange        :     quantExact ',' quantExact ;
quantMin          :     quantExact ',' ;
atom              :     normalChar | '(' regExp ')' ;
normalChar        :     NormalChar | Digit ;
quantExact        :     Digit+ ;

Digit             :     [0-9] ;
NormalChar        :     ~[.\\?*+{}()|[\]] ;
Quantifiers       :     [?*+] ;