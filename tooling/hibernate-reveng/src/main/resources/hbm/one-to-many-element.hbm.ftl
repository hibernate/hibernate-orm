<one-to-many class="${elementValue.getAssociatedClass().getClassName()}"
	<#if !elementValue.getAssociatedClass().getClassName().equals(elementValue.getReferencedEntityName())> 
		entity-name="${elementValue.getReferencedEntityName()}"
	</#if>/>
