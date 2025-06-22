/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.Iterator;

import org.hibernate.Hibernate;
import org.hibernate.persister.collection.CollectionPersister;

import org.hibernate.testing.hamcrest.InitializationCheckMatcher;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfSets;
import org.hibernate.testing.orm.domain.gambit.EnumValue;
import org.hibernate.testing.orm.domain.gambit.SimpleComponent;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.collection.IsIterableContainingInOrder;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isInitialized;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isNotInitialized;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory
public class SetOperationTests {
	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = new EntityOfSets( 1, "first-map-entity" );
					entity.addBasic( "a value" );
					entity.addBasic( "another value" );

					entity.addSortedBasic( "def" );
					entity.addSortedBasic( "cde" );
					entity.addSortedBasic( "bcd" );
					entity.addSortedBasic( "efg" );
					entity.addSortedBasic( "abc" );

					entity.addSortedBasicWithComparator( "DeF" );
					entity.addSortedBasicWithComparator( "cDe" );
					entity.addSortedBasicWithComparator( "bcD" );
					entity.addSortedBasicWithComparator( "Efg" );
					entity.addSortedBasicWithComparator( "aBC" );

					entity.addSortedBasicWithSortNaturalByDefault( "def" );
					entity.addSortedBasicWithSortNaturalByDefault( "abc" );

					entity.addEnum( EnumValue.ONE );
					entity.addEnum( EnumValue.TWO );

					entity.addConvertedEnum( EnumValue.ONE );
					entity.addConvertedEnum( EnumValue.THREE );

					entity.addComponent( new SimpleComponent( "the stuff - 1", "the stuff - 2" ) );
					entity.addComponent( new SimpleComponent( "other stuff - 1", "other stuff - 2" ) );

					session.persist( entity );
				}
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.getReference( EntityOfSets.class, 1 );
					assertThat( entity, notNullValue() );
					assertThat( entity, isNotInitialized() );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.get( EntityOfSets.class, 1 );
					assertThat( entity, notNullValue() );
					assertThat( entity, isInitialized() );
					assertThat( entity.getSetOfBasics(), isNotInitialized() );
				}
		);
	}

	@Test
	public void testSqm(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.createQuery(
							"select e from EntityOfSets e",
							EntityOfSets.class
					).getSingleResult();

					assertThat( entity.getSetOfBasics(), isNotInitialized() );
				}
		);
	}

	@Test
	public void testSqmFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.createQuery(
							"select e from EntityOfSets e join fetch e.setOfBasics",
							EntityOfSets.class
					).getSingleResult();

					assertThat( entity.getSetOfBasics(), isInitialized() );
					assertThat( entity.getSetOfBasics(), hasSize( 2 ) );
				}
		);
	}

	@Test
	public void testDeleteWithElementCollectionData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.getReference( EntityOfSets.class, 1 );
					session.remove( entity );
				}
		);

		// re-create it so the drop-data can succeed
		createData( scope );
	}

	@Test
	public void testTriggerFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.get( EntityOfSets.class, 1 );
					assertThat( entity.getSetOfBasics(), isNotInitialized() );

					// trigger the init
					assertThat( entity.getSetOfBasics(), hasSize( 2 ) );

					assertThat( entity.getSetOfBasics(), isInitialized() );
					assertThat( entity.getSetOfEnums(), isNotInitialized() );
				}
		);
	}

	@Test
	public void testSortedSetAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.get( EntityOfSets.class, 1 );
					assertThat( entity.getSortedSetOfBasics(), isNotInitialized() );

					// trigger the init
					Hibernate.initialize( entity.getSortedSetOfBasics() );
					assertThat( entity.getSortedSetOfBasics(), isInitialized() );
					assertThat( entity.getSortedSetOfBasics(), hasSize( 5 ) );
					assertThat( entity.getSetOfEnums(), isNotInitialized() );

					assertThat( entity.getSortedSetOfBasics(), IsIterableContainingInOrder.contains(
							"abc",
							"bcd",
							"cde",
							"def",
							"efg"
					) );
				}
		);
	}

	@Test
	public void testSortedSetWithComparatorAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.get( EntityOfSets.class, 1 );
					assertThat( entity.getSortedSetOfBasicsWithComparator(), isNotInitialized() );

					// trigger the init
					Hibernate.initialize( entity.getSortedSetOfBasicsWithComparator() );
					assertThat( entity.getSortedSetOfBasicsWithComparator(), isInitialized() );
					assertThat( entity.getSortedSetOfBasicsWithComparator(), hasSize( 5 ) );
					assertThat( entity.getSetOfEnums(), isNotInitialized() );

					assertThat( entity.getSortedSetOfBasicsWithComparator(), IsIterableContainingInOrder.contains(
							"aBC",
							"bcD",
							"cDe",
							"DeF",
							"Efg"
					) );
				}
		);
	}

	@Test
	public void testSortedSetWithSortNaturalByDefaultAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.get( EntityOfSets.class, 1 );
					assertThat( entity.getSortedSetOfBasicsWithSortNaturalByDefault(), InitializationCheckMatcher.isNotInitialized() );

					// trigger the init
					Hibernate.initialize( entity.getSortedSetOfBasicsWithSortNaturalByDefault() );
					assertThat( entity.getSortedSetOfBasicsWithSortNaturalByDefault(), InitializationCheckMatcher.isInitialized() );
					assertThat( entity.getSortedSetOfBasicsWithSortNaturalByDefault().size(), is( 2 ) );
					assertThat( entity.getSetOfEnums(), InitializationCheckMatcher.isNotInitialized() );

					final Iterator<String> iterator = entity.getSortedSetOfBasicsWithSortNaturalByDefault().iterator();
					final String first = iterator.next();
					final String second = iterator.next();
					assertThat( first, is( "abc" ) );
					assertThat( second, is( "def" ) );
				}
		);
	}

	@Test
	public void testOrderedSet(SessionFactoryScope scope) {
		// atm we can only check the fragment translation
		final CollectionPersister collectionDescriptor = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findCollectionDescriptor( EntityOfSets.class.getName() + ".orderedSetOfBasics" );
		assertThat(
				collectionDescriptor.getAttributeMapping().getOrderByFragment(),
				notNullValue()
		);

		scope.inTransaction(
				session -> session.createQuery( "from EntityOfSets e join fetch e.orderedSetOfBasics" ).list()
		);
	}
}
