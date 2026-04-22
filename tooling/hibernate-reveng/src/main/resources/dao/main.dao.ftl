<#--
  ~ Copyright 2004 - 2025 Red Hat, Inc.
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
<#assign classbody>
<#assign declarationName = helper.importType(helper.getQualifiedDeclarationName())>/**
 * Home object for domain model class ${declarationName}.
 * @see ${helper.getQualifiedDeclarationName()}
 * @author Hibernate Tools
 */
<#if helper.isEjb3()>
@${helper.importType("jakarta.ejb.Stateless")}
</#if>
public class ${declarationName}Home {

    private static final ${helper.importType("java.util.logging.Logger")} logger = ${helper.importType("java.util.logging.Logger")}.getLogger(${helper.getDeclarationName()}Home.class.getName());

<#if helper.isEjb3()>
<#include "dao-ejb3.dao.ftl"/>
<#else>
<#include "dao-session.dao.ftl"/>
</#if>
}
</#assign>

${helper.getPackageDeclaration()}
// Generated ${date?datetime} by Hibernate Tools ${version}

${helper.generateImports()}
${classbody}
