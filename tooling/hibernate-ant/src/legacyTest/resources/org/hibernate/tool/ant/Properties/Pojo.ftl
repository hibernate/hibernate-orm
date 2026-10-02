hbm2java.weirdAl: ${.data_model["hbm2java.weirdAl"]}
ant.project.name: ${.data_model["ant.project.name"]}
<#if !.data_model["foo.weirdAl"]?exists>foo.weirdAl does not exist</#if>
bar: ${bar}
file: ${file}
value: ${value}
