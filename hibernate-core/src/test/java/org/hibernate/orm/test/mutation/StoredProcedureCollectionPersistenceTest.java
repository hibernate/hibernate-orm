/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mutation;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.supportsSqlArrayType;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStoredProcedure.class)
@ServiceRegistry(
		settings = @Setting(name = MappingSettings.USE_STORED_PROCEDURES, value = "true")
)
@DomainModel(annotatedClasses = CollectionThing.class)
@SessionFactory(useCollectingStatementInspector = true)
public class StoredProcedureCollectionPersistenceTest {

	@Test
	void testCollectionFetchAndMutationsUseStoredProcedures(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final CollectionThing thing = new CollectionThing( 1L );
			thing.labels.add( "blue" );
			session.persist( thing );
		} );

		scope.inTransaction( session -> {
			final CollectionThing thing = session.find( CollectionThing.class, 1L );
			assertNotNull( thing );

			inspector.clear();
			assertEquals( 1, thing.labels.size() );
		} );
		assertSingleStoredProcedureCollectionLoad( inspector );

		scope.inTransaction( session -> {
			final CollectionThing thing = session.find( CollectionThing.class, 1L );
			thing.labels.size();

			inspector.clear();
			thing.labels.add( "green" );
		} );
		assertSingleStoredProcedureCollectionMutation( inspector, "insert" );

		scope.inTransaction( session -> {
			final CollectionThing thing = session.find( CollectionThing.class, 1L );
			thing.labels.size();

			inspector.clear();
			thing.labels.remove( "green" );
		} );
		assertSingleStoredProcedureCollectionMutation( inspector, "delete" );

		scope.inTransaction( session -> {
			final CollectionThing thing = session.find( CollectionThing.class, 1L );
			thing.labels.size();

			inspector.clear();
			thing.labels.clear();
		} );
		assertSingleStoredProcedureCollectionMutation( inspector, "delete" );
	}

	@Test
	void testCollectionBatchFetchUsesStoredProcedureArrayLoader(SessionFactoryScope scope) {
		Assumptions.assumeTrue(
				supportsSqlArrayType( scope.getSessionFactory().getJdbcServices().getDialect() )
		);

		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final CollectionThing first = new CollectionThing( 11L );
			first.labels.add( "blue" );
			session.persist( first );

			final CollectionThing second = new CollectionThing( 12L );
			second.labels.add( "red" );
			session.persist( second );
		} );

		scope.inTransaction( session -> {
			final CollectionThing first = session.find( CollectionThing.class, 11L );
			final CollectionThing second = session.find( CollectionThing.class, 12L );
			assertNotNull( first );
			assertNotNull( second );

			inspector.clear();
			assertEquals( 1, first.labels.size() );
			assertEquals( 1, second.labels.size() );
		} );

		assertSingleStoredProcedureCollectionLoad( inspector );
	}

	private static void assertSingleStoredProcedureCollectionLoad(SQLStatementInspector inspector) {
		assertEquals( 1, inspector.getSqlQueries().size(), "Expected a single collection-load statement" );
		final String sql = inspector.getSqlQueries().get( 0 ).toLowerCase();
		assertTrue( sql.contains( "labels" ), () -> "Expected stored-procedure routine name in SQL: " + sql );
		assertFalse(
				sql.contains( "stored_collection_labels" ),
				() -> "Expected stored-procedure collection load SQL, got direct table SQL: " + sql
		);
	}

	private static void assertSingleStoredProcedureCollectionMutation(
			SQLStatementInspector inspector,
			String mutationPrefix) {
		assertEquals( 1, inspector.getSqlQueries().size(), "Expected a single collection-mutation statement" );
		final String sql = inspector.getSqlQueries().get( 0 ).toLowerCase();
		assertTrue(
				sql.contains( mutationPrefix ),
				() -> "Expected stored-procedure mutation SQL containing `" + mutationPrefix + "`, got: " + sql
		);
		assertFalse(
				sql.contains( "stored_collection_labels" ),
				() -> "Expected stored-procedure collection mutation SQL, got direct table SQL: " + sql
		);
	}
}
