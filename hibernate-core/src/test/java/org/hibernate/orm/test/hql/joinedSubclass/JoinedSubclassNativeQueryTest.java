/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql.joinedSubclass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = {
				JoinedSubclassNativeQueryTest.Person.class
		}
)
@SessionFactory
public class JoinedSubclassNativeQueryTest {

	@BeforeAll
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person();
					p.setFirstName( "Jan" );
					session.persist( p );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createMutationQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-16180")
	public void testJoinedInheritanceNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = (Person) session.createNativeQuery( "select p.*, 0 as clazz_ from Person p", Person.class ).getSingleResult();
					Assertions.assertNotNull( p );
					Assertions.assertEquals( p.getFirstName(), "Jan" );
				}
		);
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		@Basic(optional = false)
		private String firstName;

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
	}
}
