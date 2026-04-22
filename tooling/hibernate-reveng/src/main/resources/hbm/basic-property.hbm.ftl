<#-- Basic properties (skip PK, version, FK columns) -->
<#list helper.getBasicFields() as field>
    <property name="${field.getName()}"<#if !helper.hasTypeParameters(field)>
        type="${helper.getHibernateTypeName(field)}"</#if><#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if><#if !helper.isPropertyUpdatable(field)>
        update="false"</#if><#if !helper.isPropertyInsertable(field)>
        insert="false"</#if><#if helper.isPropertyLazy(field)>
        lazy="true"</#if><#if helper.isOptimisticLockExcluded(field)>
        optimistic-lock="false"</#if><#if helper.getFormula(field)??>
        formula="${helper.getFormula(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if helper.hasTypeParameters(field)>
        <type name="${helper.getHibernateTypeName(field)}">
<#list helper.getTypeParameters(field)?keys as paramName>
            <param name="${paramName}">${helper.getTypeParameters(field)[paramName]}</param>
</#list>
        </type>
</#if>
<#if !helper.getFormula(field)??>
<#if helper.getColumnComment(field)??>
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}>
            <comment>${helper.getColumnComment(field)}</comment>
        </column>
<#else>
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
</#if>
</#if>
    </property>
</#list>
