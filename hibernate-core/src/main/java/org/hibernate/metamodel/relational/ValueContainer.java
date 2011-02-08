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
 * Contract for data containers (what the ANSI SQL spec calls "table specifications") to which we can map
 * entity state.  The two flavors here are {@link Table physical table} and {@link InLineView inline view}, but a
 * {@link Tuple} is a conceptual value container as well.
 *
 * @author Steve Ebersole
 */
public interface ValueContainer {
	/**
	 * Obtain an iterator over this containers current set of value definitions.
	 *
	 * @return Iterator over value definitions.
	 */
	public Iterable<Value> values();

	/**
	 * Get a qualifier which can be used to qualify {@link Value values} belonging to this container in
	 * their logging.
	 *
	 * @return The qualifier
	 */
	public String getLoggableValueQualifier();

	/**
	 * Obtain the string representation of this value usable in log statements.
	 *
	 * @return The loggable representation
	 */
	public String toLoggableString();

	/**
	 * Factory method for creating a {@link Column} associated with this container.
	 *
	 * @param name The column name
	 *
	 * @return The generated column
	 */
	public Column createColumn(String name);

	/**
	 * Factory method for creating a {@link DerivedValue} associated with this container.
	 *
	 * @param fragment The value expression
	 *
	 * @return The generated value.
	 */
	public DerivedValue createDerivedValue(String fragment);

	/**
	 * Factory method for creating a {@link Tuple} associated with this container.
	 *
	 * @param name The (logical) tuple name
	 *
	 * @return The generated tuple.
	 */
	public Tuple createTuple(String name);
}
