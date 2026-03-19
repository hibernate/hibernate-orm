<#if !templateHelper.isSubclass() && templateHelper.needsEqualsHashCode()>
<#if templateHelper.hasExplicitEqualsColumns()>
<#assign eqFields = templateHelper.getEqualsFields()>
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof ${templateHelper.getDeclarationName()})) return false;
        ${templateHelper.getDeclarationName()} castOther = (${templateHelper.getDeclarationName()}) other;
        return <#list eqFields as field>${templateHelper.generateEqualsExpression(field)}<#if field_has_next>
                && </#if></#list>;
    }

    @Override
    public int hashCode() {
        int result = 17;
<#list eqFields as field>
        result = 37 * result + ${templateHelper.generateHashCodeExpression(field)};
</#list>
        return result;
    }
<#elseif templateHelper.hasCompositeId()>
<#assign idField = templateHelper.getCompositeIdField().getName()>
<#assign idGetter = templateHelper.getGetterName(idField)>
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof ${templateHelper.getDeclarationName()})) return false;
        ${templateHelper.getDeclarationName()} castOther = (${templateHelper.getDeclarationName()}) other;
        return ((${idGetter}() == castOther.${idGetter}()) || (${idGetter}() != null && castOther.${idGetter}() != null && ${idGetter}().equals(castOther.${idGetter}())));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (${idGetter}() == null ? 0 : ${idGetter}().hashCode());
        return result;
    }
<#elseif (templateHelper.getIdentifierFields()?size > 0)>
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof ${templateHelper.getDeclarationName()})) return false;
        ${templateHelper.getDeclarationName()} castOther = (${templateHelper.getDeclarationName()}) other;
        return <#list templateHelper.getIdentifierFields() as field>${templateHelper.generateEqualsExpression(field)}<#if field_has_next>
                && </#if></#list>;
    }

    @Override
    public int hashCode() {
        int result = 17;
<#list templateHelper.getIdentifierFields() as field>
        result = 37 * result + ${templateHelper.generateHashCodeExpression(field)};
</#list>
        return result;
    }
</#if>
</#if>