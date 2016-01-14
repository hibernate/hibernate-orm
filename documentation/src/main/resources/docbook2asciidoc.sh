#!/bin/bash

xmls=`find ../docbook/mappingGuide/en-US/chapters/basic  -name '*.xml'`

for xml in ../docbook/devguide-old/en-US/Batch_Processing.xml
do
    adoc="${xml%.*}.adoc"
    echo 'Converting' $xml to $adoc
    cp $xml $adoc
    sed -i -r 's_<(programlisting.*?)>_<\!--\1-->_g' $adoc
    pandoc -s -r docbook "$adoc" -t asciidoc --no-wrap -o "$adoc"
done