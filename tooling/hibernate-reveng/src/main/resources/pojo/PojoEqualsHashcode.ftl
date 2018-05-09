<#if pojo.needsEqualsHashCode() && !clazz.superclass?exists>
<#assign classNameToCastTo><#if clazz.getProxyInterfaceName?exists>${clazz.getProxyInterfaceName()}<#else>${pojo.getDeclarationName()}</#if></#assign>
   public boolean equals(Object other) {
         if ( (this == other ) ) return true;
		 if ( (other == null ) ) return false;
		 if ( !(other instanceof ${classNameToCastTo}) ) return false;
		 ${classNameToCastTo} castOther = ( ${classNameToCastTo} ) other; 
         
		 return ${pojo.generateEquals("this", "castOther", jdk5)};
   }
   
   public int hashCode() {
         int result = 17;
         
<#foreach property in pojo.getAllPropertiesIterator()>         ${pojo.generateHashCode(property, "result", "this", jdk5)}
</#foreach>         return result;
   }   
</#if>