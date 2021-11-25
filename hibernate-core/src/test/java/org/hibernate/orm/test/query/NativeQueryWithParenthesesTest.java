/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = NativeQueryWithParenthesesTest.Person.class
)
@SessionFactory
public class NativeQueryWithParenthesesTest {

	@Test
	public void testParseParentheses(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
						entityManager.createNativeQuery(
								"(SELECT p.id, p.name FROM Person p WHERE p.name LIKE 'A%') " +
										"UNION " +
										"(SELECT p.id, p.name FROM Person p WHERE p.name LIKE 'B%')",
								Person.class
						).getResultList()
		);
	}

	@Entity
	@Table(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
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
