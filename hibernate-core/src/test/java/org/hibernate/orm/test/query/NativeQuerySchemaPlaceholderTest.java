/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.query.NativeQuery;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = { NativeQuerySchemaPlaceholderTest.TestEntity.class }
)
@SessionFactory
@JiraKeyGroup( value = {
		@JiraKey( value = "HHH-15269" ),
		@JiraKey( value = "HHH-18215" )
} )
public class NativeQuerySchemaPlaceholderTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( 1l, "test" );
					session.persist( testEntity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery<Tuple> nativeQuery = session.createNativeQuery(
							"UPDATE {h-schema}TestEntity SET name = 'updated_test'"
					);
					nativeQuery.executeUpdate();
				}
		);
		scope.inTransaction(
				session -> {
					List<TestEntity> testEntities = session.createQuery( "from TestEntity", TestEntity.class )
							.list();
					TestEntity testEntity = testEntities.get( 0 );
					assertThat( testEntity.name, is( "updated_test" ) );
				}
		);

		scope.inTransaction(
				session -> {
					NativeQuery<Tuple> nativeQuery = session.createNativeQuery(
							"UPDATE {h-schema}TestEntity SET name = '{updated_test'"
					);
					nativeQuery.executeUpdate();
				}
		);
		scope.inTransaction(
				session -> {
					List<TestEntity> testEntities = session.createQuery( "from TestEntity", TestEntity.class )
							.list();
					TestEntity testEntity = testEntities.get( 0 );
					assertThat( testEntity.name, is( "{updated_test" ) );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery<Long> nativeQuery = session.createNativeQuery(
							"select id from {h-schema}TestEntity",
							Long.class
					);
					List<Long> results = nativeQuery.list();
					assertThat( results.get( 0 ), is( 1l ) );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
