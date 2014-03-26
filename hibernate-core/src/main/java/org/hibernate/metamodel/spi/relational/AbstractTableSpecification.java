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

/**
 * Convenience base class for implementing the {@link ValueContainer} contract centralizing commonality
 * between modeling tables, views and inline views.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class AbstractTableSpecification implements TableSpecification {

	/**
	 * A column and derived value can have the same text; this value class helps
	 * to disambiguate.
	 */
	private class ValueKey {
		private final Value.ValueType valueType;
		private final Identifier identifier;

		private ValueKey(Value.ValueType valueType, Identifier identifier) {
			this.valueType = valueType;
			this.identifier = identifier;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			ValueKey valueKey = (ValueKey) o;

			if ( identifier != null ? !identifier.equals( valueKey.identifier ) : valueKey.identifier != null ) {
				return false;
			}
			return valueType == valueKey.valueType;

		}

		@Override
		public int hashCode() {
			int result = valueType != null ? valueType.hashCode() : 0;
			result = 31 * result + ( identifier != null ? identifier.hashCode() : 0 );
			return result;
		}

		@Override
		public String toString() {
			return "ValueKey{valueType=" + valueType.name() +
					", identifier=" + identifier +
					'}';
		}
	}
	private int tableNumber;

	private final List<Value> valueList = new ArrayList<Value>();
	private final LinkedHashMap<ValueKey, Value> valueMap = new LinkedHashMap<ValueKey, Value>();

	private final PrimaryKey primaryKey = new PrimaryKey( this );
	private final List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();

	@Override
	public int getTableNumber() {
		return tableNumber;
	}
	
	public void setTableNumber( int tableNumber ) {
		// This must be done outside of Table, rather than statically, to ensure
		// deterministic alias names.  See HHH-2448.
		this.tableNumber = tableNumber;
	}

	@Override
	public List<Value> values() {
		return Collections.unmodifiableList( valueList );
	}

	@Override
	public Column locateOrCreateColumn(String name) {
		Column column = locateColumn( name );
		if(column == null){
			column = createColumn( name );
		}
		return column;
	}

	@Override
	public Column locateColumn(String name) {
		final Identifier identifier = Identifier.toIdentifier( name );
		final ValueKey valueKey = new ValueKey( Value.ValueType.COLUMN, identifier );
		if ( valueMap.containsKey( valueKey ) ) {
			Value value = valueMap.get( valueKey );
			return Column.class.isInstance( value ) ? Column.class.cast( value ) : null;
		}
		return null;
	}

	@Override
	public boolean hasValue(Value value) {
		return valueMap.containsValue( value );
	}

	@Override
	public Column createColumn(String name) {
		return createColumn( Identifier.toIdentifier( name ) );
	}

	@Override
	public Column createColumn(Identifier name) {
		final Column column = new Column( valueList.size(), name );
		valueMap.put( new ValueKey( column.getValueType(), name ), column );
		valueList.add( column );
		return column;
	}

	@Override
	public DerivedValue locateOrCreateDerivedValue(String fragment) {
		DerivedValue value = locateDerivedValue( fragment );
		return value != null ? value : createDerivedValue( fragment );
	}

	protected DerivedValue locateDerivedValue(String fragment) {
		final Identifier identifier = Identifier.toIdentifier( fragment );
		final ValueKey valueKey = new ValueKey( Value.ValueType.DERIVED_VALUE, identifier );
		if ( valueMap.containsKey( valueKey ) ) {
			Value value = valueMap.get( valueKey );
			if ( DerivedValue.class.isInstance( value ) ) {
				return DerivedValue.class.cast( value );
			}
		}
		return null;
	}

	protected DerivedValue createDerivedValue(String fragment) {
		final Identifier identifier = Identifier.toIdentifier( fragment );
		final DerivedValue value = new DerivedValue( valueList.size(), fragment );
		valueMap.put( new ValueKey( value.getValueType(), identifier ), value );
		valueList.add( value );
		return value;
	}

	@Override
	public Iterable<ForeignKey> getForeignKeys() {
		return foreignKeys;
	}

	@Override
	public ForeignKey createForeignKey(TableSpecification targetTable, String name, boolean createConstraint) {
		Identifier identifier = Identifier.toIdentifier( name );
		return createForeignKey( targetTable, identifier, createConstraint );
	}

	@Override
	public ForeignKey createForeignKey(TableSpecification targetTable, Identifier name, boolean createConstraint) {
		ForeignKey fk = new ForeignKey( this, targetTable, name, createConstraint );
		foreignKeys.add( fk );
		return fk;
	}

	@Override
	public ForeignKey locateForeignKey(String name) {
		return locateConstraint( foreignKeys, name );
	}

	protected <T extends Constraint> T locateConstraint(Iterable<T> constraints, String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "name must be non-null." );
		}
		Identifier identifier = Identifier.toIdentifier( name );
		for ( T constraint : constraints ) {
			if ( identifier.equals( constraint.getName() ) ) {
				return constraint;
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
	@Override
	public int columnListId(Iterable<Column> columns) {
		int result = getLogicalName().hashCode();
		for ( Column column : columns ) {
			sameTableCheck( column );
			result = 31 * result + column.getColumnName().hashCode();
		}
		return result;
	}

	private void sameTableCheck(Column column) {
		if ( ! hasValue( column ) ) {
			throw new IllegalArgumentException( "All columns must be from this table." );
		}
	}
}