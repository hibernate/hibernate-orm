/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.Type;

/**
 * An instance of <tt>CriteriaQuery</tt> is passed to criterion, 
 * order and projection instances when actually compiling and
 * executing the query. This interface is not used by application
 * code.
 * 
 * @author Gavin King
 */
public interface CriteriaQuery {
	/**
	 * Provides access to the SessionFactory
	 *
	 * @return The SessionFactory
	 */
	public SessionFactoryImplementor getFactory();
	
	/**
	 * Resolve a property path to the name of the column it maps to.  Ignores projection aliases.
	 *
	 * @param criteria The overall criteria
	 * @param propertyPath The property path to resolve
	 *
	 * @return The column name
	 *
	 * @throws HibernateException if the property maps to more than 1 column, or if the property could not be resolved
	 *
	 * @see #getColumns
	 */
	public String getColumn(Criteria criteria, String propertyPath) throws HibernateException;

	/**
	 * Resolve a property path to the names of the columns it maps to.  Ignores projection aliases
	 *
	 * @param criteria The criteria
	 * @param propertyPath The property path to resolve
	 *
	 * @return The column names
	 *
	 * @throws HibernateException if the property maps to more than 1 column, or if the property could not be resolved
	 */
	public String[] getColumns(String propertyPath, Criteria criteria) throws HibernateException;

	/**
	 * Get the names of the columns mapped by a property path; if the property path is not found in criteria, try
	 * the "outer" query.  Projection aliases are ignored.
	 *
	 * @param criteria The criteria
	 * @param propertyPath The property path to resolve
	 *
	 * @return The column names
	 *
	 * @throws HibernateException if the property could not be resolved
	 */
	public String[] findColumns(String propertyPath, Criteria criteria) throws HibernateException;

	/**
	 * Get the type of a property path.
	 *
	 * @param criteria The criteria
	 * @param propertyPath The property path to resolve
	 *
	 * @return The type
	 *
	 * @throws HibernateException if the property could not be resolved
	 */
	public Type getType(Criteria criteria, String propertyPath) throws HibernateException;

	/**
	 * Get the names of the columns mapped by a property path.  Here, the property path can refer to
	 * a projection alias.
	 *
	 * @param criteria The criteria
	 * @param propertyPath The property path to resolve or projection alias
	 *
	 * @return The column names
	 *
	 * @throws HibernateException if the property/alias could not be resolved
	 */
	public String[] getColumnsUsingProjection(Criteria criteria, String propertyPath) throws HibernateException;

	/**
	 * Get the type of a property path.  Here, the property path can refer to a projection alias.
	 *
	 * @param criteria The criteria
	 * @param propertyPath The property path to resolve or projection alias
	 *
	 * @return The type
	 *
	 * @throws HibernateException if the property/alias could not be resolved
	 */
	public Type getTypeUsingProjection(Criteria criteria, String propertyPath) throws HibernateException;

	/**
	 * Build a typed-value for the property/value combo.  Essentially the same as manually building a TypedValue
	 * using the given value and the resolved type using {@link #getTypeUsingProjection}.
	 *
	 * @param criteria The criteria query
	 * @param propertyPath The property path/alias to resolve to type.
	 * @param value The value
	 *
	 * @return The TypedValue
	 *
	 * @throws HibernateException if the property/alias could not be resolved
	 */
	public TypedValue getTypedValue(Criteria criteria, String propertyPath, Object value) throws HibernateException;

	/**
	 * Get the entity name of an entity
	 *
	 * @param criteria The criteria
	 *
	 * @return The entity name
	 */
	public String getEntityName(Criteria criteria);
	
	/**
	 * Get the entity name of an entity, taking into account the qualifier of the property path
	 *
	 * @param criteria The criteria
	 * @param propertyPath The property path that (supposedly) references an entity
	 *
	 * @return The entity name
	 */
	public String getEntityName(Criteria criteria, String propertyPath);

	/**
	 * Get the root table alias of an entity
	 *
	 * @param criteria The criteria
	 *
	 * @return The SQL table alias for the given criteria
	 */
	public String getSQLAlias(Criteria criteria);

	/**
	 * Get the root table alias of an entity, taking into account
	 * the qualifier of the property path
	 *
	 * @param criteria The criteria
	 * @param propertyPath The property path whose SQL alias should be returned.
	 *
	 * @return The SQL table alias for the given criteria
	 */
	public String getSQLAlias(Criteria criteria, String propertyPath);
	
	/**
	 * Get the property name, given a possibly qualified property name
	 *
	 * @param propertyName The (possibly qualified) property name
	 *
	 * @return The simple property name
	 */
	public String getPropertyName(String propertyName);
	
	/**
	 * Get the identifier column names of this entity
	 *
	 * @param criteria The criteria
	 *
	 * @return The identifier column names
	 */
	public String[] getIdentifierColumns(Criteria criteria);
	
	/**
	 * Get the identifier type of this entity
	 *
	 * @param criteria The criteria
	 *
	 * @return The identifier type.
	 */
	public Type getIdentifierType(Criteria criteria);

	/**
	 * Build a TypedValue for the given identifier value.
	 *
	 * @param criteria The criteria whose identifier is referenced.
	 * @param value The identifier value
	 *
	 * @return The TypedValue
	 */
	public TypedValue getTypedIdentifierValue(Criteria criteria, Object value);

	/**
	 * Generate a unique SQL alias
	 *
	 * @return The generated alias
	 */
	public String generateSQLAlias();
}
