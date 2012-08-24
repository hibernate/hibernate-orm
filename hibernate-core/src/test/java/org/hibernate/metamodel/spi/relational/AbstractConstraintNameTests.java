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

import org.junit.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public abstract class AbstractConstraintNameTests extends BaseUnitTestCase {
	private static final JdbcDataType INTEGER = new JdbcDataType( Types.INTEGER, "INTEGER", Long.class );
	protected static final String[] COLUMN_NAMES = new String[] { "col_1", "col_2", "col_3" };
	private Dialect dialect = new H2Dialect();

	@Test
	public void testConstraintNameUninitializedInConstructor() {
		AbstractConstraint constraint = createConstraintAndAddColumns( "my_table", null, COLUMN_NAMES[0] );
		assertEquals( null, constraint.getName() );

		String generatedName = constraint.generateName();
		checkGeneratedName( constraint );

		// ensure the generated name doesn't change when constraint.generateName() is called again
		assertEquals( generatedName, constraint.generateName() );

		// since the constraint name is null, the DDL should contain the generated name;
		checkNameInDDL( constraint, generatedName );
	}

	@Test
	public void testConstraintNameInitializedInConstructor() {
		AbstractConstraint constraint = createConstraintAndAddColumns( "my_table", "AName", COLUMN_NAMES[0] );
		assertEquals( "AName", constraint.getName() );

		// since the constraint name is non-null, the DDL should contain the constraint name;
		checkNameInDDL( constraint, constraint.getName() );
	}

	@Test
	public void testConstraintNameUninitializedInConstructorThenSet() {
		AbstractConstraint constraint = createConstraintAndAddColumns( "my_table", null, COLUMN_NAMES[0] );
		assertEquals( null, constraint.getName() );

		constraint.setName( "AName" );
		assertEquals( "AName", constraint.getName() );

		// since the constraint name is non-null now, the DDL should contain the constraint name;
		checkNameInDDL( constraint, constraint.getName() );
	}

	@Test
	public void testResetConstraintNameToNull() {
		AbstractConstraint constraint = createConstraintAndAddColumns( "my_table", "AName", COLUMN_NAMES[0] );
		assertEquals( "AName", constraint.getName() );

		try {
			constraint.setName( null );
			fail( "should have thrown exception" );
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void testResetConstraintNameToNewValue() {
		AbstractConstraint constraint = createConstraintAndAddColumns( "my_table", "AName", COLUMN_NAMES[0] );
		assertEquals( "AName", constraint.getName() );

		try {
			constraint.setName( "ANewName" );
			fail( "should have thrown exception" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testConstraintsOnSameColumnInDiffTableHaveDiffGeneratedName() {
		AbstractConstraint constraint1 = createConstraintAndAddColumns( "my_table", null, COLUMN_NAMES[0] );
		AbstractConstraint constraint2 = createConstraintAndAddColumns( "my_other_table", null, COLUMN_NAMES[0] );
		// generated names should be different because the constraints are on different tables
		assertTrue( ! constraint1.generateName().equals( constraint2.generateName() ) );
	}

	@Test
	public void testConstraintNameOnDiffColumnInSameTableHaveDiffName() {
		AbstractConstraint constraint1 = createConstraintAndAddColumns( "my_table", null, COLUMN_NAMES[0] );
		AbstractConstraint constraint2 = createConstraintAndAddColumns( "my_table", null, COLUMN_NAMES[1] );
		// generated names should be different because the constraints are on different columns in the same table
		if ( constraint1 instanceof PrimaryKey && constraint2 instanceof PrimaryKey ) {
			// generated name for primary keys does not depend on column name; this is ok
			// because there is only 1 primary key can be defined on the table
			assertTrue( constraint1.generateName().equals( constraint2.generateName() ) );
		}
		else {
			// generated names for non-primary key constraints does depend on the column,
			// so the generated names should be different.
			assertTrue( ! constraint1.generateName().equals( constraint2.generateName() ) );
		}
	}

	protected abstract AbstractConstraint createConstraint(TableSpecification table, String constraintName);

	private AbstractConstraint createConstraintAndAddColumns(
			String tableName,
			String constraintName,
			String constraintColumn) {
		final Table table = createTable( tableName );
		AbstractConstraint constraint = createConstraint( table, constraintName );
		constraint.addColumn( table.locateColumn( constraintColumn ) );
		return constraint;
	}

	protected void checkGeneratedName(AbstractConstraint constraint) {
		assertTrue(
				constraint.generateName().startsWith(
						constraint.getGeneratedNamePrefix() +
								Integer.toHexString(
										constraint.getTable().getLogicalName().hashCode()
								).toUpperCase()
				)
		);
	}

	private void checkNameInDDL(AbstractConstraint constraint, String expectedNameInDDL) {
		// since the name is null, the generated name should be in the DDL
		String sqlConstraintStringInAlterTable = constraint.sqlConstraintStringInAlterTable( dialect );
		assertTrue( sqlConstraintStringInAlterTable.contains( expectedNameInDDL ));

		String[] sqlCreateStrings = constraint.sqlCreateStrings( dialect );
		assertEquals( 1, sqlCreateStrings.length );
		assertTrue( sqlCreateStrings[0].contains( sqlConstraintStringInAlterTable ) );

		String[] sqlDropStrings = constraint.sqlDropStrings( dialect );
		assertEquals( 1, sqlDropStrings.length );
		assertTrue( sqlDropStrings[0].contains( expectedNameInDDL ) );
	}

	protected Table createTable(String name) {
		Schema schema = new Schema( null, null );
		Table table = schema.createTable( Identifier.toIdentifier( name ), Identifier.toIdentifier( name ) );

		for ( String colName : COLUMN_NAMES ) {
			Column column = table.locateOrCreateColumn( colName );
			column.setJdbcDataType( INTEGER );
		}

		return table;
	}
}
