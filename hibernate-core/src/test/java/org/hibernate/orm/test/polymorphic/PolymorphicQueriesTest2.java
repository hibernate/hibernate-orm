/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.polymorphic;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = PolymorphicQueriesTest2.Human.class
)
@SessionFactory
public class PolymorphicQueriesTest2 {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Human( "Fab" ) );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15744")
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<Animal> query = session.createQuery(
							"from org.hibernate.orm.test.polymorphic.PolymorphicQueriesTest2$Animal u where (u.name = ?1)",
							Animal.class
					);
					query.setParameter( 1, "Fab" );
					query.list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-15850")
	public void testQueryLike(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( "Fab", session.createQuery(
					"from org.hibernate.orm.test.polymorphic.PolymorphicQueriesTest2$Animal u where (u.name like ?1)",
					Animal.class
			).setParameter( 1, "F%" ).getSingleResult().getName() );
		} );
	}

	public interface Animal {
		String getName();
	}

	@Entity(name = "Human")
	public static class Human implements Animal {
		@Id
		@GeneratedValue
		private Long id;
		private String name;

		public Human() {
		}

		public Human(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}
	}
}
