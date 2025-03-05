/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = GeneratedWritableIdTest.TestEntity.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18679")
@RequiresDialect(H2Dialect.class)
public class GeneratedWritableIdTest {
	@Test
	public void testDefaultId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity entity = new TestEntity( null, "entity_1" );
			session.persist( entity );
			session.flush();
			assertThat( entity.getId() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testAssignedId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity entity = new TestEntity( 2L, "entity_2" );
			session.persist( entity );
			session.flush();
			assertThat( entity.getId() ).isEqualTo( 2L );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "TestEntity")
	static class TestEntity {
		@Id
		@Generated(writable = true)
		@ColumnDefault("1")
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
