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
    </composite-id>
<#else>
<#list helper.getIdFields() as field>
    <id
        name="${field.getName()}"
        type="${helper.getHibernateTypeName(field)}">
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
        <generator class="${helper.getGeneratorClass(field)}"/>
    </id>
</#list>
</#if>
