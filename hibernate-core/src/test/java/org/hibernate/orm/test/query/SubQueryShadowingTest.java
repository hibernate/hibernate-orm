/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


@DomainModel(
		annotatedClasses = {
				SubQueryShadowingTest.TestEntity.class,
		}
)
@SessionFactory
public class SubQueryShadowingTest {

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19745")
	public void testSelectCase(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery(
							"""
							from TestEntity t
							left join TestEntity t2 on exists (
								select 1
								from TestEntity t
								order by t.id
								limit 1
							)
							""", TestEntity.class )
					.list();
		} );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(String name) {
			this.name = name;
		}
	}
}
