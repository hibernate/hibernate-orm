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
		Table table = schema.createTable( Identifier.toIdentifier( "my_table" ), true );
		assertNull( table.getSchema().getName().getSchema() );
		assertNull( table.getSchema().getName().getCatalog() );
		assertEquals( "my_table", table.getTableName().toString() );
		assertEquals( "my_table", table.getExportIdentifier() );
		assertNull( table.getPrimaryKey().getName() );
		assertFalse( table.values().iterator().hasNext() );

		Column idColumn = table.locateOrCreateColumn( "id" );
		idColumn.setJdbcDataType( INTEGER );
		idColumn.setSize( Size.precision( 18, 0 ) );
		table.getPrimaryKey().addColumn( idColumn );
		table.getPrimaryKey().setName( "my_table_pk" );
		assertEquals( "my_table_pk", table.getPrimaryKey().getName() );
		assertEquals( "my_table.PK", table.getPrimaryKey().getExportIdentifier() );

		Column col_1 = table.locateOrCreateColumn( "col_1" );
		col_1.setJdbcDataType( VARCHAR );
		col_1.setSize( Size.length( 512 ) );

		for ( Value value : table.values() ) {
			assertTrue( Column.class.isInstance( value ) );
			Column column = ( Column ) value;
			if ( column.getColumnName().getName().equals( "id" ) ) {
				assertEquals( INTEGER, column.getJdbcDataType() );
				assertEquals( 18, column.getSize().getPrecision() );
				assertEquals( 0, column.getSize().getScale() );
				assertEquals( -1, column.getSize().getLength() );
				assertNull( column.getSize().getLobMultiplier() );
			}
			else {
				assertEquals( "col_1", column.getColumnName().getName() );
				assertEquals( VARCHAR, column.getJdbcDataType() );
				assertEquals( -1, column.getSize().getPrecision() );
				assertEquals( -1, column.getSize().getScale() );
				assertEquals( 512, column.getSize().getLength() );
				assertNull( column.getSize().getLobMultiplier() );
			}
		}
	}

	@Test
	public void testTableSpecificationCounter() {
		Schema schema = new Schema( null, null );
		Table table = schema.createTable( Identifier.toIdentifier( "my_table" ), true );
		InLineView inLineView = schema.createInLineView( "my_inlineview", "subselect" );
		InLineView otherInLineView = schema.createInLineView( "my_other_inlineview", "other subselect" );
		Table otherTable = schema.createTable( Identifier.toIdentifier( "my_other_table" ), true );

		int firstTableNumber = table.getTableNumber();
		assertEquals( firstTableNumber, table.getTableNumber() );
		assertEquals( firstTableNumber + 1, inLineView.getTableNumber() );
		assertEquals( firstTableNumber + 2, otherInLineView.getTableNumber() );
		assertEquals( firstTableNumber + 3, otherTable.getTableNumber() );
	}

	@Test
	public void testBasicForeignKeyDefinition() {
		Schema schema = new Schema( null, null );
		Table book = schema.createTable( Identifier.toIdentifier( "BOOK" ), true );

		Column bookId = book.locateOrCreateColumn( "id" );
		bookId.setJdbcDataType( INTEGER );
		bookId.setSize( Size.precision( 18, 0 ) );
		book.getPrimaryKey().addColumn( bookId );
		book.getPrimaryKey().setName( "BOOK_PK" );

		Table page = schema.createTable( Identifier.toIdentifier( "PAGE" ), true );

		Column pageId = page.locateOrCreateColumn( "id" );
		pageId.setJdbcDataType( INTEGER );
		pageId.setSize( Size.precision( 18, 0 ) );
		page.getPrimaryKey().addColumn( pageId );
		page.getPrimaryKey().setName( "PAGE_PK" );

		Column pageBookId = page.locateOrCreateColumn( "BOOK_ID" );
		pageId.setJdbcDataType( INTEGER );
		pageId.setSize( Size.precision( 18, 0 ) );
		ForeignKey pageBookFk = page.createForeignKey( book, "PAGE_BOOK_FK" );
		pageBookFk.addColumn( pageBookId );

		assertEquals( page, pageBookFk.getSourceTable() );
		assertEquals( book, pageBookFk.getTargetTable() );
	}

	@Test
	public void testQualifiedName() {
		Dialect dialect = new H2Dialect();
		Schema schema = new Schema( Identifier.toIdentifier( "schema" ), Identifier.toIdentifier( "`catalog`" ) );
		Table table = schema.createTable( Identifier.toIdentifier( "my_table" ), true );
		assertEquals( "my_table", table.getTableName().getName() );
		assertEquals( "my_table", table.getTableName().toString() );
		assertEquals( "schema.\"catalog\".my_table", table.getQualifiedName( dialect ) );

		table = schema.createTable( Identifier.toIdentifier( "`my_table`" ), true );
		assertEquals( "my_table", table.getTableName().getName() );
		assertEquals( "`my_table`", table.getTableName().toString() );
		assertEquals( "schema.\"catalog\".\"my_table\"", table.getQualifiedName( dialect ) );

		InLineView inLineView = schema.createInLineView( "my_inlineview", "select ..." );
		assertEquals( "( select ... )", inLineView.getQualifiedName( dialect ) );
	}
	
	@Test
	public void testTableLogicalName() {
		Schema schema = new Schema( Identifier.toIdentifier( "schema" ), Identifier.toIdentifier( "`catalog`" ) );
		Table table = schema.createTable( Identifier.toIdentifier( "my_table" ), false );
		assertEquals( "my_table", table.getLogicalName() );
		assertNull( table.getTableName() );
		assertSame(  table, schema.locateTable( table.getLogicalName() ) );
		table.setPhysicalName( Identifier.toIdentifier( "my_new_table" ) );
		assertEquals( "my_new_table", table.getLogicalName() );
		assertEquals( "my_new_table", table.getTableName().getName() );
		assertSame( table, schema.locateTable( "my_new_table" ) );
		assertNull( schema.locateTable( "my_table" ) );
		table.setPhysicalName( Identifier.toIdentifier( "my_newer_table" ) );
		assertEquals( "my_newer_table", table.getLogicalName() );
		assertEquals( "my_newer_table", table.getTableName().getName() );
		assertSame( table, schema.locateTable( "my_newer_table" ) );
		assertNull( schema.locateTable( "my_new_table" ) );
	}

	@Test
	public void testQuotedTableLogicalName() {
		Schema schema = new Schema( Identifier.toIdentifier( "schema" ), Identifier.toIdentifier( "`catalog`" ) );
		Table table = schema.createTable( Identifier.toIdentifier( "`my_table`" ), false );
		assertEquals( "`my_table`", table.getLogicalName() );
		assertNull( table.getTableName() );
		assertSame( table, schema.locateTable( table.getLogicalName() ) );
		table.setPhysicalName( Identifier.toIdentifier( "`my_new_table`" ) );
		assertEquals( "`my_new_table`", table.getLogicalName() );
		assertEquals( "my_new_table", table.getTableName().getName() );
		assertSame( table, schema.locateTable( "`my_new_table`" ) );
		assertNull( schema.locateTable( "`my_table`" ) );
		table.setPhysicalName( Identifier.toIdentifier( "`my_newer_table`" ) );
		assertEquals( "`my_newer_table`", table.getLogicalName() );
		assertEquals( "my_newer_table", table.getTableName().getName() );
		assertSame( table, schema.locateTable( "`my_newer_table`" ) );
		assertNull( schema.locateTable( "`my_new_table`" ) );
	}

	@Test
	public void testInLineViewLogicalName() {
		Schema schema = new Schema( Identifier.toIdentifier( "schema" ), Identifier.toIdentifier( "`catalog`" ) );
		InLineView view = schema.createInLineView( "my_view", "select" );
		assertEquals( "my_view", view.getLogicalName() );
		assertEquals( "select", view.getSelect() );
		assertSame(  view, schema.getInLineView( view.getLogicalName() ) );
	}

	@Test
	public void testLocateOrCreateTable() {
		Schema schema = new Schema( Identifier.toIdentifier( "schema" ), Identifier.toIdentifier( "`catalog`" ) );
		Table table = schema.locateOrCreateTable( "my_table", false );
		assertEquals( "my_table", table.getLogicalName() );
		assertNull( table.getTableName() );
		Table tableTemp = schema.locateOrCreateTable( "my_table", true );
		assertSame( table, tableTemp );
		assertEquals( "my_table", table.getLogicalName() );
		assertEquals( Identifier.toIdentifier( "my_table" ), table.getTableName() );
		tableTemp = schema.locateOrCreateTable( "my_table", false );
		assertSame( table, tableTemp );
		assertEquals( "my_table", table.getLogicalName() );
		assertEquals( Identifier.toIdentifier( "my_table" ), table.getTableName() );
	}
}
