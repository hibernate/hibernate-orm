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

<#--  /** default constructor */ -->
    public ${pojo.getDeclarationName()}() {
    }

<#if pojo.needsMinimalConstructor()>	<#-- /** minimal constructor */ -->
    public ${pojo.getDeclarationName()}(${c2j.asParameterList(pojo.getPropertyClosureForMinimalConstructor(), jdk5, pojo)}) {
<#if pojo.isSubclass() && !pojo.getPropertyClosureForSuperclassMinimalConstructor().isEmpty()>
        super(${c2j.asArgumentList(pojo.getPropertyClosureForSuperclassMinimalConstructor())});        
</#if>
<#list pojo.getPropertiesForMinimalConstructor() as field>
        this.${c2j.keyWordCheck(field.name)} = ${c2j.keyWordCheck(field.name)};
</#list>
    }
</#if>    
<#if pojo.needsFullConstructor()>
<#-- /** full constructor */ -->
    public ${pojo.getDeclarationName()}(${c2j.asParameterList(pojo.getPropertyClosureForFullConstructor(), jdk5, pojo)}) {
<#if pojo.isSubclass() && !pojo.getPropertyClosureForSuperclassFullConstructor().isEmpty()>
        super(${c2j.asArgumentList(pojo.getPropertyClosureForSuperclassFullConstructor())});        
</#if>
<#list pojo.getPropertiesForFullConstructor() as field>
       this.${c2j.keyWordCheck(field.name)} = ${c2j.keyWordCheck(field.name)};
</#list>
    }
</#if>    
