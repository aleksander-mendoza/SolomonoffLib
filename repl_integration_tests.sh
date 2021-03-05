#!/bin/bash

for test in batch_tests/test*.input ; do
  name=${test%.input}
  echo "./solomonoff repl --exec \"$(cat "$name.input") /exit\" > \"$name.output\""
  ./solomonoff repl --exec "$(cat "$name.input") /exit" > "$name.output"
  if ! cmp --silent "$name.expected" "$name.output" ; then
    echo "Fail: $name"
  fi
done
