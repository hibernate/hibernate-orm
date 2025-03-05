/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.persister.collection.CollectionPersister;

import org.hibernate.testing.hamcrest.InitializationCheckMatcher;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfMaps;
import org.hibernate.testing.orm.domain.gambit.EnumValue;
import org.hibernate.testing.orm.domain.gambit.SimpleComponent;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 * @author Fabio Massimo Ercoli
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory
public class MapOperationTests {
	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfMaps entityContainingMaps = new EntityOfMaps( 1, "first-map-entity" );
					entityContainingMaps.addBasicByBasic( "someKey", "someValue" );
					entityContainingMaps.addBasicByBasic( "anotherKey", "anotherValue" );

					entityContainingMaps.addSortedBasicByBasic( "key2", "value2" );
					entityContainingMaps.addSortedBasicByBasic( "key1", "value1" );

					entityContainingMaps.addSortedBasicByBasicWithComparator( "kEy1", "value1" );
					entityContainingMaps.addSortedBasicByBasicWithComparator( "KeY2", "value2" );

					entityContainingMaps.addSortedBasicByBasicWithSortNaturalByDefault( "key2", "value2" );
					entityContainingMaps.addSortedBasicByBasicWithSortNaturalByDefault( "key1", "value1" );

					entityContainingMaps.addBasicByEnum( EnumValue.ONE, "one" );
					entityContainingMaps.addBasicByEnum( EnumValue.TWO, "two" );

					entityContainingMaps.addBasicByConvertedEnum( EnumValue.THREE, "three" );

					entityContainingMaps.addComponentByBasic( "the stuff", new SimpleComponent( "the stuff - 1", "the stuff - 2" ) );
					entityContainingMaps.addComponentByBasic( "the other stuff", new SimpleComponent( "the other stuff - 1", "the other stuff - 2" ) );

					session.persist( entityContainingMaps );
				}
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope, DomainModelScope domainModelScope) {
		// there is a problem with deleting entities which have basic collections.  for some reason those
		// do not register as cascadable, so we do not delete the collection rows first

		scope.inTransaction(
				session -> {
					final EntityOfMaps entity = session.getReference( EntityOfMaps.class, 1 );
					session.remove( entity );
				}
		);

		// uber hacky temp way:

//		TempDropDataHelper.cleanDatabaseSchema( scope, domainModelScope );
	}

	@Test
	public void testSqmFetching(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfMaps entity = session.createQuery(
							"select e from EntityOfMaps e join fetch e.basicByEnum",
							EntityOfMaps.class
					).getSingleResult();

					assert Hibernate.isInitialized( entity.getBasicByEnum() );

					assert  entity.getBasicByEnum().size() == 2;
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfMaps entity = session.createQuery(
							"select e from EntityOfMaps e join fetch e.basicByEnum",
							EntityOfMaps.class
					).getSingleResult();

					session.remove( entity );
				}
		);

		// re-create it so the drop-data can succeed
		createData( scope );
	}

	@Test
	public void testSortedMapAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfMaps entity = session.get( EntityOfMaps.class, 1 );
					assertThat( entity.getSortedBasicByBasic(), InitializationCheckMatcher.isNotInitialized() );

					// trigger the init
					Hibernate.initialize( entity.getSortedBasicByBasic() );
					assertThat( entity.getSortedBasicByBasic(), InitializationCheckMatcher.isInitialized() );
					assertThat( entity.getSortedBasicByBasic().size(), is( 2 ) );
					assertThat( entity.getBasicByEnum(), InitializationCheckMatcher.isNotInitialized() );

					final Iterator<Map.Entry<String, String>> iterator = entity.getSortedBasicByBasic().entrySet().iterator();
					final Map.Entry<String, String> first = iterator.next();
					final Map.Entry<String, String> second = iterator.next();
					assertThat( first.getKey(), is( "key1" ) );
					assertThat( first.getValue(), is( "value1" ) );
					assertThat( second.getKey(), is( "key2" ) );
					assertThat( second.getValue(), is( "value2" ) );
				}
		);
	}

	@Test
	public void testSortedMapWithComparatorAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfMaps entity = session.get( EntityOfMaps.class, 1 );
					assertThat( entity.getSortedBasicByBasicWithComparator(), InitializationCheckMatcher.isNotInitialized() );

					// trigger the init
					Hibernate.initialize( entity.getSortedBasicByBasicWithComparator() );
					assertThat( entity.getSortedBasicByBasicWithComparator(), InitializationCheckMatcher.isInitialized() );
					assertThat( entity.getSortedBasicByBasicWithComparator().size(), is( 2 ) );
					assertThat( entity.getBasicByEnum(), InitializationCheckMatcher.isNotInitialized() );

					final Iterator<Map.Entry<String, String>> iterator = entity.getSortedBasicByBasicWithComparator().entrySet().iterator();
					final Map.Entry<String, String> first = iterator.next();
					final Map.Entry<String, String> second = iterator.next();
					assertThat( first.getKey(), is( "kEy1" ) );
					assertThat( first.getValue(), is( "value1" ) );
					assertThat( second.getKey(), is( "KeY2" ) );
					assertThat( second.getValue(), is( "value2" ) );
				}
		);
	}

	@Test
	public void testSortedMapWithSortNaturalByDefaultAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfMaps entity = session.get( EntityOfMaps.class, 1 );
					assertThat( entity.getSortedBasicByBasicWithSortNaturalByDefault(), InitializationCheckMatcher.isNotInitialized() );

					// trigger the init
					Hibernate.initialize( entity.getSortedBasicByBasicWithSortNaturalByDefault() );
					assertThat( entity.getSortedBasicByBasicWithSortNaturalByDefault(), InitializationCheckMatcher.isInitialized() );
					assertThat( entity.getSortedBasicByBasicWithSortNaturalByDefault().size(), is( 2 ) );
					assertThat( entity.getBasicByEnum(), InitializationCheckMatcher.isNotInitialized() );

					final Iterator<Map.Entry<String, String>> iterator = entity.getSortedBasicByBasicWithSortNaturalByDefault().entrySet().iterator();
					final Map.Entry<String, String> first = iterator.next();
					final Map.Entry<String, String> second = iterator.next();
					assertThat( first.getKey(), is( "key1" ) );
					assertThat( first.getValue(), is( "value1" ) );
					assertThat( second.getKey(), is( "key2" ) );
					assertThat( second.getValue(), is( "value2" ) );
				}
		);
	}

	@Test
	public void testOrderedMap(SessionFactoryScope scope) {
		// atm we can only check the fragment translation
		final CollectionPersister collectionDescriptor = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findCollectionDescriptor( EntityOfMaps.class.getName() + ".componentByBasicOrdered" );
		assertThat(
				collectionDescriptor.getAttributeMapping().getOrderByFragment(),
				notNullValue()
		);

		scope.inTransaction(
				session -> session.createQuery( "from EntityOfMaps e join fetch e.componentByBasicOrdered" ).list()
		);
	}
}
