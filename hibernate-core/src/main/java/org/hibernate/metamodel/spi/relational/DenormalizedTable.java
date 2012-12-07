package org.hibernate.metamodel.spi.relational;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.internal.util.collections.JoinedIterable;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class DenormalizedTable extends Table {
	private final Table includedTable;

	public DenormalizedTable(Schema database, Identifier logicalName, Identifier physicalName, Table includedTable) {
		super( database, logicalName, physicalName );
		this.includedTable = includedTable;
	}

	@Override
	public List<Value> values() {
		List<Value> values = new ArrayList<Value>( super.values().size() + includedTable.values().size() );
		values.addAll( super.values() );
		values.addAll( includedTable.values() );
		return Collections.unmodifiableList( values );
	}

	@Override
	public Column locateColumn(String name) {
		Column column = includedTable.locateColumn( name );
		if(column!=null){
			return column;
		}
		return super.locateColumn( name );
	}

	@Override
	public boolean hasValue(Value value) {
		return includedTable.hasValue( value ) || super.hasValue( value );
	}

	@Override
	protected DerivedValue locateDerivedValue(String fragment) {
		DerivedValue value = includedTable.locateDerivedValue( fragment );
		return value != null ? value : super.locateDerivedValue( fragment );
	}

	@Override
	public Iterable<ForeignKey> getForeignKeys() {
		return new JoinedIterable<ForeignKey>(
				Arrays.asList(
						includedTable.getForeignKeys(),
						super.getForeignKeys()
				)
		);
	}

	@Override
	public Iterable<ForeignKey> locateForeignKey(TableSpecification targetTable) {
		Iterable<ForeignKey> fks = includedTable.locateForeignKey( targetTable );
		return fks != null ? fks : super.locateForeignKey( targetTable );
	}

	@Override
	protected <T extends Constraint> T locateConstraint(Iterable<T> constraints, String name) {
		T t = includedTable.locateConstraint( constraints, name );
		return t != null ? t : super.locateConstraint( constraints, name );
	}

	@Override
	public PrimaryKey getPrimaryKey() {
		return includedTable.getPrimaryKey();
	}
}
