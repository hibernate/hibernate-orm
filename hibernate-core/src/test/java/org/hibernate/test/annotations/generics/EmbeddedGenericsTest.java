/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.generics;

import org.hibernate.Session;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@FailureExpectedWithNewMetamodel( jiraKey = "HHH-9049" )
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
				Classes.PopularBook.class,
				Classes.Edition.class
		};
	}
}
