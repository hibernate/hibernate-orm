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

import java.util.Iterator;

import org.junit.Test;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public abstract class AbstractJPAIndexTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testTableIndex() {
		if ( isMetadataUsed() ) {
			TableSpecification table = SchemaUtil.getTable( Car.class, metadata() );
			Iterator<org.hibernate.metamodel.spi.relational.UniqueKey> uniqueKeys = table.getUniqueKeys().iterator();
			assertTrue( uniqueKeys.hasNext() );
			org.hibernate.metamodel.spi.relational.UniqueKey uk = uniqueKeys.next();
			assertFalse( uniqueKeys.hasNext() );
			assertTrue( StringHelper.isNotEmpty( uk.getName() ) );
			assertEquals( 2, uk.getColumnSpan() );
			org.hibernate.metamodel.spi.relational.Column column =  uk.getColumns().get( 0 );
			assertEquals( "brand", column.getColumnName().getText() );
			column = uk.getColumns().get( 1 );
			assertEquals( "producer", column.getColumnName().getText() );
			assertSame( table, uk.getTable() );


			Iterator<org.hibernate.metamodel.spi.relational.Index> indexes = table.getIndexes().iterator();
			assertTrue( indexes.hasNext() );
			org.hibernate.metamodel.spi.relational.Index index = indexes.next();
			assertFalse( indexes.hasNext() );
			assertEquals( "Car_idx", index.getName() );
			assertEquals( 1, index.getColumnSpan() );
			column = index.getColumns().get( 0 );
			assertEquals( "since", column.getColumnName().getText() );
			assertSame( table, index.getTable() );
		}
		else {
			PersistentClass entity = configuration().getClassMapping( Car.class.getName() );
			Iterator itr = entity.getTable().getUniqueKeyIterator();
			assertTrue( itr.hasNext() );
			UniqueKey uk = (UniqueKey) itr.next();
			assertFalse( itr.hasNext() );
			assertTrue( StringHelper.isNotEmpty( uk.getName() ) );
			assertEquals( 2, uk.getColumnSpan() );
			Column column = (Column) uk.getColumns().get( 0 );
			assertEquals( "brand", column.getName() );
			column = (Column) uk.getColumns().get( 1 );
			assertEquals( "producer", column.getName() );
			assertSame( entity.getTable(), uk.getTable() );


			itr = entity.getTable().getIndexIterator();
			assertTrue( itr.hasNext() );
			Index index = (Index)itr.next();
			assertFalse( itr.hasNext() );
			assertEquals( "Car_idx", index.getName() );
			assertEquals( 1, index.getColumnSpan() );
			column = index.getColumnIterator().next();
			assertEquals( "since", column.getName() );
			assertSame( entity.getTable(), index.getTable() );
		}
	}

	@Test
	public void testSecondaryTableIndex(){
		if ( isMetadataUsed() ) {
			EntityBinding entity = metadata().getEntityBinding( Car.class.getName() );

			TableSpecification table = entity.locateTable( "T_DEALER" );
			Iterator<org.hibernate.metamodel.spi.relational.Index> indexes = table.getIndexes().iterator();
			assertTrue( indexes.hasNext() );
			org.hibernate.metamodel.spi.relational.Index index = indexes.next();
			assertFalse( indexes.hasNext() );
			assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
			assertEquals( 2, index.getColumnSpan() );
			org.hibernate.metamodel.spi.relational.Column column = index.getColumns().get( 0 );
			assertEquals( "dealer_name", column.getColumnName().getText() );
			column = index.getColumns().get( 1 );
			assertEquals( "rate", column.getColumnName().getText() );
			assertSame( table, index.getTable() );
		}
		else {
			PersistentClass entity = configuration().getClassMapping( Car.class.getName() );

			Join join = (Join)entity.getJoinIterator().next();
			Iterator<Index> itr = join.getTable().getIndexIterator();
			assertTrue( itr.hasNext() );
			Index index = itr.next();
			assertFalse( itr.hasNext() );
			assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
			assertEquals( 2, index.getColumnSpan() );
			Iterator<Column> columnIterator = index.getColumnIterator();
			Column column = columnIterator.next();
			assertEquals( "dealer_name", column.getName() );
			column = columnIterator.next();
			assertEquals( "rate", column.getName() );
			assertSame( join.getTable(), index.getTable() );
		}
	}

	@Test
	public void testCollectionTableIndex(){
		if ( isMetadataUsed() ) {
			TableSpecification table = SchemaUtil.getCollectionTable( Car.class, "otherDealers", metadata() );

			Iterator<org.hibernate.metamodel.spi.relational.Index> indexes = table.getIndexes().iterator();
			assertTrue( indexes.hasNext() );
			org.hibernate.metamodel.spi.relational.Index index = indexes.next();
			assertFalse( indexes.hasNext() );
			assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
			assertEquals( 1, index.getColumnSpan() );
			org.hibernate.metamodel.spi.relational.Column column = index.getColumns().get( 0 );
			assertEquals( "name", column.getColumnName().getText() );
			assertSame( table, index.getTable() );
		}
		else {
			PersistentClass entity = configuration().getClassMapping( Car.class.getName() );
			Property property = entity.getProperty( "otherDealers" );
			Set set = (Set)property.getValue();
			Table collectionTable = set.getCollectionTable();

			Iterator<Index> itr = collectionTable.getIndexIterator();
			assertTrue( itr.hasNext() );
			Index index = itr.next();
			assertFalse( itr.hasNext() );
			assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
			assertEquals( 1, index.getColumnSpan() );
			Iterator<Column> columnIterator = index.getColumnIterator();
			Column column = columnIterator.next();
			assertEquals( "name", column.getName() );
			assertSame( collectionTable, index.getTable() );
		}
	}

	@Test
	public void testJoinTableIndex(){
		if ( isMetadataUsed() ) {
			TableSpecification table = SchemaUtil.getCollectionTable( Importer.class, "cars", metadata() );

			Iterator<org.hibernate.metamodel.spi.relational.Index> indexes = table.getIndexes().iterator();
			assertTrue( indexes.hasNext() );
			org.hibernate.metamodel.spi.relational.Index index = indexes.next();
			assertFalse( indexes.hasNext() );
			assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
			assertEquals( 1, index.getColumnSpan() );
			org.hibernate.metamodel.spi.relational.Column column = index.getColumns().get( 0 );
			assertEquals( "importers_id", column.getColumnName().getText() );
			assertSame( table, index.getTable() );
		}
		else {
			PersistentClass entity = configuration().getClassMapping( Importer.class.getName() );
			Property property = entity.getProperty( "cars" );
			Bag set = (Bag)property.getValue();
			Table collectionTable = set.getCollectionTable();

			Iterator<Index> itr = collectionTable.getIndexIterator();
			assertTrue( itr.hasNext() );
			Index index = itr.next();
			assertFalse( itr.hasNext() );
			assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
			assertEquals( 1, index.getColumnSpan() );
			Iterator<Column> columnIterator = index.getColumnIterator();
			Column column = columnIterator.next();
			assertEquals( "importers_id", column.getName() );
			assertSame( collectionTable, index.getTable() );
		}
	}

	@Test
	public void testTableGeneratorIndex(){
		//todo
	}
}
