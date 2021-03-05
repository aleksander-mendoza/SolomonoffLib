#!/bin/bash

for test in batch_tests/test*.input ; do
  name=${test%.input}
  text=$(cat "$name.input" | tr -d "\n\r" )
  echo "./solomonoff repl --exec \"$text /exit\" > \"$name.output\""
  ./solomonoff repl --exec "$text /exit" > "$name.output"
  if ! cmp --silent "$name.expected" "$name.output" ; then
    echo "Fail: $name"
    diff "$name.expected" "$name.output"
  fi
done
