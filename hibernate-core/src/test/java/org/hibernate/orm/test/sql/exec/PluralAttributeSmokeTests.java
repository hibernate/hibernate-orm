/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec;

import java.sql.Statement;

import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.orm.test.metamodel.mapping.PluralAttributeTests.Component;
import org.hibernate.orm.test.metamodel.mapping.PluralAttributeTests.EntityContainingLists;
import org.hibernate.orm.test.metamodel.mapping.PluralAttributeTests.EntityContainingSets;
import org.hibernate.orm.test.metamodel.mapping.PluralAttributeTests.EnumValue;
import org.hibernate.orm.test.metamodel.mapping.PluralAttributeTests.SimpleEntity;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				SimpleEntity.class,
				EntityContainingLists.class,
				EntityContainingSets.class,
				Component.class
		}
)
@ServiceRegistry
@SessionFactory
@SuppressWarnings("WeakerAccess")
public class PluralAttributeSmokeTests {

	@Test
	public void listBaselineTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<EntityContainingLists> query = session.createQuery(
							"select e from EntityContainingLists e",
							EntityContainingLists.class
					);
					final EntityContainingLists result = query.uniqueResult();

					assertThat( result, notNullValue() );
					assertThat( result.getListOfBasics(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfBasics() ), is( false ) );
				}
		);
	}

	@Test
	public void listEagerBasicTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<EntityContainingLists> query = session.createQuery(
							"select e from EntityContainingLists e join fetch e.listOfBasics",
							EntityContainingLists.class
					);
					final EntityContainingLists result = query.uniqueResult();

					assertThat( result, notNullValue() );

					assertThat( result.getListOfBasics(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfBasics() ), is( true ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfBasics() ) );
					assertThat( result.getListOfBasics().size(), is( 3 ) );

					assertThat( result.getListOfConvertedBasics(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfConvertedBasics() ), is( false ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfConvertedBasics() ) );

					assertThat( result.getListOfEnums(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfEnums() ), is( false ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfEnums() ) );

					assertThat( result.getListOfComponents(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfComponents() ), is( false ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfComponents() ) );

					assertThat( result.getListOfEntities(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfEntities() ), is( false ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfEntities() ) );

				}
		);
	}

	@Test
	public void setBaselineTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<EntityContainingSets> query = session.createQuery(
							"select e from EntityContainingSets e",
							EntityContainingSets.class
					);

					final EntityContainingSets result = query.uniqueResult();

					assertThat( result, notNullValue() );
					assertThat( result.getSetOfBasics(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getSetOfBasics() ), is( false ) );
				}
		);
	}


	@BeforeAll
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final SimpleEntity simpleEntity1 = new SimpleEntity( 1, "simple-1" );
					final SimpleEntity simpleEntity2 = new SimpleEntity( 2, "simple-2" );

					session.save( simpleEntity1 );
					session.save( simpleEntity2 );

					{
						final EntityContainingLists entityContainingLists = new EntityContainingLists( 1, "first" );

						entityContainingLists.addBasic( "abc" );
						entityContainingLists.addBasic( "def" );
						entityContainingLists.addBasic( "ghi" );

						entityContainingLists.addConvertedBasic( EnumValue.TWO );

						entityContainingLists.addEnum( EnumValue.ONE );
						entityContainingLists.addEnum( EnumValue.THREE );

						entityContainingLists.addComponent( new Component( "first-a1", "first-another-a1" ) );
						entityContainingLists.addComponent( new Component( "first-a2", "first-another-a2" ) );

						entityContainingLists.addSimpleEntity( simpleEntity1 );
						entityContainingLists.addSimpleEntity( simpleEntity2 );

						session.save( entityContainingLists );
					}

					{
						final EntityContainingSets entity = new EntityContainingSets( 1, "first" );

						entity.addBasic( "abc" );
						entity.addBasic( "def" );
						entity.addBasic( "ghi" );

						entity.addConvertedBasic( EnumValue.TWO );

						entity.addEnum( EnumValue.ONE );
						entity.addEnum( EnumValue.THREE );

						entity.addComponent( new Component( "first-a1", "first-another-a1" ) );
						entity.addComponent( new Component( "first-a2", "first-another-a2" ) );

						entity.addSimpleEntity( simpleEntity1 );
						entity.addSimpleEntity( simpleEntity2 );

						session.save( entity );
					}
				}
		);
	}

	@AfterAll
	public void deleteTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.doWork(
						conn -> {
							try ( Statement stmnt = conn.createStatement() ) {
								stmnt.execute( "delete from EntityContainingLists_listOfEnums" );
								stmnt.execute( "delete from EntityContainingLists_listOfConvertedBasics" );
								stmnt.execute( "delete from EntityContainingLists_listOfComponents" );
								stmnt.execute( "delete from EntityContainingLists_listOfBasics" );
								stmnt.execute( "delete from entity_containing_lists_simple_entity" );
								stmnt.execute( "delete from entity_containing_lists" );
							}
						}
				)
		);
	}
}
