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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Convenience base class for implementing the {@link ValueContainer} contract centralizing commonality
 * between modelling tables views and inline views.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableSpecification implements TableSpecification, ValueContainer {
	private final LinkedHashSet<SimpleValue> values = new LinkedHashSet<SimpleValue>();
	private PrimaryKey primaryKey = new PrimaryKey( this );
	private List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();

	@Override
	public Iterable<SimpleValue> values() {
		return values;
	}

	@Override
	public Column createColumn(String name) {
		final Column column = new Column( this, values.size(), name );
		values.add( column );
		return column;
	}

	@Override
	public DerivedValue createDerivedValue(String fragment) {
		final DerivedValue value = new DerivedValue( this, values.size(), fragment );
		values.add( value );
		return value;
	}

	@Override
	public Tuple createTuple(String name) {
		return new Tuple( this, name );
	}

	@Override
	public Iterable<ForeignKey> getForeignKeys() {
		return foreignKeys;
	}

	@Override
	public ForeignKey createForeignKey(TableSpecification targetTable, String name) {
		ForeignKey fk = new ForeignKey( this, targetTable, name );
		foreignKeys.add( fk );
		return fk;
	}

	/**
	 * {@inheritDoc}
	 */
	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}
}
