/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded.one2many;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class EmbeddableWithOne2ManyTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
//		return new Class[] { Alias.class, Person.class };
		return new Class[] {  };
	}

	@Test
	@FailureExpected( jiraKey = "HHH-4883")
	public void testJoinAcrossEmbedded() {
		// NOTE : this may or may not work now with HHH-4883 fixed,
		// but i cannot do this checking until HHH-4599 is done.
		Session session = openSession();
		session.beginTransaction();
		session.createQuery( "from Person p join p.name.aliases a where a.source = 'FBI'" )
				.list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-4599")
	public void testBasicOps() {
		Session session = openSession();
		session.beginTransaction();
		Alias alias = new Alias( "Public Enemy", "Number 1", "FBI" );
		session.persist( alias );
		Person person = new Person( "John", "Dillinger" );
		person.getName().getAliases().add( alias );
		session.persist( person );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		person = (Person) session.load( Person.class, person.getId() );
		session.delete( person );
		List aliases = session.createQuery( "from Alias" ).list();
		assertEquals( 0, aliases.size() );
		session.getTransaction().commit();
		session.close();
	}
}
