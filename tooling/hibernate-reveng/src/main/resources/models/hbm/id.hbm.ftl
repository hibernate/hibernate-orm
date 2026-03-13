<#if table.getCompositeId()??>
<#assign cid = table.getCompositeId()>
    <composite-id
        name="${cid.getFieldName()}"
        class="${cid.getIdClassPackage()}.${cid.getIdClassName()}">
<#list cid.getAttributeOverrides() as ao>
        <key-property name="${ao.getFieldName()}">
            <column name="${ao.getColumnName()}"/>
        </key-property>
</#list>
    </composite-id>
<#else>
<#list table.getColumns() as col>
<#if col.isPrimaryKey()>
    <id
        name="${col.getFieldName()}"
        type="${helper.getHibernateTypeName(col)}">
        <column name="${col.getColumnName()}" ${helper.getColumnAttributes(col)}/>
        <generator class="${helper.getGeneratorClass(col.getGenerationType())}"/>
    </id>
</#if>
</#list>
</#if>
