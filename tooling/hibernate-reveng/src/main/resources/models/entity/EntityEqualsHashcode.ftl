<#if !templateHelper.isSubclass() && templateHelper.needsEqualsHashCode()>
<#if templateHelper.hasExplicitEqualsColumns()>
<#assign eqCols = templateHelper.getEqualsColumns()>
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof ${templateHelper.getDeclarationName()})) return false;
        ${templateHelper.getDeclarationName()} castOther = (${templateHelper.getDeclarationName()}) other;
        return <#list eqCols as col>${templateHelper.generateEqualsExpression(col)}<#if col_has_next>
                && </#if></#list>;
    }

    @Override
    public int hashCode() {
        int result = 17;
<#list eqCols as col>
        result = 37 * result + ${templateHelper.generateHashCodeExpression(col)};
</#list>
        return result;
    }
<#elseif templateHelper.hasCompositeId()>
<#assign idField = templateHelper.getTable().getCompositeId().getFieldName()>
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
<#elseif (templateHelper.getIdentifierColumns()?size > 0)>
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof ${templateHelper.getDeclarationName()})) return false;
        ${templateHelper.getDeclarationName()} castOther = (${templateHelper.getDeclarationName()}) other;
        return <#list templateHelper.getIdentifierColumns() as col>${templateHelper.generateEqualsExpression(col)}<#if col_has_next>
                && </#if></#list>;
    }

    @Override
    public int hashCode() {
        int result = 17;
<#list templateHelper.getIdentifierColumns() as col>
        result = 37 * result + ${templateHelper.generateHashCodeExpression(col)};
</#list>
        return result;
    }
</#if>
</#if>