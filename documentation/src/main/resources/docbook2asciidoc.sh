#!/bin/bash

xmls=`find ../docbook/mappingGuide/en-US/chapters/categoizations  -name '*.xml'`

for xml in $xmls
do
    adoc="${xml%.*}.adoc"
    echo 'Converting' $xml to $adoc
    cp $xml $adoc
    sed -i -r 's_<(programlisting.*?)>_<\!--\1-->_g' $adoc
    pandoc -s -r docbook "$adoc" -t asciidoc --atx-headers --chapters -o "$adoc"
done