/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.converted.converter.mutabiity;

import java.util.Map;

import org.hibernate.annotations.Immutable;
import org.hibernate.internal.util.collections.CollectionHelper;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = ConvertedMapImmutableTests.TestEntity.class )
@SessionFactory( useCollectingStatementInspector = true )
public class ConvertedMapImmutableTests {

	@Test
	@JiraKey( "HHH-16081" )
	void testManagedUpdate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final TestEntity loaded = session.get( TestEntity.class, 1 );
			loaded.values.put( "ghi", "789" );
			statementInspector.clear();
		} );

		final TestEntity after = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( after.values ).hasSize( 2 );
	}

	@Test
	@JiraKey( "HHH-16081" )
	@FailureExpected( reason = "Fails due to HHH-16132 - Hibernate believes the attribute is dirty, even though it is immutable." )
	void testMerge(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		final TestEntity loaded = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( loaded.values ).hasSize( 2 );

		loaded.values.put( "ghi", "789" );
		statementInspector.clear();
		scope.inTransaction( (session) -> session.merge( loaded ) );

		final TestEntity merged = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( merged.values ).hasSize( 2 );
	}

	@Test
	@JiraKey( "HHH-16132" )
	@FailureExpected( reason = "Fails due to HHH-16132 - Hibernate believes the attribute is dirty, even though it is immutable." )
	void testDirtyChecking(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		// make changes to a managed entity - should not trigger update since it is immutable
		scope.inTransaction( (session) -> {
			final TestEntity managed = session.get( TestEntity.class, 1 );
			statementInspector.clear();
			assertThat( managed.values ).hasSize( 2 );
			// make the change
			managed.values.put( "ghi", "789" );
		} );
		assertThat( statementInspector.getSqlQueries() ).isEmpty();

		// make no changes to a detached entity and merge it - should not trigger update
		final TestEntity loaded = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( loaded.values ).hasSize( 2 );
		// make the change
		loaded.values.put( "ghi", "789" );
		statementInspector.clear();
		scope.inTransaction( (session) -> session.merge( loaded ) );
		// the SELECT
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
	}

	@Test
	@JiraKey( "HHH-16132" )
	void testNotDirtyChecking(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		// make changes to a managed entity - should not trigger update
		scope.inTransaction( (session) -> {
			final TestEntity managed = session.get( TestEntity.class, 1 );
			statementInspector.clear();
			assertThat( managed.values ).hasSize( 2 );
		} );
		assertThat( statementInspector.getSqlQueries() ).isEmpty();

		// make no changes to a detached entity and merge it - should not trigger update
		final TestEntity loaded = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( loaded.values ).hasSize( 2 );
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
					CollectionHelper.toMap(
							"abc", "123",
							"def", "456"
					)
			) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete TestEntity" ).executeUpdate();
		} );
	}

	@Immutable
	public static class ImmutableMapConverter extends ConvertedMapMutableTests.MapConverter {
	}

	@Entity( name = "TestEntity" )
	@Table( name = "entity_immutable_map" )
	public static class TestEntity {
	    @Id
	    private Integer id;

		@Convert( converter = ImmutableMapConverter.class )
		@Column( name="vals" )
		private Map<String,String> values;

		private TestEntity() {
			// for use by Hibernate
		}

		public TestEntity(
				Integer id,
				Map<String,String> values) {
			this.id = id;
			this.values = values;
		}
	}
}
