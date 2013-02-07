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

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that there is no collision between the generated names for different types of constraints.
 * @author Gail Badner
 */
public class GeneratedNamesForDifferentConstraintTypes {
	private static final JdbcDataType INTEGER = new JdbcDataType( Types.INTEGER, "INTEGER", Long.class );
	protected static final String[] COLUMN_NAMES = new String[] { "col_1", "col_2", "col_3" };

	@Test
	public void testGeneratedNamesDoNotCollide() {
		Set<String> generatedNames = new HashSet<String>();

		// create a table for constraints
		TableSpecification tab1 = createTable( "my_table" );
		Column pkColTab1 = tab1.locateColumn( COLUMN_NAMES[0] );
		Column col1Tab1 = tab1.locateColumn( COLUMN_NAMES[1] );
		Column col2Tab1 = tab1.locateColumn( COLUMN_NAMES[2] );

		// create another table for constraints
		TableSpecification tab2 = createTable( "my_other_table" );
		Column pkColTab2 = tab2.locateColumn( COLUMN_NAMES[0] );
		Column col1Tab2 = tab2.locateColumn( COLUMN_NAMES[1] );
		Column col2Tab2 = tab2.locateColumn( COLUMN_NAMES[2] );

		// create a target table for foreign keys
		TableSpecification refTab1 = createTable( "my_referenced_table" );
		Column pkColTab1Ref = refTab1.locateColumn( COLUMN_NAMES[0] );
		Column col1Tab1Ref = refTab1.locateColumn( COLUMN_NAMES[1] );
		Column col2Tab1Ref = refTab1.locateColumn( COLUMN_NAMES[2] );

		// create another target table for foreign keys
		TableSpecification refTab2 = createTable( "other_referenced_table" );
		Column pkColTab2Ref = refTab2.locateColumn( COLUMN_NAMES[0] );
		Column col1Tab2Ref = refTab2.locateColumn( COLUMN_NAMES[1] );
		Column col2Tab2Ref = refTab2.locateColumn( COLUMN_NAMES[2] );


		// Add primary key columns to PK constraints and add generated name
		assertTrue( generatedNames.add( addColumnToPrimaryKey( tab1, pkColTab1 ).generateName() ) );
		assertTrue( generatedNames.add( addColumnToPrimaryKey( tab2, pkColTab2 ).generateName() ) );
		assertTrue( generatedNames.add( addColumnToPrimaryKey( refTab1, pkColTab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( addColumnToPrimaryKey( refTab2, pkColTab2Ref ).generateName() ) );

		// add constraints to tab1

		// add other types of constraints to the PK column for tab1
		assertTrue( generatedNames.add( createUniqueKey( tab1, pkColTab1 ).generateName() ) );
		assertTrue( generatedNames.add( createIndex( tab1, pkColTab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, pkColTab1, tab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, pkColTab1, tab1, col1Tab1 ).generateName() ) );

		// add constraints to 1st non-PK column for tab1 (col1Tab1)
		assertTrue( generatedNames.add( createUniqueKey( tab1, col1Tab1 ).generateName() ) );
		assertTrue( generatedNames.add( createIndex( tab1, col1Tab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col1Tab1, tab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col1Tab1, tab1, col2Tab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col1Tab1, refTab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col1Tab1, refTab1, col1Tab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col1Tab1, refTab1, col2Tab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col1Tab1, refTab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col1Tab1, refTab2, col1Tab2Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col1Tab1, refTab2, col2Tab2Ref ).generateName() ) );

		// add constraints to 2nd non-PK column for tab1 (col2Tab1)
		assertTrue( generatedNames.add( createUniqueKey( tab1, col2Tab1 ).generateName() ) );
		assertTrue( generatedNames.add( createIndex( tab1, col2Tab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col2Tab1, tab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col2Tab1, tab1, col1Tab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col2Tab1, refTab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col2Tab1, refTab1, col1Tab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col2Tab1, refTab1, col2Tab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col2Tab1, refTab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col2Tab1, refTab2, col1Tab2Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, col2Tab1, refTab2, col2Tab2Ref ).generateName() ) );

		// add multi-column constraints to tab1
		List<Column> colsTab1 = new ArrayList<Column>( );
		colsTab1.add( col1Tab1 );
		colsTab1.add( col2Tab1 );

		List<Column> colsTab1Reversed = new ArrayList<Column>( );
		colsTab1Reversed.add( col2Tab1 );
		colsTab1Reversed.add( col1Tab1 );

		List<Column> colsTab1Ref = new ArrayList<Column>( );
		colsTab1Ref.add( col1Tab1Ref );
		colsTab1Ref.add( col2Tab1Ref );

		List<Column> colsTab1RefReversed = new ArrayList<Column>( );
		colsTab1RefReversed.add( col2Tab1Ref );
		colsTab1RefReversed.add( col1Tab1Ref );

		assertTrue( generatedNames.add( createUniqueKey( tab1, colsTab1 ).generateName() ) );
		assertTrue( generatedNames.add( createIndex( tab1, colsTab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, colsTab1, refTab1, colsTab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, colsTab1, refTab1, colsTab1RefReversed ).generateName() ) );

		assertTrue( generatedNames.add( createUniqueKey( tab1, colsTab1Reversed ).generateName() ) );
		assertTrue( generatedNames.add( createIndex( tab1, colsTab1Reversed ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, colsTab1Reversed, refTab1, colsTab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab1, colsTab1Reversed, refTab1, colsTab1RefReversed ).generateName() ) );

		// add constraints to tab2

		// add other types of constraints to the PK column for tab2
		assertTrue( generatedNames.add( createUniqueKey( tab2, pkColTab2 ).generateName() ) );
		assertTrue( generatedNames.add( createIndex( tab2, pkColTab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, pkColTab2, tab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, pkColTab2, tab2, col1Tab2 ).generateName() ) );

		// add constraints to 1st non-PK column for tab2 (col1Tab2)
		assertTrue( generatedNames.add( createUniqueKey( tab2, col1Tab2 ).generateName() ) );
		assertTrue( generatedNames.add( createIndex( tab2, col1Tab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col1Tab2, tab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col1Tab2, tab2, col2Tab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col1Tab2, refTab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col1Tab2, refTab1, col1Tab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col1Tab2, refTab1, col2Tab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col1Tab2, refTab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col1Tab2, refTab2, col1Tab2Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col1Tab2, refTab2, col2Tab2Ref ).generateName() ) );

		// add constraints to 2nd non-PK column (col2Tab1) for tab1
		assertTrue( generatedNames.add( createUniqueKey( tab2, col2Tab2 ).generateName() ) );
		assertTrue( generatedNames.add( createIndex( tab2, col2Tab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col2Tab2, tab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col2Tab2, tab2, col1Tab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col2Tab2, refTab1 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col2Tab2, refTab1, col1Tab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col2Tab2, refTab1, col2Tab1Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col2Tab2, refTab2 ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col2Tab2, refTab2, col1Tab2Ref ).generateName() ) );
		assertTrue( generatedNames.add( createForeignKey( tab2, col2Tab2, refTab2, col2Tab2Ref ).generateName() ) );
	}

	@Test
	public void testForeignKeySameColumnMappingGeneratedName() {
		TableSpecification table = createTable( "my_table" );
		Column fkColumn = table.locateColumn( COLUMN_NAMES[0] );

		// create a target table for foreign keys
		TableSpecification referencedTable = createTable( "my_referenced_table" );
		Column referencedPKColumn = referencedTable.locateColumn( COLUMN_NAMES[0] );
		referencedTable.getPrimaryKey().addColumn( referencedPKColumn );

		ForeignKey fkImplicitFKMapping = createForeignKey( table, fkColumn, referencedTable );
		ForeignKey fkExplicitFKMappingToPK = createForeignKey(
				table,
				fkColumn,
				referencedTable,
				referencedPKColumn
		);
		assertEquals( fkImplicitFKMapping.generateName(), fkExplicitFKMappingToPK.generateName() );
	}

	private PrimaryKey addColumnToPrimaryKey(TableSpecification tableSpecification, Column column) {
		PrimaryKey primaryKey = tableSpecification.getPrimaryKey();
		primaryKey.addColumn( column );
		return primaryKey;
	}

	private ForeignKey createForeignKey(TableSpecification sourceTable, Column column, TableSpecification referencedTable ) {
		ForeignKey foreignKey = sourceTable.createForeignKey( referencedTable, null );
		foreignKey.addColumn( column );
		return foreignKey;
	}

	private ForeignKey createForeignKey(TableSpecification sourceTable, Column column, TableSpecification referencedTable, Column referencedColumn ) {
		ForeignKey foreignKey = sourceTable.createForeignKey( referencedTable, null );
		foreignKey.addColumnMapping( column, referencedColumn );
		return foreignKey;
	}

	private ForeignKey createForeignKey(
			TableSpecification table,
			List<Column> columns,
			TableSpecification referencedTable,
			List<Column> referencedColumns) {
		ForeignKey foreignKey = table.createForeignKey( referencedTable, null );
		for ( int i = 0 ; i < columns.size() ; i++ ) {
			foreignKey.addColumnMapping( columns.get( i ), referencedColumns.get( i ) );
		}
		return foreignKey;
	}

	private UniqueKey createUniqueKey(TableSpecification table, Column column) {
		UniqueKey uniqueKey = table.getOrCreateUniqueKey( null );
		uniqueKey.addColumn( column );
		return uniqueKey;
	}

	private UniqueKey createUniqueKey(TableSpecification table, List<Column> columns) {
		UniqueKey uniqueKey = table.getOrCreateUniqueKey( null );
		for ( Column column : columns ) {
			uniqueKey.addColumn( column );
		}
		return uniqueKey;
	}

	private Index createIndex(TableSpecification table, Column column) {
		Index index = table.getOrCreateIndex( null );
		index.addColumn( column );
		return index;
	}

	private Index createIndex(TableSpecification table, List<Column> columns) {
		Index index = table.getOrCreateIndex( null );
		for ( Column column : columns ) {
			index.addColumn( column );
		}
		return index;
	}

	protected TableSpecification createTable(String name) {
		Schema schema = new Schema( null, null );
		Table table = schema.createTable( Identifier.toIdentifier( name ), Identifier.toIdentifier( name ) );

		for ( String colName : COLUMN_NAMES ) {
			Column column = table.locateOrCreateColumn( colName );
			column.setJdbcDataType( INTEGER );
		}

		return table;
	}
}
