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

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.supportsSqlArrayType;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStoredProcedure.class)
@ServiceRegistry(
		settings = @Setting(name = MappingSettings.USE_STORED_PROCEDURES, value = "true")
)
@DomainModel(annotatedClasses = Thing.class)
@SessionFactory(useCollectingStatementInspector = true)
public class StoredProcedurePersistenceTest {

	@Test
	void testStoredProcedureMutations(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		final boolean supportsArrayMultiLoad =
				supportsSqlArrayType( scope.getSessionFactory().getJdbcServices().getDialect() );

		scope.inTransaction(session -> {
			inspector.clear();
			session.persist(new Thing(1L, "first"));
		});
		assertSingleCallableMutation( inspector, "insert" );

		scope.inTransaction(session -> {
			inspector.clear();
			final Thing entity = session.find( Thing.class, 1L);
			assertNotNull(entity);
			assertSingleStoredProcedureLoad( inspector, "get" );

			inspector.clear();
			session.refresh(entity);
			assertSingleStoredProcedureLoad( inspector, "get" );

			inspector.clear();
			final List<Thing> entities = session.findMultiple(
					Thing.class,
					List.of( 1L, 2L )
			);
			assertEquals( 2, entities.size() );
			assertNotNull( entities.get( 0 ) );
			assertNull( entities.get( 1 ) );
			if ( supportsArrayMultiLoad ) {
				assertSingleStoredProcedureLoad( inspector, "get" );
			}
			else {
				assertSingleDirectLoad( inspector );
			}

			inspector.clear();
			final List<Thing> namedHql = session.createNamedQuery(
					"Thing.byName",
					Thing.class
			).setParameter( "name", "first" ).list();
			assertEquals( 1, namedHql.size() );
			assertSingleStoredProcedureLoad( inspector, "byName" );
			assertNamedRoutineNameCanonicalized( inspector, "Thing.byName" );
		});

		scope.inTransaction(session -> {
			final Thing entity = session.find( Thing.class, 1L);
			inspector.clear();
			entity.name = "second";
		});
		assertSingleCallableMutation( inspector, "update" );

		scope.inTransaction(session -> {
			final Thing entity = session.find( Thing.class, 1L);
			inspector.clear();
			session.remove(entity);
		});
		assertSingleCallableMutation( inspector, "delete" );

		scope.inTransaction(session -> {
			assertNull(session.find( Thing.class, 1L));
		});
	}

	private static void assertSingleCallableMutation(SQLStatementInspector inspector, String expectedRoutinePrefix) {
		assertEquals(1, inspector.getSqlQueries().size(), "Expected a single mutation statement");
		final String sql = inspector.getSqlQueries().get( 0 );
		assertTrue(
				sql.startsWith( "{call " + expectedRoutinePrefix ),
				() -> "Expected callable mutation SQL using routine `" + expectedRoutinePrefix + "`, got: " + sql
		);
	}

	private static void assertSingleStoredProcedureLoad(SQLStatementInspector inspector, String expectedRoutineFragment) {
		assertEquals(1, inspector.getSqlQueries().size(), "Expected a single load statement");
		final String sql = inspector.getSqlQueries().get( 0 );
		assertTrue(
				sql.contains( expectedRoutineFragment ),
				() -> "Expected stored-procedure load SQL containing `" + expectedRoutineFragment + "`, got: " + sql
		);
	}

	private static void assertSingleDirectLoad(SQLStatementInspector inspector) {
		assertEquals( 1, inspector.getSqlQueries().size(), "Expected a single load statement" );
		final String sql = inspector.getSqlQueries().get( 0 );
		assertFalse(
				sql.toLowerCase( Locale.ROOT ).contains( "getthing" ),
				() -> "Expected direct SQL load for findMultiple, got stored-procedure SQL: " + sql
		);
	}

	private static void assertNamedRoutineNameCanonicalized(SQLStatementInspector inspector, String namedQueryName) {
		assertEquals( 1, inspector.getSqlQueries().size(), "Expected a single named-query load statement" );
		final String sql = inspector.getSqlQueries().get( 0 );
		assertFalse(
				sql.contains( namedQueryName ),
				() -> "Expected canonicalized stored-procedure name, got raw named-query name in SQL: " + sql
		);
	}
}
