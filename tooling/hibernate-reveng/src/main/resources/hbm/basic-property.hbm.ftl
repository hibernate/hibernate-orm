<#-- Basic properties (skip PK, version, FK columns) -->
<#list fields.getBasicFields() as field>
    <property name="${field.getName()}"<#if !fieldAttrs.hasTypeParameters(field)>
        type="${fieldAttrs.getHibernateTypeName(field)}"</#if><#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if><#if !fieldAttrs.isPropertyUpdatable(field)>
        update="false"</#if><#if !fieldAttrs.isPropertyInsertable(field)>
        insert="false"</#if><#if fieldAttrs.isPropertyLazy(field)>
        lazy="true"</#if><#if fieldAttrs.isOptimisticLockExcluded(field)>
        optimistic-lock="false"</#if><#if fieldAttrs.getFormula(field)??>
        formula="${fieldAttrs.getFormula(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if fieldAttrs.hasTypeParameters(field)>
        <type name="${fieldAttrs.getHibernateTypeName(field)}">
<#list fieldAttrs.getTypeParameters(field)?keys as paramName>
            <param name="${paramName}">${fieldAttrs.getTypeParameters(field)[paramName]}</param>
</#list>
        </type>
</#if>
<#if !fieldAttrs.getFormula(field)??>
<#if fieldAttrs.getColumnComment(field)??>
        <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}>
            <comment>${fieldAttrs.getColumnComment(field)}</comment>
        </column>
<#else>
        <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}/>
</#if>
</#if>
    </property>
</#list>
