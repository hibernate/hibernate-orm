/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.index.jpa;

import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.*;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.List;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.mapping.Value;
import org.hibernate.test.annotations.embedded.Address;
import org.hibernate.test.annotations.embedded.AddressType;
import org.hibernate.test.annotations.embedded.Book;
import org.hibernate.test.annotations.embedded.Person;
import org.hibernate.test.annotations.embedded.Summary;
import org.hibernate.test.annotations.embedded.WealthyPerson;
import org.hibernate.test.event.collection.detached.*;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class IndexTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Car.class,
				Book.class,
				Summary.class,
				WealthyPerson.class,
				Person.class,
				AddressType.class,
				Address.class,
				Alias.class,
				org.hibernate.test.event.collection.detached.Character.class
		};
	}

	@Test
	public void testTableIndex() {
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

	@Test
	public void testSecondaryTableIndex(){
		PersistentClass entity = configuration().getClassMapping( Book.class.getName() );

		Join join = (Join)entity.getJoinIterator().next();
		Iterator<Index> itr = join.getTable().getIndexIterator();
		assertTrue( itr.hasNext() );
		Index index = itr.next();
		assertFalse( itr.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
		assertEquals( 2, index.getColumnSpan() );
		Iterator<Column> columnIterator = index.getColumnIterator();
		Column column = columnIterator.next();
		assertEquals( "summ_size", column.getName() );
		column = columnIterator.next();
		assertEquals( "text", column.getName() );
		assertSame( join.getTable(), index.getTable() );

	}

	@Test
	public void testCollectionTableIndex(){
		PersistentClass entity = configuration().getClassMapping( WealthyPerson.class.getName() );
		Property property = entity.getProperty( "explicitVacationHomes" );
		Set set = (Set)property.getValue();
		Table collectionTable = set.getCollectionTable();

		Iterator<Index> itr = collectionTable.getIndexIterator();
		assertTrue( itr.hasNext() );
		Index index = itr.next();
		assertFalse( itr.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
		assertEquals( 2, index.getColumnSpan() );
		Iterator<Column> columnIterator = index.getColumnIterator();
		Column column = columnIterator.next();
		assertEquals( "countryName", column.getName() );
		column = columnIterator.next();
		assertEquals( "type_id", column.getName() );
		assertSame( collectionTable, index.getTable() );

	}

	@Test
	public void testJoinTableIndex(){
		PersistentClass entity = configuration().getClassMapping( Alias.class.getName() );
		Property property = entity.getProperty( "characters" );
		Bag set = (Bag)property.getValue();
		Table collectionTable = set.getCollectionTable();

		Iterator<UniqueKey> itr = collectionTable.getUniqueKeyIterator();
		assertTrue( itr.hasNext() );
		UniqueKey index = itr.next();
		assertFalse( itr.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
		assertEquals( 1, index.getColumnSpan() );
		Iterator<Column> columnIterator = index.getColumnIterator();
		Column column = columnIterator.next();
		assertEquals( "characters_id", column.getName() );
		assertSame( collectionTable, index.getTable() );
	}

	@Test
	public void testTableGeneratorIndex(){
	   //todo
	}
}
