/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import java.util.Iterator;

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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel(
		annotatedClasses = {
				SimpleEntity.class,
				EntityContainingSets.class,
				SomeStuff.class
		}
)
@ServiceRegistry
@SessionFactory
public class SetOperationTests {
	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityContainingSets entity = new EntityContainingSets( 1, "first-map-entity" );
					entity.addBasic( "a value" );
					entity.addBasic( "another value" );

					entity.addSortedBasic( "def" );
					entity.addSortedBasic( "abc" );

					entity.addEnum( EnumValue.ONE );
					entity.addEnum( EnumValue.TWO );

					entity.addConvertedBasic( EnumValue.ONE );
					entity.addConvertedBasic( EnumValue.THREE );

					entity.addComponent( new SomeStuff( "the stuff - 1", "the stuff - 2" ) );
					entity.addComponent( new SomeStuff( "other stuff - 1", "other stuff - 2" ) );

					session.save( entity );
				}
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope, DomainModelScope domainModelScope) {
		scope.inTransaction(
				session -> {
					final EntityContainingSets entity = session.load( EntityContainingSets.class, 1 );
					session.delete( entity );
				}
		);
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityContainingSets entity = session.load( EntityContainingSets.class, 1 );
					assertThat( entity, is( notNullValue() ) );
					assertThat( Hibernate.isInitialized( entity ), is( false ) );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityContainingSets entity = session.get( EntityContainingSets.class, 1 );
					assertThat( entity, is( notNullValue() ) );
					assertThat( Hibernate.isInitialized( entity ), is( true ) );
					assertThat( Hibernate.isInitialized( entity.getSetOfBasics() ), is( false ) );
				}
		);
	}

	@Test
	public void testSqmFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityContainingSets entity = session.createQuery(
							"select e from EntityContainingSets e join fetch e.setOfBasics",
							EntityContainingSets.class
					).getSingleResult();

					assert Hibernate.isInitialized( entity.getSetOfBasics() );

					assert  entity.getSetOfBasics().size() == 2;
				}
		);
	}

	@Test
	public void testDeleteWithElementCollectionData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityContainingSets entity = session.load( EntityContainingSets.class, 1 );
					session.delete( entity );
				}
		);

		// re-create it so the drop-data can succeed
		createData( scope );
	}

	@Test
	public void testTriggerFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityContainingSets entity = session.get( EntityContainingSets.class, 1 );
					assert ! Hibernate.isInitialized( entity.getSetOfBasics() );

					assertThat( entity.getSetOfBasics().size(), is( 2 ) );

					assert Hibernate.isInitialized( entity.getSetOfBasics() );

					assert ! Hibernate.isInitialized( entity.getSetOfEnums() );
				}
		);
	}

	@Test
	public void testSortedSetAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityContainingSets entity = session.get( EntityContainingSets.class, 1 );
					assert ! Hibernate.isInitialized( entity.getSortedSetOfBasics() );

					Hibernate.initialize( entity.getSortedSetOfBasics() );

					assertThat( entity.getSortedSetOfBasics().size(), is( 2 ) );

					final Iterator<String> iterator = entity.getSortedSetOfBasics().iterator();
					final String first = iterator.next();
					final String second = iterator.next();
					assertThat( first, is( "abc" ) );
					assertThat( second, is( "def" ) );
				}
		);
	}
}
