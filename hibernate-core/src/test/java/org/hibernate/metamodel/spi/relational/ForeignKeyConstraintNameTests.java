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

import junit.framework.Assert;
import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Gail Badner
 */
public class ForeignKeyConstraintNameTests extends AbstractConstraintNameTests {
	@Override
	protected AbstractConstraint createConstraint(TableSpecification table, String constraintName) {
		// create referenced table with primary key; foreign key will reference
		// referencedTable's primary key
		TableSpecification referencedTable = createTable( "my_referenced_table" );
		referencedTable.getPrimaryKey().addColumn( referencedTable.locateColumn( COLUMN_NAMES[0] ) );
		return table.createForeignKey( referencedTable, constraintName );
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testGeneratedExportedIdentifier(){
		testExportedIdentifier( null, null, "ABs", "idA", "idB", "`As`", "id", "Bs", "id" );
	}

	@Test
	public void testNonGeneratedExportedIdentifier(){
		testExportedIdentifier( "name1", "name2", "ABs", "idA", "idB", "`As`", "id", "Bs", "id" );
	}

	private void testExportedIdentifier(
			String fkName1,
			String fkName2,
			String sourceTableName,
			String sourceColumnName1,
			String sourceColumnName2,
			String targetTableName1,
			String targetColumnName1,
			String targetTableName2,
			String targetColumnName2){

		TableSpecification targetTable1 = createTable( targetTableName1 );
		Column targetColumn1 = targetTable1.createColumn( targetColumnName1 );

		TableSpecification targetTable2 = createTable( targetTableName2 );
		Column targetColumn2 = targetTable2.createColumn( targetColumnName2 );

		TableSpecification sourceTable = createTable( sourceTableName );
		Column sourceColumn1 = sourceTable.createColumn( sourceColumnName1 );
		Column sourceColumn2 = sourceTable.createColumn( sourceColumnName2 );

		ForeignKey fk1 = sourceTable.createForeignKey( targetTable1, fkName1 );
		fk1.addColumnMapping( sourceColumn1, targetColumn1 );
		ForeignKey fk2 = sourceTable.createForeignKey( targetTable2, fkName2 );
		fk2.addColumnMapping( sourceColumn2, targetColumn2 );

		Assert.assertTrue( ! fk1.getExportIdentifier().equals(  fk2.getExportIdentifier() ) );
	}

}
