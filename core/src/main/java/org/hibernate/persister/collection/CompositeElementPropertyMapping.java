//$Id: CompositeElementPropertyMapping.java 6136 2005-03-21 18:15:29Z oneovthafew $
package org.hibernate.persister.collection;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.persister.entity.AbstractPropertyMapping;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class CompositeElementPropertyMapping extends AbstractPropertyMapping {

	private final AbstractComponentType compositeType;
	
	public CompositeElementPropertyMapping(
			String[] elementColumns, 
			String[] elementFormulaTemplates, 
			AbstractComponentType compositeType, 
			Mapping factory)
	throws MappingException {

		this.compositeType = compositeType;

		initComponentPropertyPaths(null, compositeType, elementColumns, elementFormulaTemplates, factory);

	}

	public Type getType() {
		return compositeType;
	}

	protected String getEntityName() {
		return compositeType.getName();
	}

}