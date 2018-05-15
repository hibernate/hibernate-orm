/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.generics;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

public class EmbeddedGenericsTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testWorksWithGenericEmbedded() {
		Session session = openSession();
		session.beginTransaction();
		Classes.Edition<String> edition = new Classes.Edition<String>();
		edition.name = "Second";
		Classes.Book b = new Classes.Book();
		b.edition = edition;
		session.persist( b );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Classes.Book retrieved = (Classes.Book) session.get( Classes.Book.class, b.id );
		assertEquals( "Second", retrieved.edition.name );
		session.delete( retrieved );
		session.getTransaction().commit();
		session.close();
	}

	public void testWorksWithGenericCollectionOfElements() {
		Session session = openSession();
		session.beginTransaction();
		Classes.Edition<String> edition = new Classes.Edition<String>();
		edition.name = "Second";
		Classes.PopularBook b = new Classes.PopularBook();
		b.editions.add( edition );
		session.persist( b );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Classes.PopularBook retrieved = (Classes.PopularBook) session.get( Classes.PopularBook.class, b.id );
		assertEquals( "Second", retrieved.editions.iterator().next().name );
		session.delete( retrieved );
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Classes.Book.class,
				Classes.PopularBook.class
		};
	}
}
