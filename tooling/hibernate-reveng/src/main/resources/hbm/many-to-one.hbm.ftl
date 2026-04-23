<#-- Many-to-one -->
<#list fields.getManyToOneFields() as field>
    <many-to-one
        name="${field.getName()}"
<#if fieldAttrs.isManyToOneEntityNameRef(field)>
        entity-name="${fieldAttrs.getManyToOneEntityName(field)}"<#else>
        class="${fieldAttrs.getTargetEntityName(field)}"</#if><#if fieldAttrs.getPropertyRef(field)??>
        property-ref="${fieldAttrs.getPropertyRef(field)}"</#if><#if fieldAttrs.isManyToOneLazy(field)>
        fetch="select"
        lazy="proxy"</#if><#if fieldAttrs.getFetchMode(field)??>
        fetch="${fieldAttrs.getFetchMode(field)}"</#if><#if !fieldAttrs.isManyToOneOptional(field)>
        not-null="true"</#if><#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if><#if fieldAttrs.getNotFoundAction(field)??>
        not-found="${fieldAttrs.getNotFoundAction(field)}"</#if><#if fieldAttrs.isOptimisticLockExcluded(field)>
        optimistic-lock="false"</#if><#if !fieldAttrs.isManyToOneUpdatable(field)>
        update="false"</#if><#if !fieldAttrs.isManyToOneInsertable(field)>
        insert="false"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#list fieldAttrs.getJoinColumnNames(field) as colName>
        <column name="${colName}"/>
</#list>
<#list fieldAttrs.getManyToOneFormulas(field) as formula>
        <formula>${formula}</formula>
</#list>
    </many-to-one>
</#list>
<#-- Constrained one-to-one with composite FK (rendered as many-to-one unique="true") -->
<#list fields.getConstrainedOneToOneAsM2OFields() as field>
    <many-to-one
        name="${field.getName()}"
        class="${fieldAttrs.getTargetEntityName(field)}"
        update="false"
        insert="false"
        unique="true">
<#list fieldAttrs.getJoinColumnNames(field) as colName>
        <column name="${colName}"/>
</#list>
    </many-to-one>
</#list>
<#-- One-to-one -->
<#list fields.getOneToOneFields() as field>
<#if (fieldAttrs.getFieldMetaAttributes(field)?size == 0)>
    <one-to-one
        name="${field.getName()}"
        class="${fieldAttrs.getTargetEntityName(field)}"<#if fieldAttrs.getOneToOneMappedBy(field)??>
        property-ref="${fieldAttrs.getOneToOneMappedBy(field)}"</#if><#if fieldAttrs.getOneToOneCascadeString(field)??>
        cascade="${fieldAttrs.getOneToOneCascadeString(field)}"</#if>
        constrained="${fieldAttrs.isOneToOneConstrained(field)?string('true', 'false')}"<#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>/>
<#else>
    <one-to-one
        name="${field.getName()}"
        class="${fieldAttrs.getTargetEntityName(field)}"<#if fieldAttrs.getOneToOneMappedBy(field)??>
        property-ref="${fieldAttrs.getOneToOneMappedBy(field)}"</#if><#if fieldAttrs.getOneToOneCascadeString(field)??>
        cascade="${fieldAttrs.getOneToOneCascadeString(field)}"</#if>
        constrained="${fieldAttrs.isOneToOneConstrained(field)?string('true', 'false')}"<#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
    </one-to-one>
</#if>
</#list>
