/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import java.util.Iterator;

import org.hibernate.Hibernate;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.hamcrest.InitializationCheckMatcher;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfSets;
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
 */
@SuppressWarnings("WeakerAccess")
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
					entity.addSortedBasic( "abc" );

					entity.addEnum( EnumValue.ONE );
					entity.addEnum( EnumValue.TWO );

					entity.addConvertedEnum( EnumValue.ONE );
					entity.addConvertedEnum( EnumValue.THREE );

					entity.addComponent( new SimpleComponent( "the stuff - 1", "the stuff - 2" ) );
					entity.addComponent( new SimpleComponent( "other stuff - 1", "other stuff - 2" ) );

					session.save( entity );
				}
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from EntityOfSets where name is not null" ).executeUpdate();
				}
		);
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.load( EntityOfSets.class, 1 );
					assertThat( entity, is( notNullValue() ) );
					assertThat( entity, InitializationCheckMatcher.isNotInitialized() );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.get( EntityOfSets.class, 1 );
					assertThat( entity, is( notNullValue() ) );
					assertThat( entity, InitializationCheckMatcher.isInitialized() );
					assertThat( entity.getSetOfBasics(), InitializationCheckMatcher.isNotInitialized() );
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

					assertThat( entity.getSetOfBasics(), InitializationCheckMatcher.isNotInitialized() );
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

					assertThat( entity.getSetOfBasics(), InitializationCheckMatcher.isInitialized() );

					assert  entity.getSetOfBasics().size() == 2;
				}
		);
	}

	@Test
	public void testDeleteWithElementCollectionData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.load( EntityOfSets.class, 1 );
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
					final EntityOfSets entity = session.get( EntityOfSets.class, 1 );
					assertThat( entity.getSetOfBasics(), InitializationCheckMatcher.isNotInitialized() );

					// trigger the init
					assertThat( entity.getSetOfBasics().size(), is( 2 ) );

					assertThat( entity.getSetOfBasics(), InitializationCheckMatcher.isInitialized() );
					assertThat( entity.getSetOfEnums(), InitializationCheckMatcher.isNotInitialized() );
				}
		);
	}

	@Test
	public void testSortedSetAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfSets entity = session.get( EntityOfSets.class, 1 );
					assertThat( entity.getSortedSetOfBasics(), InitializationCheckMatcher.isNotInitialized() );

					// trigger the init
					Hibernate.initialize( entity.getSortedSetOfBasics() );
					assertThat( entity.getSortedSetOfBasics(), InitializationCheckMatcher.isInitialized() );
					assertThat( entity.getSortedSetOfBasics().size(), is( 2 ) );
					assertThat( entity.getSetOfEnums(), InitializationCheckMatcher.isNotInitialized() );

					final Iterator<String> iterator = entity.getSortedSetOfBasics().iterator();
					final String first = iterator.next();
					final String second = iterator.next();
					assertThat( first, is( "abc" ) );
					assertThat( second, is( "def" ) );
				}
		);
	}
}
