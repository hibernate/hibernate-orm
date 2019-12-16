/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import org.hibernate.Hibernate;
import org.hibernate.persister.collection.CollectionPersister;

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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
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

					entityContainingMaps.addBasicByEnum( EnumValue.ONE, "one" );
					entityContainingMaps.addBasicByEnum( EnumValue.TWO, "two" );

					entityContainingMaps.addBasicByConvertedEnum( EnumValue.THREE, "three" );

					entityContainingMaps.addComponentByBasic( "the stuff", new SimpleComponent( "the stuff - 1", "the stuff - 2" ) );
					entityContainingMaps.addComponentByBasic( "the other stuff", new SimpleComponent( "the other stuff - 1", "the other stuff - 2" ) );

					session.save( entityContainingMaps );
				}
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope, DomainModelScope domainModelScope) {
		// there is a problem with deleting entities which have basic collections.  for some reason those
		// do not register as cascadable, so we do not delete the collection rows first

		scope.inTransaction(
				session -> {
					final EntityOfMaps entity = session.load( EntityOfMaps.class, 1 );
					session.delete( entity );
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

					session.delete( entity );
				}
		);

		// re-create it so the drop-data can succeed
		createData( scope );
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
