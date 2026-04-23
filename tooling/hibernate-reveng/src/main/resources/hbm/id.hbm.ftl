<#if fields.getCompositeIdField()??>
<#assign cid = fields.getCompositeIdField()>
    <composite-id
        name="${cid.getName()}"
        class="${fields.getCompositeIdClassName()}">
<#list fields.getCompositeIdAllFields() as field>
<#if fields.isManyToOneField(field)>
        <key-many-to-one name="${field.getName()}" class="${fields.getKeyManyToOneClassName(field)}">
<#list fields.getKeyManyToOneColumnNames(field) as colName>
            <column name="${colName}"/>
</#list>
        </key-many-to-one>
<#else>
<#assign hbType = fieldAttrs.getHibernateTypeName(field)>
<#assign colAttrs = fieldAttrs.getColumnAttributes(field)>
        <key-property name="${field.getName()}"<#if hbType != "java.lang.Object">
            type="${hbType}"</#if>>
<#if fieldAttrs.getColumnComment(field)??>
            <column name="${fieldAttrs.getColumnName(field)}"<#if colAttrs?has_content> ${colAttrs}</#if>>
                <comment>${fieldAttrs.getColumnComment(field)}</comment>
            </column>
<#else>
            <column name="${fieldAttrs.getColumnName(field)}"<#if colAttrs?has_content> ${colAttrs}</#if>/>
</#if>
        </key-property>
</#if>
</#list>
    </composite-id>
<#elseif fields.hasIdClass()>
    <composite-id
        class="${fields.getIdClassName()}"
        mapped="true">
<#list fields.getIdFields() as field>
        <key-property
            name="${field.getName()}"
            type="${fieldAttrs.getHibernateTypeName(field)}">
<#if fieldAttrs.getColumnComment(field)??>
            <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}>
                <comment>${fieldAttrs.getColumnComment(field)}</comment>
            </column>
<#else>
            <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}/>
</#if>
        </key-property>
</#list>
    </composite-id>
<#elseif (fields.getIdFields()?size > 1)>
    <composite-id>
<#list fields.getIdFields() as field>
<#if fields.isManyToOneField(field)>
        <key-many-to-one name="${field.getName()}" class="${fieldAttrs.getTargetEntityName(field)}">
<#list assocAttrs.getJoinColumnNames(field) as colName>
            <column name="${colName}"/>
</#list>
        </key-many-to-one>
<#else>
<#assign hbType = fieldAttrs.getHibernateTypeName(field)>
<#assign colAttrs = fieldAttrs.getColumnAttributes(field)>
        <key-property name="${field.getName()}"<#if hbType != "java.lang.Object">
            type="${hbType}"</#if>>
<#if fieldAttrs.getColumnComment(field)??>
            <column name="${fieldAttrs.getColumnName(field)}"<#if colAttrs?has_content> ${colAttrs}</#if>>
                <comment>${fieldAttrs.getColumnComment(field)}</comment>
            </column>
<#else>
            <column name="${fieldAttrs.getColumnName(field)}"<#if colAttrs?has_content> ${colAttrs}</#if>/>
</#if>
        </key-property>
</#if>
</#list>
    </composite-id>
<#else>
<#list fields.getIdFields() as field>
    <id
        name="${field.getName()}"
        type="${fieldAttrs.getHibernateTypeName(field)}"<#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if fieldAttrs.getColumnComment(field)??>
        <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}>
            <comment>${fieldAttrs.getColumnComment(field)}</comment>
        </column>
<#else>
        <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}/>
</#if>
<#assign genParams = fieldAttrs.getGeneratorParameters(field)>
<#if genParams?has_content>
        <generator class="${fieldAttrs.getGeneratorClass(field)}">
<#list genParams?keys as paramName>
            <param name="${paramName}">${genParams[paramName]}</param>
</#list>
        </generator>
<#else>
        <generator class="${fieldAttrs.getGeneratorClass(field)}"/>
</#if>
    </id>
</#list>
</#if>
