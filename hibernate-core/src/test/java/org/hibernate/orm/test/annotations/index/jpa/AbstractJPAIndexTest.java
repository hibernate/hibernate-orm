/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.index.jpa;

import java.util.Iterator;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
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
import org.hibernate.metamodel.CollectionClassification;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu
 */
public abstract class AbstractJPAIndexTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.configureMetadataBuilder( metadataBuilder );
		metadataBuilder.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE );
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG );
	}

	@Test
	public void testTableIndex() {
		PersistentClass entity = metadata().getEntityBinding( Car.class.getName() );
		Iterator itr = entity.getTable().getUniqueKeys().values().iterator();
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


		itr = entity.getTable().getIndexes().values().iterator();
		assertTrue( itr.hasNext() );
		Index index = (Index)itr.next();
		assertFalse( itr.hasNext() );
		assertEquals( "Car_idx", index.getName() );
		assertEquals( 1, index.getColumnSpan() );
		column = index.getColumns().iterator().next();
		assertEquals( "since", column.getName() );
		assertSame( entity.getTable(), index.getTable() );
	}

	@Test
	public void testSecondaryTableIndex(){
		PersistentClass entity = metadata().getEntityBinding( Car.class.getName() );

		Join join = entity.getJoins().get( 0 );
		Iterator<Index> itr = join.getTable().getIndexes().values().iterator();
		assertTrue( itr.hasNext() );
		Index index = itr.next();
		assertFalse( itr.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
		assertEquals( 2, index.getColumnSpan() );
		Iterator<Column> columnIterator = index.getColumns().iterator();
		Column column = columnIterator.next();
		assertEquals( "dealer_name", column.getName() );
		column = columnIterator.next();
		assertEquals( "rate", column.getName() );
		assertSame( join.getTable(), index.getTable() );

	}

	@Test
	public void testCollectionTableIndex(){
		PersistentClass entity = metadata().getEntityBinding( Car.class.getName() );
		Property property = entity.getProperty( "otherDealers" );
		Set set = (Set)property.getValue();
		Table collectionTable = set.getCollectionTable();

		Iterator<Index> itr = collectionTable.getIndexes().values().iterator();
		assertTrue( itr.hasNext() );
		Index index = itr.next();
		assertFalse( itr.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
		assertEquals( 1, index.getColumnSpan() );
		Iterator<Column> columnIterator = index.getColumns().iterator();
		Column column = columnIterator.next();
		assertEquals( "name", column.getName() );
		assertSame( collectionTable, index.getTable() );

	}

	@Test
	public void testJoinTableIndex(){
		PersistentClass entity = metadata().getEntityBinding( Importer.class.getName() );
		Property property = entity.getProperty( "cars" );
		Bag set = (Bag)property.getValue();
		Table collectionTable = set.getCollectionTable();

		Iterator<Index> itr = collectionTable.getIndexes().values().iterator();
		assertTrue( itr.hasNext() );
		Index index = itr.next();
		assertFalse( itr.hasNext() );
		assertTrue( "index name is not generated", StringHelper.isNotEmpty( index.getName() ) );
		assertEquals( 1, index.getColumnSpan() );
		Iterator<Column> columnIterator = index.getColumns().iterator();
		Column column = columnIterator.next();
		assertEquals( "importers_id", column.getName() );
		assertSame( collectionTable, index.getTable() );
	}

	@Test
	public void testTableGeneratorIndex(){
		//todo
	}
}
