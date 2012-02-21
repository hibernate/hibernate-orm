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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Convenience base class for implementing the {@link ValueContainer} contract centralizing commonality
 * between modeling tables, views and inline views.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableSpecification implements TableSpecification {
	private final static AtomicInteger tableCounter = new AtomicInteger( 0 );
	private final int tableNumber;

	private final List<Value> valueList = new ArrayList<Value>();
	private final LinkedHashMap<String, Value> valueMap = new LinkedHashMap<String, Value>();

	private final PrimaryKey primaryKey = new PrimaryKey( this );
	private final List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();

	public AbstractTableSpecification() {
		this.tableNumber = tableCounter.getAndIncrement();
	}

	@Override
	public int getTableNumber() {
		return tableNumber;
	}

	@Override
	public List<Value> values() {
		return Collections.unmodifiableList( valueList );
	}

	@Override
	public Column locateOrCreateColumn(String name) {
		if ( valueMap.containsKey( name ) ) {
			return (Column) valueMap.get( name );
		}
		return createColumn( name );
	}

	@Override
	public Column locateColumn(String name) {
		if ( valueMap.containsKey( name ) ) {
			return (Column) valueMap.get( name );
		}
		return null;
	}

	@Override
	public Column createColumn(String name) {
		final Column column = new Column( this, valueList.size(), name );
		valueMap.put( name, column );
		valueList.add( column );
		return column;
	}

	@Override
	public DerivedValue locateOrCreateDerivedValue(String fragment) {
		if ( valueMap.containsKey( fragment ) ) {
			return (DerivedValue) valueMap.get( fragment );
		}
		final DerivedValue value = new DerivedValue( this, valueList.size(), fragment );
		valueMap.put( fragment, value );
		valueList.add( value );
		return value;
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

	@Override
	public ForeignKey locateForeignKey(String name) {
		for ( ForeignKey fk : foreignKeys ) {
			if ( fk.getName().equals( name ) ) {
				return fk;
			}
		}
		return null;
	}

	@Override
	public Iterable<ForeignKey> locateForeignKey(TableSpecification targetTable) {
		List<ForeignKey> result = null;
		for ( ForeignKey fk : foreignKeys ) {
			if ( fk.getTargetTable().equals( targetTable ) ) {
				if ( result == null ) {
					result = new ArrayList<ForeignKey>();
				}
				result.add( fk );
			}
		}
		return result;
	}


	@Override
	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}
}
