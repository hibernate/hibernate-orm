    public ${pojo.getDeclarationName()}() {
    }
<#if pojo.needsFullConstructor()>

    public ${pojo.getDeclarationName()}(${pojo.getFullConstructorParameterList()}) {
<#list pojo.getFullConstructorProperties() as prop>
        this.${prop.fieldName()} = ${prop.fieldName()};
</#list>
    }
</#if>