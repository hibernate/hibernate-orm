<#if (templateHelper.getToStringProperties()?size > 0)>
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getClass().getName()).append("@").append(Integer.toHexString(hashCode())).append(" [");
<#list templateHelper.getToStringProperties() as prop>
        buffer.append("${prop.fieldName()}").append("='").append(${prop.getterName()}()).append("' ");
</#list>
        buffer.append("]");
        return buffer.toString();
    }
</#if>