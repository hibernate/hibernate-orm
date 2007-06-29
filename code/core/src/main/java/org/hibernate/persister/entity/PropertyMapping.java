//$Id: PropertyMapping.java 6179 2005-03-23 15:41:48Z steveebersole $
package org.hibernate.persister.entity;

import org.hibernate.QueryException;
import org.hibernate.type.Type;

/**
 * Abstraction of all mappings that define properties:
 * entities, collection elements.
 *
 * @author Gavin King
 */
public interface PropertyMapping {
	// TODO: It would be really, really nice to use this to also model components!
	/**
	 * Given a component path expression, get the type of the property
	 */
	public Type toType(String propertyName) throws QueryException;
	/**
	 * Given a query alias and a property path, return the qualified
	 * column name
	 */
	public String[] toColumns(String alias, String propertyName) throws QueryException;
	/**
	 * Given a property path, return the corresponding column name(s).
	 */
	public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException;
	/**
	 * Get the type of the thing containing the properties
	 */
	public Type getType();
}
