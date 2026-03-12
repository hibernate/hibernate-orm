    public ${templateHelper.getDeclarationName()}() {
    }
<#if templateHelper.needsFullConstructor()>

    public ${templateHelper.getDeclarationName()}(${templateHelper.getFullConstructorParameterList()}) {
<#list templateHelper.getFullConstructorProperties() as prop>
        this.${prop.fieldName()} = ${prop.fieldName()};
</#list>
    }
</#if>