<#if helper.getCompositeIdField()??>
<#assign cid = helper.getCompositeIdField()>
    <composite-id
        name="${cid.getName()}"
        class="${helper.getCompositeIdClassName()}">
<#list helper.getAttributeOverrides(cid) as ao>
        <key-property name="${ao.fieldName()}">
            <column name="${ao.columnName()}"/>
        </key-property>
</#list>
<#list helper.getCompositeIdKeyManyToOnes() as km2o>
        <key-many-to-one name="${km2o.getName()}" class="${helper.getKeyManyToOneClassName(km2o)}">
            <column name="${helper.getKeyManyToOneColumnName(km2o)}"/>
        </key-many-to-one>
</#list>
    </composite-id>
<#else>
<#list helper.getIdFields() as field>
    <id
        name="${field.getName()}"
        type="${helper.getHibernateTypeName(field)}"<#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
<#assign genParams = helper.getGeneratorParameters(field)>
<#if genParams?has_content>
        <generator class="${helper.getGeneratorClass(field)}">
<#list genParams?keys as paramName>
            <param name="${paramName}">${genParams[paramName]}</param>
</#list>
        </generator>
<#else>
        <generator class="${helper.getGeneratorClass(field)}"/>
</#if>
    </id>
</#list>
</#if>
