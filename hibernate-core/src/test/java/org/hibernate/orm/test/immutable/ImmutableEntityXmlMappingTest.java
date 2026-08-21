/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/immutable/entitywithmutablecollection/inverse/ImmutableEntity.xml"
)
@SessionFactory
@JiraKey( "HHH-20513" )
public class ImmutableEntityXmlMappingTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1L, "initial" );
					session.persist( entity );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testImmutableEntityFromXmlMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, 1L );
					entity.setName( "updated" );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, 1L );
					assertThat( entity.getName() ).isEqualTo( "initial" );
				}
		);

	}

	public static class TestEntity {
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

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
