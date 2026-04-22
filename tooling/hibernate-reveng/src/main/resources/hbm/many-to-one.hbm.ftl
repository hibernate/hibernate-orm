<#-- Many-to-one -->
<#list helper.getManyToOneFields() as field>
    <many-to-one
        name="${field.getName()}"
<#if helper.isManyToOneEntityNameRef(field)>
        entity-name="${helper.getManyToOneEntityName(field)}"<#else>
        class="${helper.getTargetEntityName(field)}"</#if><#if helper.getPropertyRef(field)??>
        property-ref="${helper.getPropertyRef(field)}"</#if><#if helper.isManyToOneLazy(field)>
        fetch="select"
        lazy="proxy"</#if><#if helper.getFetchMode(field)??>
        fetch="${helper.getFetchMode(field)}"</#if><#if !helper.isManyToOneOptional(field)>
        not-null="true"</#if><#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if><#if helper.getNotFoundAction(field)??>
        not-found="${helper.getNotFoundAction(field)}"</#if><#if helper.isOptimisticLockExcluded(field)>
        optimistic-lock="false"</#if><#if !helper.isManyToOneUpdatable(field)>
        update="false"</#if><#if !helper.isManyToOneInsertable(field)>
        insert="false"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#list helper.getJoinColumnNames(field) as colName>
        <column name="${colName}"/>
</#list>
<#list helper.getManyToOneFormulas(field) as formula>
        <formula>${formula}</formula>
</#list>
    </many-to-one>
</#list>
<#-- Constrained one-to-one with composite FK (rendered as many-to-one unique="true") -->
<#list helper.getConstrainedOneToOneAsM2OFields() as field>
    <many-to-one
        name="${field.getName()}"
        class="${helper.getTargetEntityName(field)}"
        update="false"
        insert="false"
        unique="true">
<#list helper.getJoinColumnNames(field) as colName>
        <column name="${colName}"/>
</#list>
    </many-to-one>
</#list>
<#-- One-to-one -->
<#list helper.getOneToOneFields() as field>
<#if (helper.getFieldMetaAttributes(field)?size == 0)>
    <one-to-one
        name="${field.getName()}"
        class="${helper.getTargetEntityName(field)}"<#if helper.getOneToOneMappedBy(field)??>
        property-ref="${helper.getOneToOneMappedBy(field)}"</#if><#if helper.getOneToOneCascadeString(field)??>
        cascade="${helper.getOneToOneCascadeString(field)}"</#if>
        constrained="${helper.isOneToOneConstrained(field)?string('true', 'false')}"<#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>/>
<#else>
    <one-to-one
        name="${field.getName()}"
        class="${helper.getTargetEntityName(field)}"<#if helper.getOneToOneMappedBy(field)??>
        property-ref="${helper.getOneToOneMappedBy(field)}"</#if><#if helper.getOneToOneCascadeString(field)??>
        cascade="${helper.getOneToOneCascadeString(field)}"</#if>
        constrained="${helper.isOneToOneConstrained(field)?string('true', 'false')}"<#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
    </one-to-one>
</#if>
</#list>
