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
