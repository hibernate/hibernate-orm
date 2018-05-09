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