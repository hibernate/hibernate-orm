<#-- Many-to-one -->
<#list fields.getManyToOneFields() as field>
    <many-to-one
        name="${field.getName()}"
<#if assocAttrs.isManyToOneEntityNameRef(field)>
        entity-name="${assocAttrs.getManyToOneEntityName(field)}"<#else>
        class="${fieldAttrs.getTargetEntityName(field)}"</#if><#if assocAttrs.getPropertyRef(field)??>
        property-ref="${assocAttrs.getPropertyRef(field)}"</#if><#if assocAttrs.isManyToOneLazy(field)>
        fetch="select"
        lazy="proxy"</#if><#if fieldAttrs.getFetchMode(field)??>
        fetch="${fieldAttrs.getFetchMode(field)}"</#if><#if !assocAttrs.isManyToOneOptional(field)>
        not-null="true"</#if><#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if><#if fieldAttrs.getNotFoundAction(field)??>
        not-found="${fieldAttrs.getNotFoundAction(field)}"</#if><#if fieldAttrs.isOptimisticLockExcluded(field)>
        optimistic-lock="false"</#if><#if !assocAttrs.isManyToOneUpdatable(field)>
        update="false"</#if><#if !assocAttrs.isManyToOneInsertable(field)>
        insert="false"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#list assocAttrs.getJoinColumnNames(field) as colName>
        <column name="${colName}"/>
</#list>
<#list assocAttrs.getManyToOneFormulas(field) as formula>
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
<#list assocAttrs.getJoinColumnNames(field) as colName>
        <column name="${colName}"/>
</#list>
    </many-to-one>
</#list>
<#-- One-to-one -->
<#list fields.getOneToOneFields() as field>
<#if (fieldAttrs.getFieldMetaAttributes(field)?size == 0)>
    <one-to-one
        name="${field.getName()}"
        class="${fieldAttrs.getTargetEntityName(field)}"<#if assocAttrs.getOneToOneMappedBy(field)??>
        property-ref="${assocAttrs.getOneToOneMappedBy(field)}"</#if><#if assocAttrs.getOneToOneCascadeString(field)??>
        cascade="${assocAttrs.getOneToOneCascadeString(field)}"</#if>
        constrained="${assocAttrs.isOneToOneConstrained(field)?string('true', 'false')}"<#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>/>
<#else>
    <one-to-one
        name="${field.getName()}"
        class="${fieldAttrs.getTargetEntityName(field)}"<#if assocAttrs.getOneToOneMappedBy(field)??>
        property-ref="${assocAttrs.getOneToOneMappedBy(field)}"</#if><#if assocAttrs.getOneToOneCascadeString(field)??>
        cascade="${assocAttrs.getOneToOneCascadeString(field)}"</#if>
        constrained="${assocAttrs.isOneToOneConstrained(field)?string('true', 'false')}"<#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
    </one-to-one>
</#if>
</#list>
