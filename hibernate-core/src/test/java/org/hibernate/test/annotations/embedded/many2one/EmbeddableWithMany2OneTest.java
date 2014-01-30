/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.embedded.many2one;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel
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
