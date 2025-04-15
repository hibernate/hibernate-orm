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
<#macro header selected="">
	<div id="header">

		<div id="logo">
			<a href="http://www.hibernate.org/" target="_blank">
				<#assign src = docFileManager.getRef(docFile, docFileManager.getHibernateImageDocFile())>
				<img src="${src}" alt="Hibernate"/>
			</a>
		</div>

		<#-- unfortunately whitespace is significant here in browsers.. -->
		<ul><#--
		 --><@headerItem id="tables" label="Tables" href=docFileManager.getRef(docFile, docFileManager.getTableIndexDocFile()) selected=selected/><#--
		 --><@headerItem id="entities" label="Entities" href=docFileManager.getRef(docFile, docFileManager.getClassIndexDocFile()) selected=selected/><#--
	 --></ul>

		<hr/>

	</div>
</#macro>

<#macro headerItem id label href selected>
	<#-- unfortunately whitespace is significant here in browsers.. -->
	<#if selected == id><#--
	 --><li class="selected"><#--
		 -->${label}<#--
	 --></li><#--
 --><#else><#--
	 --><li><#--
		 --><a href="${href}" target="_top"><#--
			 -->${label}<#--
		 --></a><#--
	 --></li><#--
 --></#if>
</#macro>