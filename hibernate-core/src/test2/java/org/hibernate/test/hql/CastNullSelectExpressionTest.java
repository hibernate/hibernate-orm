/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-10757")
public class CastNullSelectExpressionTest extends BaseCoreFunctionalTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-10757")
	public void testSelectCastNull() {
		Session s = openSession();
		s.getTransaction().begin();
		Person person = new Person();
		person.firstName = "Herman";
		person.middleName = "Joseph";
		person.lastName = "Munster";
		s.persist( person );
		s.flush();
		s.clear();

		Object[] result = (Object[]) s.createQuery(
				"select firstName, cast( null as string ), lastName from CastNullSelectExpressionTest$Person where lastName='Munster'"
		).uniqueResult();

		assertEquals( 3, result.length );
		assertEquals( "Herman", result[0] );
		assertNull( result[1] );
		assertEquals( "Munster", result[2] );

		s.getTransaction().rollback();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10757")
	public void testSelectNewCastNull() {
		Session s = openSession();
		s.getTransaction().begin();
		Person person = new Person();
		person.firstName = "Herman";
		person.middleName = "Joseph";
		person.lastName = "Munster";
		s.persist( person );
		s.flush();
		s.clear();

		Person result = (Person) s.createQuery(
				"select new CastNullSelectExpressionTest$Person( id, firstName, cast( null as string ), lastName ) from CastNullSelectExpressionTest$Person where lastName='Munster'"
		).uniqueResult();
		assertEquals( "Herman", result.firstName );
		assertNull( result.middleName );
		assertEquals( "Munster", result.lastName );

		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Entity
	@Table(name = "PERSON")
	private static class Person {
		@Id
		@GeneratedValue
		private long id;

		private String firstName;

		private String middleName;

		private String lastName;

		private Integer age;

		Person() {
		}

		public Person(long id, String firstName, String middleName, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.middleName = middleName;
			this.lastName = lastName;
		}

		public Person(long id, String firstName, Integer age, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.middleName = null;
			this.lastName = lastName;
			this.age = age;
		}

	}
}
