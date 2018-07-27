#!/bin/bash

fatal=0

function grep_check {
  local pattern=$1
  local description=$2
  local opts=${3:--i -E -r -n}
  local fatalness=${4:-1}
  x=$(grep $opts "$pattern" documentation/book/)
  if [ -n "$x" ]; then
    echo "$description:"
    echo "$x"
    y=$(echo "$x" | wc -l)
    ((fatal+=fatalness*y))
  fi
}

# Check for latin abbrevs
grep_check '[^[:alpha:]](e\.g\.|eg)[^[:alpha:]]' "Replace 'e.g'. with 'for example, '"
grep_check '[^[:alpha:]](i\.e\.|ie)[^[:alpha:]]' "Replace 'i.e'. with 'that is, '"
grep_check '[^[:alpha:]]etc\.?[^[:alpha:]]' "Replace 'etc.'. with ' and so on.'"

# And/or
grep_check '[^[:alpha:]]and/or[^[:alpha:]]' "Use either 'and' or 'or', but not 'and/or'"

# Contractions
grep_check '[^[:alpha:]](do|is|are|won|have|ca|does|did|had|has|must)n'"'"'?t[^[:alpha:]]' "Avoid 'nt contraction"
grep_check '[^[:alpha:]]it'"'"'s[^[:alpha:]]' "Avoid it's contraction"
grep_check '[^[:alpha:]]can not[^[:alpha:]]' "Use 'cannot' not 'can not'"

# Asciidoc standards
#grep_check '[<][<][[:alnum:]_-]+,' "Internal links should be xrefs"
grep_check '[[]id=(["'"'"'])[[:alnum:]_-]+(?!-[{]context[}])\1' "[id=...] should end with -{context}" "-i -P -r -n"

if [ $fatal -gt 0 ]; then
  echo "${fatal} docs problems found."
  exit 1
fi
