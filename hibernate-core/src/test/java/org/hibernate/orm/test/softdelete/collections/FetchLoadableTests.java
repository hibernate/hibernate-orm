/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import java.sql.Statement;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Hibernate;

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
@DomainModel(annotatedClasses = CollectionOwner2.class)
@SessionFactory(useCollectingStatementInspector = true)
public class FetchLoadableTests {
	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final CollectionOwner2 owner = new CollectionOwner2( 1, "first" );
			final CollectionOwner2 owner2 = new CollectionOwner2( 2, "second" );

			owner.setBatchLoadable( new HashSet<>() );
			owner.getBatchLoadable().add( "batchable1" );
			owner.getBatchLoadable().add( "batchable2" );
			owner.getBatchLoadable().add( "batchable3" );

			owner.setSubSelectLoadable( new HashSet<>() );
			owner.getSubSelectLoadable().add( "subselectable1" );
			owner.getSubSelectLoadable().add( "subselectable2" );
			owner.getSubSelectLoadable().add( "subselectable3" );

			owner2.setBatchLoadable( new HashSet<>() );
			owner2.getBatchLoadable().add( "batchable21" );
			owner2.getBatchLoadable().add( "batchable22" );
			owner2.getBatchLoadable().add( "batchable23" );

			owner2.setSubSelectLoadable( new HashSet<>() );
			owner2.getSubSelectLoadable().add( "subselectable21" );
			owner2.getSubSelectLoadable().add( "subselectable22" );
			owner2.getSubSelectLoadable().add( "subselectable23" );

			session.persist( owner );
			session.persist( owner2 );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.doWork( (connection) -> {
			final Statement statement = connection.createStatement();
			statement.execute( "delete from batch_loadables" );
			statement.execute( "delete from subselect_loadables" );
			statement.execute( "delete from coll_owner2" );
		} ) );
	}

	@Test
	void testBatchLoading(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final List<CollectionOwner2> result = session.createQuery(
					"from CollectionOwner2 order",
					CollectionOwner2.class
			).list();

			final CollectionOwner2 first = result.get( 0 );
			assertThat( Hibernate.isInitialized( first ) ).isTrue();
			assertThat( Hibernate.isInitialized( first.getBatchLoadable() ) ).isFalse();

			final CollectionOwner2 second = result.get( 1 );
			assertThat( Hibernate.isInitialized( second ) ).isTrue();
			assertThat( Hibernate.isInitialized( second.getBatchLoadable() ) ).isFalse();

			statementInspector.clear();

			// trigger loading one of the batch-loadable collections
			first.getBatchLoadable().size();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).containsAnyOf( "active='Y'", "active=N'Y'" );
			assertThat( Hibernate.isInitialized( first.getBatchLoadable() ) ).isTrue();
			assertThat( Hibernate.isInitialized( second.getBatchLoadable() ) ).isTrue();
		} );
	}

	@Test
	void testSubSelectLoading(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final List<CollectionOwner2> result = session.createQuery(
					"from CollectionOwner2 order",
					CollectionOwner2.class
			).list();

			final CollectionOwner2 first = result.get( 0 );
			assertThat( Hibernate.isInitialized( first ) ).isTrue();
			assertThat( Hibernate.isInitialized( first.getSubSelectLoadable() ) ).isFalse();

			final CollectionOwner2 second = result.get( 1 );
			assertThat( Hibernate.isInitialized( second ) ).isTrue();
			assertThat( Hibernate.isInitialized( second.getSubSelectLoadable() ) ).isFalse();

			statementInspector.clear();

			// trigger loading one of the subselect-loadable collections
			first.getSubSelectLoadable().size();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).containsAnyOf( "active=1", "active=N'Y'" );
			assertThat( Hibernate.isInitialized( first.getSubSelectLoadable() ) ).isTrue();
			assertThat( Hibernate.isInitialized( second.getSubSelectLoadable() ) ).isTrue();
		} );
	}
}
