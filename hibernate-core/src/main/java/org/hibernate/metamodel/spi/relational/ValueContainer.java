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
package org.hibernate.metamodel.spi.relational;

import java.util.List;

/**
 * Contract for data containers (what the ANSI SQL spec calls "table specifications") to which we can map
 * entity state.  The two flavors here are {@link Table physical table} and {@link InLineView inline view}.
 *
 * @author Steve Ebersole
 */
public interface ValueContainer {
	/**
	 * Obtain an iterator over this containers current set of value definitions.
	 * <p/>
	 * The returned list is unmodifiable!
	 *
	 * @return Iterator over value definitions.
	 */
	public List<Value> values();

	public boolean hasValue(Value value);

	/**
	 * Get a qualifier which can be used to qualify {@link Value values} belonging to this container in
	 * their logging.
	 *
	 * @return The qualifier
	 */
	public String getLoggableValueQualifier();
}
