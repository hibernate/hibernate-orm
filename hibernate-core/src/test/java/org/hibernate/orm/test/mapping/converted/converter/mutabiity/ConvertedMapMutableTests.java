/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.converted.converter.mutabiity;

import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@JiraKey( "HHH-16081" )
@DomainModel( annotatedClasses = ConvertedMapMutableTests.TestEntity.class )
@SessionFactory( useCollectingStatementInspector = true )
public class ConvertedMapMutableTests {

	@Test
	void testMutableMap(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final TestEntity loaded = session.get( TestEntity.class, 1 );
			assertThat( loaded.values ).hasSize( 2 );
			loaded.values.put( "ghi", "789" );
			statementInspector.clear();
		} );
		assertThat( statementInspector.getSqlQueries() ).isNotEmpty();

		scope.inTransaction( (session) -> {
			final TestEntity loaded = session.get( TestEntity.class, 1 );
			assertThat( loaded.values ).hasSize( 3 );
			statementInspector.clear();
		} );
		assertThat( statementInspector.getSqlQueries() ).isEmpty();
	}

	@Test
	void testMutableMapWithMerge(SessionFactoryScope scope) {
		final TestEntity loaded = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( loaded.values ).hasSize( 2 );

		loaded.values.put( "ghi", "789" );
		scope.inTransaction( (session) -> session.merge( loaded ) );

		final TestEntity changed = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( changed.values ).hasSize( 3 );
	}

	@Test
	@JiraKey( "HHH-16132" )
	void testDirtyChecking(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		// make changes to a managed entity - should trigger update
		scope.inTransaction( (session) -> {
			final TestEntity managed = session.get( TestEntity.class, 1 );
			statementInspector.clear();
			assertThat( managed.values ).hasSize( 2 );
			// make the change
			managed.values.put( "ghi", "789" );
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );

		// make changes to a detached entity and merge it - should trigger update
		final TestEntity loaded = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( loaded.values ).hasSize( 3 );
		// make the change
		loaded.values.put( "jkl", "007" );
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

	public static class MapConverter implements AttributeConverter<Map<String,String>,String> {
		@Override
		public String convertToDatabaseColumn(Map<String,String> map) {
			if ( CollectionHelper.isEmpty( map ) ) {
				return null;
			}
			return StringHelper.join( ", ", CollectionHelper.asPairs( map ) );
		}

		@Override
		public Map<String,String> convertToEntityAttribute(String pairs) {
			if ( StringHelper.isEmpty( pairs ) ) {
				return null;
			}
			return CollectionHelper.toMap( StringHelper.split( ", ", pairs ) );
		}
	}

	@Entity( name = "TestEntity" )
	@Table( name = "entity_mutable_map" )
	public static class TestEntity {
	    @Id
	    private Integer id;

		@Convert( converter = MapConverter.class )
		@Column( name = "vals" )
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
