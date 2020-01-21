grep $1  *.txt | grep Log  | sed 's/.txt:/,/' | sort -t , -k 2 | cut -d . -f 2-
