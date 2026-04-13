<#if helper.getCompositeIdField()??>
<#assign cid = helper.getCompositeIdField()>
    <composite-id
        name="${cid.getName()}"
        class="${helper.getCompositeIdClassName()}">
<#list helper.getCompositeIdKeyProperties() as field>
<#assign hbType = helper.getHibernateTypeName(field)>
<#assign colAttrs = helper.getColumnAttributes(field)>
        <key-property name="${field.getName()}"<#if hbType != "java.lang.Object">
            type="${hbType}"</#if>>
<#if helper.getColumnComment(field)??>
            <column name="${helper.getColumnName(field)}"<#if colAttrs?has_content> ${colAttrs}</#if>>
                <comment>${helper.getColumnComment(field)}</comment>
            </column>
<#else>
            <column name="${helper.getColumnName(field)}"<#if colAttrs?has_content> ${colAttrs}</#if>/>
</#if>
        </key-property>
</#list>
<#list helper.getCompositeIdKeyManyToOnes() as km2o>
        <key-many-to-one name="${km2o.getName()}" class="${helper.getKeyManyToOneClassName(km2o)}">
            <column name="${helper.getKeyManyToOneColumnName(km2o)}"/>
        </key-many-to-one>
</#list>
    </composite-id>
<#elseif helper.hasIdClass()>
    <composite-id
        class="${helper.getIdClassName()}"
        mapped="true">
<#list helper.getIdFields() as field>
        <key-property
            name="${field.getName()}"
            type="${helper.getHibernateTypeName(field)}">
<#if helper.getColumnComment(field)??>
            <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}>
                <comment>${helper.getColumnComment(field)}</comment>
            </column>
<#else>
            <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
</#if>
        </key-property>
</#list>
    </composite-id>
<#else>
<#list helper.getIdFields() as field>
    <id
        name="${field.getName()}"
        type="${helper.getHibernateTypeName(field)}"<#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if helper.getColumnComment(field)??>
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}>
            <comment>${helper.getColumnComment(field)}</comment>
        </column>
<#else>
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
</#if>
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
