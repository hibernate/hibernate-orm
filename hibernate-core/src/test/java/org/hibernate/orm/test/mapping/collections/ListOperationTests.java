/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfLists;
import org.hibernate.testing.orm.domain.gambit.EnumValue;
import org.hibernate.testing.orm.domain.gambit.SimpleComponent;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory
public class ListOperationTests {
	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfLists entityContainingLists = new EntityOfLists( 1, "first" );

					entityContainingLists.addBasic( "abc" );
					entityContainingLists.addBasic( "def" );
					entityContainingLists.addBasic( "ghi" );

					entityContainingLists.addConvertedEnum( EnumValue.TWO );

					entityContainingLists.addEnum( EnumValue.ONE );
					entityContainingLists.addEnum( EnumValue.THREE );

					entityContainingLists.addComponent( new SimpleComponent( "first-a1", "first-another-a1" ) );
					entityContainingLists.addComponent( new SimpleComponent( "first-a2", "first-another-a2" ) );

					session.persist( entityContainingLists );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void listBaselineTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<EntityOfLists> query = session.createQuery(
							"select e from EntityOfLists e",
							EntityOfLists.class
					);
					final EntityOfLists result = query.uniqueResult();

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
					final Query<EntityOfLists> query = session.createQuery(
							"select e from EntityOfLists e join fetch e.listOfBasics",
							EntityOfLists.class
					);
					final EntityOfLists result = query.uniqueResult();

					assertThat( result, notNullValue() );

					assertThat( result.getListOfBasics(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfBasics() ), is( true ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfBasics() ) );
					assertThat( result.getListOfBasics(), hasSize( 3 ) );

					assertThat( result.getListOfConvertedEnums(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfConvertedEnums() ), is( false ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfConvertedEnums() ) );

					assertThat( result.getListOfEnums(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfEnums() ), is( false ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfEnums() ) );

					assertThat( result.getListOfComponents(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfComponents() ), is( false ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfComponents() ) );

					assertThat( result.getListOfOneToMany(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfOneToMany() ), is( false ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfOneToMany() ) );

					assertThat( result.getListOfManyToMany(), notNullValue() );
					assertThat( Hibernate.isInitialized( result.getListOfManyToMany() ), is( false ) );
					assertTrue( session.getPersistenceContext().containsCollection( (PersistentCollection) result.getListOfManyToMany() ) );

				}
		);
	}
}
