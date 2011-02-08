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


/**
 * Models a value within a {@link ValueContainer}.  This will generally be either a {@link Column column} or a
 * {@link DerivedValue derived value}, but we also allow the notion of {@link Tuple} at this level
 *
 * @author Steve Ebersole
 */
public interface Value {
	/**
	 * Retrieve the table that owns this value.
	 *
	 * @return The owning table.
	 */
	public ValueContainer getValueContainer();

	/**
	 * Obtain the string representation of this value usable in log statements.
	 *
	 * @return The loggable representation
	 */
	public String toLoggableString();
}
