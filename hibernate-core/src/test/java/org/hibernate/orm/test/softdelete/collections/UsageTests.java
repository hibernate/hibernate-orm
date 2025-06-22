/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;


import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = { CollectionOwner.class, CollectionOwned.class })
@SessionFactory(useCollectingStatementInspector = true)
public class UsageTests {
	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final CollectionOwned owned = new CollectionOwned( 1, "owned" );
			session.persist( owned );

			final CollectionOwner owner = new CollectionOwner( 1, "owner" );
			owner.addElement( "an element" );
			owner.addElement( "another element" );
			owner.addManyToMany( owned );
			session.persist( owner );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testHqlElements(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session
					.createQuery( "select o from CollectionOwner o join fetch o.elements where o.id = 1", CollectionOwner.class )
					.uniqueResult();
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).containsAnyOf( "deleted='N'", "deleted=N'N'" );
	}

	@Test
	void testHqlManyToMany(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session
					.createQuery( "select o from CollectionOwner o join fetch o.manyToMany where o.id = 1", CollectionOwner.class )
					.uniqueResult();
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "gone=0" );
	}

	@Test
	void testRemoveElements(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final CollectionOwner owner = session
					.createQuery( "select o from CollectionOwner o join fetch o.elements where o.id = 1", CollectionOwner.class )
					.uniqueResult();
			statementInspector.clear();
			owner.getElements().clear();
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).startsWith( "update " );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).containsAnyOf( "deleted='Y'", "deleted=N'Y'" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).containsAnyOf( "deleted='N'", "deleted=N'N'" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( "'N'" );

		scope.inTransaction( (session) -> {
			final CollectionOwner owner = session.get( CollectionOwner.class, 1 );
			assertThat( owner.getElements() ).hasSize( 0 );
		} );
	}

	@Test
	void testRemoveManyToMany(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final CollectionOwner owner = session
					.createQuery( "select o from CollectionOwner o join fetch o.manyToMany where o.id = 1", CollectionOwner.class )
					.uniqueResult();
			statementInspector.clear();
			owner.getManyToMany().clear();
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).startsWith( "update " );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "gone=1" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( "gone=0" );

		scope.inTransaction( (session) -> {
			final CollectionOwner owner = session.get( CollectionOwner.class, 1 );
			statementInspector.clear();
			assertThat( owner.getManyToMany() ).hasSize( 0 );
		} );
	}

	@Test
	void testDeleteElement(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final CollectionOwner owner = session
					.createQuery( "select o from CollectionOwner o join fetch o.elements where o.id = 1", CollectionOwner.class )
					.uniqueResult();
			statementInspector.clear();
			owner.getElements().remove( "an element" );
		} );
		// this will be a "recreate"
		assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).startsWith( "update " );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).containsAnyOf( "deleted='Y'", "deleted=N'Y'" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).containsAnyOf( "deleted='N'", "deleted=N'N'" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( "'N'" );
		assertThat( statementInspector.getSqlQueries().get( 1 ) ).startsWith( "insert " );

		scope.inTransaction( (session) -> {
			final CollectionOwner owner = session.get( CollectionOwner.class, 1 );
			assertThat( owner.getElements() ).hasSize( 1 );
		} );
	}

	@Test
	void testDeleteManyToMany(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final CollectionOwned owned = session.get( CollectionOwned.class, 1 );
			final CollectionOwner owner = session
					.createQuery( "select o from CollectionOwner o join fetch o.manyToMany where o.id = 1", CollectionOwner.class )
					.uniqueResult();
			statementInspector.clear();
			owner.getManyToMany().remove( owned );
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).startsWith( "update " );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "gone=1" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( "gone=0" );

		scope.inTransaction( (session) -> {
			final CollectionOwner owner = session.get( CollectionOwner.class, 1 );
			assertThat( owner.getManyToMany() ).hasSize( 0 );
		} );
	}
}
