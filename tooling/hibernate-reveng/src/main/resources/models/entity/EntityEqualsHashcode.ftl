<#if !pojo.isSubclass()>
<#if pojo.hasCompositeId()>
<#assign idField = pojo.getTable().getCompositeId().getFieldName()>
<#assign idGetter = pojo.getGetterName(idField)>
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof ${pojo.getDeclarationName()})) return false;
        ${pojo.getDeclarationName()} castOther = (${pojo.getDeclarationName()}) other;
        return ((${idGetter}() == castOther.${idGetter}()) || (${idGetter}() != null && castOther.${idGetter}() != null && ${idGetter}().equals(castOther.${idGetter}())));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (${idGetter}() == null ? 0 : ${idGetter}().hashCode());
        return result;
    }
<#elseif (pojo.getIdentifierColumns()?size > 0)>
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof ${pojo.getDeclarationName()})) return false;
        ${pojo.getDeclarationName()} castOther = (${pojo.getDeclarationName()}) other;
        return <#list pojo.getIdentifierColumns() as col>${pojo.generateEqualsExpression(col)}<#if col_has_next>
                && </#if></#list>;
    }

    @Override
    public int hashCode() {
        int result = 17;
<#list pojo.getIdentifierColumns() as col>
        result = 37 * result + ${pojo.generateHashCodeExpression(col)};
</#list>
        return result;
    }
</#if>
</#if>