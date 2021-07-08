/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ordered;

import java.util.Arrays;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = HqlOrderByIdsTest.Person.class
)
public class HqlOrderByIdsTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
						entityManager.createQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10502")
	@RequiresDialect(value = MySQLDialect.class, version = 500)
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person1 = new Person();
			person1.setId( 1L );
			person1.setName( "John" );

			Person person2 = new Person();
			person2.setId( 2L );
			person2.setName( "Doe" );

			Person person3 = new Person();
			person3.setId( 3L );
			person3.setName( "J" );

			Person person4 = new Person();
			person4.setId( 4L );
			person4.setName( "D" );

			entityManager.persist( person1 );
			entityManager.persist( person2 );
			entityManager.persist( person3 );
			entityManager.persist( person4 );
		} );

		scope.inTransaction( entityManager -> {
			List<Person> persons = entityManager.createQuery(
					"SELECT p " +
							"FROM Person p " +
							"WHERE p.id IN (:ids) " +
							"ORDER BY FIELD(id, :ids) ", Person.class )
					.setParameter( "ids", Arrays.asList( 3L, 1L, 2L ) )
					.getResultList();

			assertEquals( 3, persons.size() );
			int index = 0;
			assertEquals( 3L, persons.get( index++ ).getId() );
			assertEquals( 1L, persons.get( index++ ).getId() );
			assertEquals( 2L, persons.get( index++ ).getId() );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

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
