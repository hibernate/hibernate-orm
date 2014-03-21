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
package org.hibernate.metamodel.spi.relational;

import java.sql.Types;

import org.junit.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class TableManipulationTests extends BaseUnitTestCase {
	public static final JdbcDataType VARCHAR = new JdbcDataType( Types.VARCHAR, "VARCHAR", String.class );
	public static final JdbcDataType INTEGER = new JdbcDataType( Types.INTEGER, "INTEGER", Long.class );

	@Test
	public void testTableCreation() {
		Schema schema = new Schema( null, null );
		Table table = schema.createTable( Identifier.toIdentifier( "my_table" ), Identifier.toIdentifier( "my_table" ) );
		assertNull( table.getSchema().getName().getSchema() );
		assertNull( table.getSchema().getName().getCatalog() );
		assertEquals( "my_table", table.getPhysicalName().toString() );
		assertEquals( "my_table", table.getExportIdentifier() );
		assertNull( table.getPrimaryKey().getName() );
		assertFalse( table.values().iterator().hasNext() );

		Column idColumn = table.locateOrCreateColumn( "id" );
		idColumn.setJdbcDataType( INTEGER );
		idColumn.setSize( Size.precision( 18, 0 ) );
		table.getPrimaryKey().addColumn( idColumn );
		table.getPrimaryKey().setName( Identifier.toIdentifier( "my_table_pk" ) );
		assertEquals( "my_table_pk", table.getPrimaryKey().getName().toString() );
		assertEquals( "my_table.PK", table.getPrimaryKey().getExportIdentifier() );

		Column col_1 = table.locateOrCreateColumn( "col_1" );
		col_1.setJdbcDataType( VARCHAR );
		col_1.setSize( Size.length( 512 ) );

		for ( Value value : table.values() ) {
			assertTrue( Column.class.isInstance( value ) );
			Column column = ( Column ) value;
			if ( column.getColumnName().getText().equals( "id" ) ) {
				assertEquals( INTEGER, column.getJdbcDataType() );
				assertEquals( 18, column.getSize().getPrecision() );
				assertEquals( 0, column.getSize().getScale() );
				assertEquals( -1, column.getSize().getLength() );
				assertNull( column.getSize().getLobMultiplier() );
			}
			else {
				assertEquals( "col_1", column.getColumnName().getText() );
				assertEquals( VARCHAR, column.getJdbcDataType() );
				assertEquals( -1, column.getSize().getPrecision() );
				assertEquals( -1, column.getSize().getScale() );
				assertEquals( 512, column.getSize().getLength() );
				assertNull( column.getSize().getLobMultiplier() );
			}
		}
	}

	@Test
	public void testBasicForeignKeyDefinition() {
		Schema schema = new Schema( null, null );
		Table book = schema.createTable( Identifier.toIdentifier( "BOOK" ), Identifier.toIdentifier( "BOOK" ) );

		Column bookId = book.locateOrCreateColumn( "id" );
		bookId.setJdbcDataType( INTEGER );
		bookId.setSize( Size.precision( 18, 0 ) );
		book.getPrimaryKey().addColumn( bookId );
		book.getPrimaryKey().setName( Identifier.toIdentifier( "BOOK_PK" ) );

		Table page = schema.createTable( Identifier.toIdentifier( "PAGE" ), Identifier.toIdentifier( "PAGE" ) );

		Column pageId = page.locateOrCreateColumn( "id" );
		pageId.setJdbcDataType( INTEGER );
		pageId.setSize( Size.precision( 18, 0 ) );
		page.getPrimaryKey().addColumn( pageId );
		page.getPrimaryKey().setName( Identifier.toIdentifier( "PAGE_PK" ) );

		Column pageBookId = page.locateOrCreateColumn( "BOOK_ID" );
		pageId.setJdbcDataType( INTEGER );
		pageId.setSize( Size.precision( 18, 0 ) );
		ForeignKey pageBookFk = page.createForeignKey( book, "PAGE_BOOK_FK", true );
		pageBookFk.addColumn( pageBookId );

		assertEquals( page, pageBookFk.getSourceTable() );
		assertEquals( book, pageBookFk.getTargetTable() );

		assertEquals( 1, pageBookFk.getColumnSpan() );
		assertEquals( 1, pageBookFk.getColumns().size() );
		assertEquals( 1, pageBookFk.getSourceColumns().size() );
		assertEquals( 1, pageBookFk.getTargetColumns().size() );
		assertSame( pageBookId, pageBookFk.getColumns().get( 0 ) );
		assertSame( pageBookId, pageBookFk.getSourceColumns().get( 0 ) );
		assertSame( bookId, pageBookFk.getTargetColumns().get( 0 ) );
	}

	@Test
	public void testQualifiedName() {
		Dialect dialect = new H2Dialect();
		Schema schema = new Schema( Identifier.toIdentifier( "`catalog`" ), Identifier.toIdentifier( "schema" ) );
		Table table = schema.createTable( Identifier.toIdentifier( "my_table" ), Identifier.toIdentifier( "my_table" ) );
		assertEquals( "my_table", table.getPhysicalName().getText() );
		assertEquals( "my_table", table.getPhysicalName().toString() );
		assertEquals( "\"catalog\".schema.my_table", table.getQualifiedName( dialect ) );

		table = schema.createTable( Identifier.toIdentifier( "`my_table`" ), Identifier.toIdentifier( "`my_table`" ) );
		assertEquals( "my_table", table.getPhysicalName().getText() );
		assertEquals( "`my_table`", table.getPhysicalName().toString() );
		assertEquals( "\"catalog\".schema.\"my_table\"", table.getQualifiedName( dialect ) );

		InLineView inLineView = schema.createInLineView( Identifier.toIdentifier( "my_inlineview" ), "select ..." );
		assertEquals( "( select ... )", inLineView.getQualifiedName( dialect ) );
	}

	@Test
	public void testTableIdentifier() {
		Identifier tableIdentifier = Identifier.toIdentifier( "my_table" );
		assertEquals( "my_table", tableIdentifier.getText() );
		Schema schema = new Schema( Identifier.toIdentifier( "`catalog`" ), Identifier.toIdentifier( "schema" ) );
		Table table = schema.createTable( tableIdentifier, tableIdentifier );
		assertSame( tableIdentifier, table.getPhysicalName() );
		assertSame( table, schema.locateTable( Identifier.toIdentifier( "my_table"  ) ) );
		assertEquals( "my_table", table.getLogicalName().getText() );
	}

	@Test
	public void testQuotedTableIdentifier() {
		Identifier tableIdentifier = Identifier.toIdentifier( "`my_table`" );
		assertEquals( "my_table", tableIdentifier.getText() );
		Schema schema = new Schema( Identifier.toIdentifier( "`catalog`" ), Identifier.toIdentifier( "schema" ) );
		Table table = schema.createTable( tableIdentifier, tableIdentifier );
		assertSame( tableIdentifier, table.getPhysicalName() );
		assertSame( table, schema.locateTable( Identifier.toIdentifier( "`my_table`" ) ) );
		assertEquals( "my_table", table.getLogicalName().getText() );
		assertTrue( table.getLogicalName().isQuoted() );
		assertNull( schema.locateTable( Identifier.toIdentifier( "my_table" ) ) );
	}

	@Test
	public void testInLineViewLogicalName() {
		Schema schema = new Schema( Identifier.toIdentifier( "`catalog`" ), Identifier.toIdentifier( "schema" ) );
		InLineView view = schema.createInLineView( Identifier.toIdentifier( "my_view" ), "select" );
		assertEquals( "my_view", view.getLogicalName().getText() );
		assertEquals( "select", view.getSelect() );
		assertSame(  view, schema.getInLineView( view.getLogicalName() ) );
	}
}
