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
	public SessionFactoryImplementor getFactory();
	
	/**
	 * Get the names of the columns mapped by a property path,
	 * ignoring projection aliases
	 * @throws org.hibernate.QueryException if the property maps to more than 1 column
	 */
	public String getColumn(Criteria criteria, String propertyPath) 
	throws HibernateException;
	
	/**
	 * Get the names of the columns mapped by a property path,
	 * ignoring projection aliases
	 */
	public String[] getColumns(String propertyPath, Criteria criteria)
	throws HibernateException;

	/**
	 * Get the names of the columns mapped by a property path; if the
	 * property path is not found in criteria, try the "outer" query.
	 * Projection aliases are ignored.
	 */
	public String[] findColumns(String propertyPath, Criteria criteria)
	throws HibernateException;

	/**
	 * Get the type of a property path, ignoring projection aliases
	 */
	public Type getType(Criteria criteria, String propertyPath)
	throws HibernateException;

	/**
	 * Get the names of the columns mapped by a property path
	 */
	public String[] getColumnsUsingProjection(Criteria criteria, String propertyPath) 
	throws HibernateException;

	/**
	 * Get the type of a property path
	 */
	public Type getTypeUsingProjection(Criteria criteria, String propertyPath)
	throws HibernateException;

	/**
	 * Get the a typed value for the given property value.
	 */
	public TypedValue getTypedValue(Criteria criteria, String propertyPath, Object value)
	throws HibernateException;
	
	/**
	 * Get the entity name of an entity
	 */
	public String getEntityName(Criteria criteria);
	
	/**
	 * Get the entity name of an entity, taking into account
	 * the qualifier of the property path
	 */
	public String getEntityName(Criteria criteria, String propertyPath);

	/**
	 * Get the root table alias of an entity
	 */
	public String getSQLAlias(Criteria subcriteria);

	/**
	 * Get the root table alias of an entity, taking into account
	 * the qualifier of the property path
	 */
	public String getSQLAlias(Criteria criteria, String propertyPath);
	
	/**
	 * Get the property name, given a possibly qualified property name
	 */
	public String getPropertyName(String propertyName);
	
	/**
	 * Get the identifier column names of this entity
	 */
	public String[] getIdentifierColumns(Criteria subcriteria);
	
	/**
	 * Get the identifier type of this entity
	 */
	public Type getIdentifierType(Criteria subcriteria);
	
	public TypedValue getTypedIdentifierValue(Criteria subcriteria, Object value);
	
	public String generateSQLAlias();
}