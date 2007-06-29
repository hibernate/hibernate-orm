//$Id: ElementPropertyMapping.java 6179 2005-03-23 15:41:48Z steveebersole $
package org.hibernate.persister.collection;

import org.hibernate.MappingException;

import org.hibernate.QueryException;

import org.hibernate.persister.entity.PropertyMapping;

import org.hibernate.type.Type;

import org.hibernate.util.StringHelper;

/**
 * @author Gavin King
 */
public class ElementPropertyMapping implements PropertyMapping {

	private final String[] elementColumns;
	private final Type type;

	public ElementPropertyMapping(String[] elementColumns, Type type)
	throws MappingException {
		this.elementColumns = elementColumns;
		this.type = type;
	}

	public Type toType(String propertyName) throws QueryException {
		if ( propertyName==null || "id".equals(propertyName) ) {
			return type;
		}
		else {
			throw new QueryException("cannot dereference scalar collection element: " + propertyName);
		}
	}

	public String[] toColumns(String alias, String propertyName) throws QueryException {
		if (propertyName==null || "id".equals(propertyName) ) {
			return StringHelper.qualify(alias, elementColumns);
		}
		else {
			throw new QueryException("cannot dereference scalar collection element: " + propertyName);
		}
	}

	/**
	 * Given a property path, return the corresponding column name(s).
	 */
	public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException {
		throw new UnsupportedOperationException( "References to collections must be define a SQL alias" );
	}

	public Type getType() {
		return type;
	}

}