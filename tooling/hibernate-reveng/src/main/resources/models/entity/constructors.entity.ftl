    public ${templateHelper.getDeclarationName()}() {
    }
<#if templateHelper.needsMinimalConstructor()>

    public ${templateHelper.getDeclarationName()}(${templateHelper.getMinimalConstructorParameterList()}) {
<#list templateHelper.getMinimalConstructorProperties() as prop>
        this.${prop.fieldName()} = ${prop.fieldName()};
</#list>
    }
</#if>
<#if templateHelper.needsFullConstructor()>

    public ${templateHelper.getDeclarationName()}(${templateHelper.getFullConstructorParameterList()}) {
<#list templateHelper.getFullConstructorProperties() as prop>
        this.${prop.fieldName()} = ${prop.fieldName()};
</#list>
    }
</#if>