/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.relational;

import java.sql.Types;

import org.hibernate.testing.junit.UnitTestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class TableManipulationTests extends UnitTestCase {
	public static final Datatype VARCHAR = new Datatype( Types.VARCHAR, "VARCHAR", String.class );
	public static final Datatype INTEGER = new Datatype( Types.INTEGER, "INTEGER", Long.class );

	public TableManipulationTests(String string) {
		super( string );
	}

	public void testTableCreation() {
		Table table = new Table( new ObjectName( null, null, "my_table" ) );
		assertNull( table.getObjectName().getSchema() );
		assertNull( table.getObjectName().getCatalog() );
		assertEquals( "my_table", table.getObjectName().getName().toString() );
		assertEquals( "my_table", table.getExportIdentifier() );
		assertNull( table.getPrimaryKey().getName() );
		assertFalse( table.values().iterator().hasNext() );

		Column idColumn = table.createColumn( "id" );
		idColumn.setDatatype( INTEGER );
		idColumn.setSize( Column.Size.precision( 18, 0 ) );
		table.getPrimaryKey().addColumn( idColumn );
		table.getPrimaryKey().setName( "my_table_pk" );
		assertEquals( "my_table_pk", table.getPrimaryKey().getName() );
		assertEquals( "my_table.PK", table.getPrimaryKey().getExportIdentifier() );

		Column col_1 = table.createColumn( "col_1" );
		col_1.setDatatype( VARCHAR );
		col_1.setSize( Column.Size.length( 512 ) );

		for ( Value value : table.values() ) {
			assertTrue( Column.class.isInstance( value ) );
			Column column = ( Column ) value;
			if ( column.getName().equals( "id" ) ) {
				assertEquals( INTEGER, column.getDatatype() );
				assertEquals( 18, column.getSize().getPrecision() );
				assertEquals( 0, column.getSize().getScale() );
				assertEquals( -1, column.getSize().getLength() );
				assertNull( column.getSize().getLobMultiplier() );
			}
			else {
				assertEquals( "col_1", column.getName() );
				assertEquals( VARCHAR, column.getDatatype() );
				assertEquals( -1, column.getSize().getPrecision() );
				assertEquals( -1, column.getSize().getScale() );
				assertEquals( 512, column.getSize().getLength() );
				assertNull( column.getSize().getLobMultiplier() );
			}
		}
	}

	public void testBasicForeignKeyDefinition() {
		Table book = new Table( new ObjectName( null, null, "BOOK" ) );

		Column bookId = book.createColumn( "id" );
		bookId.setDatatype( INTEGER );
		bookId.setSize( Column.Size.precision( 18, 0 ) );
		book.getPrimaryKey().addColumn( bookId );
		book.getPrimaryKey().setName( "BOOK_PK" );

		Table page = new Table( new ObjectName( null, null, "PAGE" ) );

		Column pageId = page.createColumn( "id" );
		pageId.setDatatype( INTEGER );
		pageId.setSize( Column.Size.precision( 18, 0 ) );
		page.getPrimaryKey().addColumn( pageId );
		page.getPrimaryKey().setName( "PAGE_PK" );

		Column pageBookId = page.createColumn( "BOOK_ID" );
		pageId.setDatatype( INTEGER );
		pageId.setSize( Column.Size.precision( 18, 0 ) );
		ForeignKey pageBookFk = page.createForeignKey( book, "PAGE_BOOK_FK" );
		pageBookFk.addColumn( pageBookId );

		assertEquals( page, pageBookFk.getSourceTable() );
		assertEquals( book, pageBookFk.getTargetTable() );
	}
}
