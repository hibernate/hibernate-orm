/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class DenormalizedTable extends Table {
	private final Table includedTable;

	public DenormalizedTable(Schema database, Identifier logicalName, Identifier physicalName, Table includedTable) {
		super( database, logicalName, physicalName );
		this.includedTable = includedTable;
		this.includedTable.setHasDenormalizedTables( true );
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
	//todo other constraints other than fk
	//we have to copy all FKs defined in the parent table to this sub table, can this be doing only once? like using ValueHolder?
	@Override
	public Iterable<ForeignKey> getForeignKeys() {
		copyFKsFromParentTable();
		return super.getForeignKeys();
	}
	private Set<ForeignKey> alreadyCopiedNonNameParentFK = new HashSet<ForeignKey>(  );
	private void copyFKsFromParentTable() {
		Iterable<ForeignKey> fksInSuperTable = includedTable.getForeignKeys();
		final String fkNamePostfix = Integer.toHexString( getTableName().hashCode() );
		for ( ForeignKey fk : fksInSuperTable ) {

			Identifier name = fk.getName();
			if (name == null || name.getText() == null) {
				if(!alreadyCopiedNonNameParentFK.contains( fk )){
					copyFK( fk, name );
					alreadyCopiedNonNameParentFK.add( fk );
				}
			}
			else {
				Identifier fkName = name.applyPostfix( fkNamePostfix );
				ForeignKey copiedFK = super.locateForeignKey( fkName.toString() );
				if ( copiedFK == null ) {
					copyFK( fk, fkName );
				}
			}
		}
	}

	private void copyFK(ForeignKey fk, Identifier fkName) {
		ForeignKey copiedFK = createForeignKey( fk.getTargetTable(), fkName, fk.createConstraint() );
		copiedFK.setDeleteRule( fk.getDeleteRule() );
		copiedFK.setUpdateRule( fk.getUpdateRule() );
		Iterable<ForeignKey.ColumnMapping> columnMappings = fk.getColumnMappings();
		for ( ForeignKey.ColumnMapping cm : columnMappings ) {
			copiedFK.addColumnMapping( cm.getSourceColumn(), cm.getTargetColumn() );
		}
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
