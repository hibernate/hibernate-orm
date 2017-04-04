/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded.many2one;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class EmbeddableWithMany2OneTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Country.class };
	}

	@Test
	public void testJoinAcrossEmbedded() {
		Session session = openSession();
		session.beginTransaction();
		session.createQuery( "from Person p join p.address as a join a.country as c where c.name = 'US'" )
				.list();
		session.createQuery( "from Person p join p.address as a join a.country as c where c.id = 'US'" )
				.list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testBasicOps() {
		Session session = openSession();
		session.beginTransaction();
		Country country = new Country( "US", "United States of America" );
		session.persist( country );
		Person person = new Person( "Steve", new Address() );
		person.getAddress().setLine1( "123 Main" );
		person.getAddress().setCity( "Anywhere" );
		person.getAddress().setCountry( country );
		person.getAddress().setPostalCode( "123456789" );
		session.persist( person );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.createQuery( "from Person p where p.address.country.iso2 = 'US'" )
				.list();
		// same query!
		session.createQuery( "from Person p where p.address.country.id = 'US'" )
				.list();
		person = (Person) session.load( Person.class, person.getId() );
		session.delete( person );
		List countries = session.createQuery( "from Country" ).list();
		assertEquals( 1, countries.size() );
		session.delete( countries.get( 0 ) );

		session.getTransaction().commit();
		session.close();
	}
}
