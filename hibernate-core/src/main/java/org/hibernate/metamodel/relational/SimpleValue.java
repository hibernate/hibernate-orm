/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.relational;

import org.hibernate.dialect.Dialect;

/**
 * Models a simple, non-compound value.
 *
 * @author Steve Ebersole
 */
public interface SimpleValue extends Value {
	/**
	 * Retrieve the datatype of this value.
	 *
	 * @return The value's datatype
	 */
	public Datatype getDatatype();

	/**
	 * Set the datatype of this value.
	 *
	 * @param datatype The value's datatype
	 */
	public void setDatatype(Datatype datatype);

	/**
	 * For any column name, generate an alias that is unique
	 * to that column name, unique across tables, and within
	 * alias size constraints determined by
	 * {@link org.hibernate.dialect.Dialect#getMaxAliasLength()}.
	 *
	 * @param dialect the dialect.
	 * @return the alias.
	 */
	public String getAlias(Dialect dialect);
}
