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
import java.util.LinkedHashSet;

/**
 * Convenience base class for implementing the {@link ValueContainer} contract
 *
 * @author Steve Ebersole
 */
public abstract class AbstractValueContainer implements ValueContainer {
	private final LinkedHashSet<Value> values = new LinkedHashSet<Value>();

	@Override
	public Iterable<Value> values() {
		return values;
	}

	@Override
	public Column createColumn(String name) {
		final Column column = new Column( this, name );
		values.add( column );
		return column;
	}

	@Override
	public DerivedValue createDerivedValue(String fragment) {
		final DerivedValue value = new DerivedValue( this, fragment );
		values.add( value );
		return value;
	}

	@Override
	public Tuple createTuple(String name) {
		final Tuple tuple = new Tuple( this, name );
		values.add( tuple );
		return tuple;
	}
}
