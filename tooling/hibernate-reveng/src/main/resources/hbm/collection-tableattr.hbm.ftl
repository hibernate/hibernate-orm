table="${value.collectionTable.quotedName}"
<#if value.collectionTable.catalog?exists && ((clazz.table.catalog?exists && clazz.table.catalog!=value.collectionTable.catalog) || (!clazz.table.catalog?exists && value.collectionTable.catalog?exists)) >
catalog="${value.collectionTable.catalog}"
</#if>
<#if value.collectionTable.schema?exists && ((clazz.table.schema?exists && clazz.table.schema!=value.collectionTable.schema) || (!clazz.table.schema?exists && value.collectionTable.schema?exists)) >
schema="${value.collectionTable.quotedSchema}"
</#if>