/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.FetchMode;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * A {@link EntityPersister} that may be loaded by outer join using
 * and may be an element of a one-to-many association.
 *
 * @author Gavin King
 *
 * @deprecated Use {@link EntityMappingType}
 */
@Deprecated(since = "6", forRemoval = true)
public interface OuterJoinLoadable extends Loadable, Joinable {

	/**
	 * Generate a list of collection index, key and element columns
	 */
	String selectFragment(String alias, String suffix);
	/**
	 * How many properties are there, for this class and all subclasses?
	 */
	int countSubclassProperties();

	/**
	 * May this (subclass closure) property be fetched using an SQL outerjoin?
	 */
	FetchMode getFetchMode(int i);
	/**
	 * Get the cascade style of this (subclass closure) property
	 */
	CascadeStyle getCascadeStyle(int i);

	/**
	 * Is this property defined on a subclass of the mapped class.
	 */
	boolean isDefinedOnSubclass(int i);

	/**
	 * Get the type of the numbered property of the class or a subclass.
	 */
	Type getSubclassPropertyType(int i);

	/**
	 * Get the name of the numbered property of the class or a subclass.
	 */
	String getSubclassPropertyName(int i);
	
	/**
	 * Is the numbered property of the class of subclass nullable?
	 */
	boolean isSubclassPropertyNullable(int i);

	/**
	 * Return the column names used to persist the numbered property of the
	 * class or a subclass.
	 */
	String[] getSubclassPropertyColumnNames(int i);

	/**
	 * Return the table name used to persist the numbered property of the
	 * class or a subclass.
	 */
	String getSubclassPropertyTableName(int i);

	/**
	 * The name of the table to use when performing mutations (INSERT,UPDATE,DELETE)
	 * for the given attribute
	 */
	default String getAttributeMutationTableName(int attributeIndex) {
		return getSubclassPropertyTableName( attributeIndex );
	}

	/**
	 * Given the number of a property of a subclass, and a table alias,
	 * return the aliased column names.
	 */
	String[] toColumns(String name, int i);

	/**
	 * Get the main from table fragment, given a query alias.
	 */
	String fromTableFragment(String alias);

	/**
	 * Get the column names for the given property path
	 */
	String[] getPropertyColumnNames(String propertyPath);
	/**
	 * Get the table name for the given property path
	 */
	String getPropertyTableName(String propertyName);
	
	EntityType getEntityType();

}
