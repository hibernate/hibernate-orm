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
