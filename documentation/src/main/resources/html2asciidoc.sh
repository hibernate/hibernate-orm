#!/bin/bash

htmls=`find ../../../target/docbook -name '*.html'`

for html in $htmls
do
    echo 'Converting' $html
    adoc="${html%.*}.adoc"
    pandoc -s -r html "$html" -t asciidoc -o "$adoc"
done