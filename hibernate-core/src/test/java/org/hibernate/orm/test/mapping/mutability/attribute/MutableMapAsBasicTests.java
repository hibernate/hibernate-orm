/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.attribute;

import java.util.Map;

import org.hibernate.orm.test.mapping.mutability.converted.MapConverter;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@JiraKey( "HHH-16081" )
@DomainModel( annotatedClasses = MutableMapAsBasicTests.TestEntity.class )
@SessionFactory( useCollectingStatementInspector = true )
public class MutableMapAsBasicTests {

	@Test
	@JiraKey( "HHH-16132" )
	void testDirtyChecking(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		// make changes to a managed entity - should trigger update
		scope.inTransaction( (session) -> {
			final TestEntity managed = session.find( TestEntity.class, 1 );
			statementInspector.clear();
			assertThat( managed.data ).hasSize( 2 );
			// make the change
			managed.data.put( "ghi", "789" );
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );

		// make changes to a detached entity and merge it - should trigger update
		final TestEntity loaded = scope.fromTransaction( (session) -> session.find( TestEntity.class, 1 ) );
		assertThat( loaded.data ).hasSize( 3 );
		// make the change
		loaded.data.put( "jkl", "007" );
		statementInspector.clear();
		scope.inTransaction( (session) -> session.merge( loaded ) );
		// the SELECT + UPDATE
		assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
	}

	@Test
	@JiraKey( "HHH-16132" )
	void testNotDirtyChecking(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		// make no changes to a managed entity - should not trigger update
		scope.inTransaction( (session) -> {
			final TestEntity managed = session.find( TestEntity.class, 1 );
			statementInspector.clear();
			assertThat( managed.data ).hasSize( 2 );
		} );
		assertThat( statementInspector.getSqlQueries() ).isEmpty();

		// make no changes to a detached entity and merge it - should not trigger update
		final TestEntity loaded = scope.fromTransaction( (session) -> session.find( TestEntity.class, 1 ) );
		assertThat( loaded.data ).hasSize( 2 );
		statementInspector.clear();
		scope.inTransaction( (session) -> session.merge( loaded ) );
		// the SELECT
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TestEntity(
					1,
					Map.of(
							"abc", "123",
							"def", "456"
					)
			) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "TestEntity" )
	@Table( name = "entity_mutable_map" )
	public static class TestEntity {
		@Id
		private Integer id;

		@Convert( converter = MapConverter.class )
		private Map<String,String> data;

		private TestEntity() {
			// for use by Hibernate
		}

		public TestEntity(Integer id, Map<String,String> data) {
			this.id = id;
			this.data = data;
		}
	}
}
