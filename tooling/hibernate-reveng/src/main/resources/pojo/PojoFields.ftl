<#-- // Fields -->

<#foreach field in pojo.getAllPropertiesIterator()><#if pojo.getMetaAttribAsBool(field, "gen-property", true)> <#if pojo.hasMetaAttribute(field, "field-description")>    /**
     ${pojo.getFieldJavaDoc(field, 0)}
     */
 </#if>    ${pojo.getFieldModifiers(field)} ${pojo.getJavaTypeName(field, jdk5)} ${c2j.keyWordCheck(field.name)}<#if pojo.hasFieldInitializor(field, jdk5)> = ${pojo.getFieldInitialization(field, jdk5)}</#if>;
</#if>
</#foreach>
