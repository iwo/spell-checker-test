#!/bin/bash

HUNSPELL_DIR=/usr/share/hunspell
for LOCALE in en_US es_ES pl_PL ru_RU
do
    echo
    echo "Generating word list for $LOCALE"
    ENC=$(grep "^SET " $HUNSPELL_DIR/$LOCALE.aff | cut -d " " -f 2)
    echo "Encoding: $ENC"
    unmunch $HUNSPELL_DIR/$LOCALE.dic $HUNSPELL_DIR/$LOCALE.aff > unmunched_$LOCALE.txt 2> /dev/null
    echo "Cleaning up words list"
    iconv -f $ENC -t UTF-8 unmunched_$LOCALE.txt | sort -S 2000000000 | uniq > hunspell_words_$LOCALE.txt
    rm unmunched_$LOCALE.txt
done

ls -l hunspell_words_*

