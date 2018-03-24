/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.notfound;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.Session;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
public class NotFoundOneToOneNonInsertableNonUpdateableTest extends BaseCoreFunctionalTestCase {
	private static final int ID = 1;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				PersonInfo.class
		};
	}

	@Test
	public void testOneToOne() {

		Session session = openSession();
		session.getTransaction().begin();
		{
					Person person = new Person();
					person.id = ID;
					person.personInfo = new PersonInfo();
					person.personInfo.id = ID;
					session.persist( person );
		}
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		{
					session.delete( session.get( PersonInfo.class, ID ) );
		}
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		{
					Person person = session.get( Person.class, ID );
					assertNotNull( person );
					assertNull( person.personInfo );

					session.delete( person );
		}
		session.getTransaction().commit();
		session.close();
	}

	@Entity(name="Person")
	public static class Person {

		@Id
		private int id;

		@OneToOne(optional = true, cascade = CascadeType.ALL)
		@JoinColumn(
				name = "id",
				updatable = false,
				insertable = false
		)
		@NotFound(action = NotFoundAction.IGNORE)
		private PersonInfo personInfo;
	}

	@Entity(name = "PersonInfo")
	public static class PersonInfo {
		@Id
		private int id;

	}
}
