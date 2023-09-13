<#if pojo.needsToString()>    /**
     * toString
     * @return String
     */
     public String toString() {
	  StringBuffer buffer = new StringBuffer();

      buffer.append(getClass().getName()).append("@").append(Integer.toHexString(hashCode())).append(" [");
<#list pojo.getToStringPropertiesIterator() as property>      buffer.append("${property.getName()}").append("='").append(${pojo.getGetterSignature(property)}()).append("' ");			
</#list>      buffer.append("]");
      
      return buffer.toString();
     }
</#if>