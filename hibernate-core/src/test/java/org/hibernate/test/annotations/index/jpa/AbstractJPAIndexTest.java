/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013 by Red Hat Inc and/or its affiliates or by
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
package org.hibernate.test.annotations.index.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Index;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */

public abstract class AbstractJPAIndexTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testTableIndex() {
		TableSpecification table = SchemaUtil.getTable( Car.class, metadata() );
		Iterator<UniqueKey> uniqueKeys = table.getUniqueKeys().iterator();
		assertTrue( uniqueKeys.hasNext() );
		UniqueKey uk = (UniqueKey) uniqueKeys.next();
		assertFalse( uniqueKeys.hasNext() );
		assertTrue( StringHelper.isNotEmpty( uk.getName() ) );
		assertEquals( 2, uk.getColumnSpan() );
		Column column = (Column) uk.getColumns().get( 0 );
		assertEquals( "brand", column.getColumnName().getText() );
		column = (Column) uk.getColumns().get( 1 );
		assertEquals( "producer", column.getColumnName().getText() );
		assertSame( table, uk.getTable() );


		Iterator<Index> indexes = table.getIndexes().iterator();
		assertTrue( indexes.hasNext() );
		Index index = (Index)indexes.next();
		assertFalse( indexes.hasNext() );
		assertEquals( "Car_idx", index.getName() );
		assertEquals( 1, index.getColumnSpan() );
		column = index.getColumns().get( 0 );
		assertEquals( "since", column.getColumnName().getText() );
		assertSame( table, index.getTable() );
	}

	@Test
	public void testSecondaryTableIndex(){
		EntityBinding entity = SchemaUtil.getEntityBinding( Car.class, metadata() );

		TableSpecification table = entity.locateTable( "T_DEALER" );
		Iterator<Index> indexes = table.getIndexes().iterator();
		assertTrue( indexes.hasNext() );
		Index index = indexes.next();
		assertFalse( indexes.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
		assertEquals( 2, index.getColumnSpan() );
		Column column = index.getColumns().get( 0 );
		assertEquals( "dealer_name", column.getColumnName().getText() );
		column = index.getColumns().get( 1 );
		assertEquals( "rate", column.getColumnName().getText() );
		assertSame( table, index.getTable() );

	}

	@Test
	public void testCollectionTableIndex() {
		TableSpecification table = SchemaUtil.getCollectionTable( Car.class, "otherDealers", metadata() );

		Iterator<Index> indexes = table.getIndexes().iterator();
		assertTrue( indexes.hasNext() );
		Index index = indexes.next();
		assertFalse( indexes.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
		assertEquals( 1, index.getColumnSpan() );
		Column column = index.getColumns().get( 0 );
		assertEquals( "name", column.getColumnName().getText() );
		assertSame( table, index.getTable() );
	}

	@Test
	public void testJoinTableIndex(){
		TableSpecification table = SchemaUtil.getCollectionTable( Importer.class, "cars", metadata() );

		Iterator<Index> indexes = table.getIndexes().iterator();
		assertTrue( indexes.hasNext() );
		Index index = indexes.next();
		assertFalse( indexes.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
		assertEquals( 1, index.getColumnSpan() );
		Column column = index.getColumns().get( 0 );
		assertEquals( "importers_id", column.getColumnName().getText() );
		assertSame( table, index.getTable() );
	}
}
