/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel(
		annotatedClasses = {
				SimpleEntity.class,
				EntityContainingMaps.class,
				SomeStuff.class
		}
)
@ServiceRegistry
@SessionFactory
public class MapOperationTests {
	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityContainingMaps entityContainingMaps = new EntityContainingMaps( 1, "first-map-entity" );
					entityContainingMaps.addBasicByBasic( "someKey", "someValue" );
					entityContainingMaps.addBasicByBasic( "anotherKey", "anotherValue" );

					entityContainingMaps.addBasicByEnum( EnumValue.ONE, "one" );
					entityContainingMaps.addBasicByEnum( EnumValue.TWO, "two" );

					entityContainingMaps.addBasicByConvertedEnum( EnumValue.THREE, "three" );

					entityContainingMaps.addSomeStuffByBasic( "the stuff", new SomeStuff( "the stuff - 1", "the stuff - 2" ) );
					entityContainingMaps.addSomeStuffByBasic( "the other stuff", new SomeStuff( "the other stuff - 1", "the other stuff - 2" ) );

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
					final EntityContainingMaps entity = session.load( EntityContainingMaps.class, 1 );
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
					final EntityContainingMaps entity = session.createQuery(
							"select e from EntityContainingMaps e join fetch e.basicByEnum",
							EntityContainingMaps.class
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
					final EntityContainingMaps entity = session.createQuery(
							"select e from EntityContainingMaps e join fetch e.basicByEnum",
							EntityContainingMaps.class
					).getSingleResult();

					session.delete( entity );
				}
		);

		// re-create it so the drop-data can succeed
		createData( scope );
	}
}
