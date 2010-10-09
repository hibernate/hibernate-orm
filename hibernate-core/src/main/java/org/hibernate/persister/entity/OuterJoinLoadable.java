/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.persister.entity;

import org.hibernate.FetchMode;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * A <tt>EntityPersister</tt> that may be loaded by outer join using
 * the <tt>OuterJoinLoader</tt> hierarchy and may be an element
 * of a one-to-many association.
 *
 * @see org.hibernate.loader.OuterJoinLoader
 * @author Gavin King
 */
public interface OuterJoinLoadable extends Loadable, Joinable {

	/**
	 * Generate a list of collection index, key and element columns
	 */
	public String selectFragment(String alias, String suffix);
	/**
	 * How many properties are there, for this class and all subclasses?
	 */
	public int countSubclassProperties();

	/**
	 * May this (subclass closure) property be fetched using an SQL outerjoin?
	 */
	public FetchMode getFetchMode(int i);
	/**
	 * Get the cascade style of this (subclass closure) property
	 */
	public CascadeStyle getCascadeStyle(int i);

	/**
	 * Is this property defined on a subclass of the mapped class.
	 */
	public boolean isDefinedOnSubclass(int i);

	/**
	 * Get the type of the numbered property of the class or a subclass.
	 */
	public Type getSubclassPropertyType(int i);

	/**
	 * Get the name of the numbered property of the class or a subclass.
	 */
	public String getSubclassPropertyName(int i);
	
	/**
	 * Is the numbered property of the class of subclass nullable?
	 */
	public boolean isSubclassPropertyNullable(int i);

	/**
	 * Return the column names used to persist the numbered property of the
	 * class or a subclass.
	 */
	public String[] getSubclassPropertyColumnNames(int i);

	/**
	 * Return the table name used to persist the numbered property of the
	 * class or a subclass.
	 */
	public String getSubclassPropertyTableName(int i);
	/**
	 * Given the number of a property of a subclass, and a table alias,
	 * return the aliased column names.
	 */
	public String[] toColumns(String name, int i);

	/**
	 * Get the main from table fragment, given a query alias.
	 */
	public String fromTableFragment(String alias);

	/**
	 * Get the column names for the given property path
	 */
	public String[] getPropertyColumnNames(String propertyPath);
	/**
	 * Get the table name for the given property path
	 */
	public String getPropertyTableName(String propertyName);
	
	public EntityType getEntityType();

}
