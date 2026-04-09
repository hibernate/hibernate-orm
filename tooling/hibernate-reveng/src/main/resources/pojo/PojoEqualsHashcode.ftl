<#--
~ Copyright 2010 - 2025 Red Hat, Inc.
~
~ Licensed under the Apache License, Version 2.0 (the "License");
~ you may not use this file except in compliance with the License.
~ You may obtain a copy of the License at
~
~     http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" basis,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
-->
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
         
<#list pojo.getAllPropertiesIterator() as property>        ${pojo.generateHashCode(property, "result", "this", jdk5)}
</#list>         return result;
   }   
</#if>