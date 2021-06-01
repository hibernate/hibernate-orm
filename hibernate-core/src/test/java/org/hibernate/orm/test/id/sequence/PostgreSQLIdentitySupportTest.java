/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id.sequence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Vlad Mhalcea
 */
@TestForIssue(jiraKey = "HHH-13202")
@RequiresDialect(value = PostgreSQLDialect.class)
@Jpa(
		annotatedClasses = PostgreSQLIdentitySupportTest.Role.class
)
public class PostgreSQLIdentitySupportTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Role _role = scope.fromTransaction( entityManager -> {
			Role role = new Role();

			entityManager.persist( role );

			return role;
		} );

		scope.inTransaction( entityManager -> {
			Role role = entityManager.find( Role.class, _role.getId() );
			assertNotNull( role );
		} );
	}

	@Entity(name = "Role")
	public static class Role {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	}

}
