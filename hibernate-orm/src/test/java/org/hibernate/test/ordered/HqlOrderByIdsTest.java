/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ordered;

import java.util.Arrays;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class HqlOrderByIdsTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10502" )
	@RequiresDialect( value = MySQL5Dialect.class )
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Person> persons = entityManager.createQuery(
				"SELECT p " +
				"FROM Person p " +
				"WHERE p.id IN (:ids) " +
				"ORDER BY FIELD(id, :ids) ", Person.class)
			.setParameter( "ids" , Arrays.asList(3L, 1L, 2L))
			.getResultList();

			assertEquals(3, persons.size());
			int index = 0;
			assertEquals( Long.valueOf( 3L ), persons.get( index++ ).getId() );
			assertEquals( Long.valueOf( 1L ), persons.get( index++ ).getId() );
			assertEquals( Long.valueOf( 2L ), persons.get( index++ ).getId() );
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
