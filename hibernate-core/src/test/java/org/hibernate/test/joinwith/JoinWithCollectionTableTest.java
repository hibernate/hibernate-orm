/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.joinwith;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

public class JoinWithCollectionTableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class, Person.class };
	}
	
	private void insertData() {
		// Drop and create table like this since cleanupTestData() does not work
		buildSessionFactory();
		// Insert a document with two contacts
		Session session = openSession();
		session.beginTransaction();

		Person p1 = new Person();
		Person p2 = new Person();
		Document d = new Document();

		d.getContacts().put( 1, p1 );
		d.getContacts().put( 2, p2 );

		session.persist( p1 );
		session.persist( p2 );
		session.persist( d );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9329")
	public void testJoinCollectionTableWithClause() {
		insertData();
		Session session = openSession();
		session.beginTransaction();
		final String qry = "SELECT d.id FROM Document d LEFT JOIN d.contacts c WITH KEY(c) = 1";

		List l = session.createQuery( qry ).list();

		session.getTransaction().commit();
		session.close();

		Assert.assertEquals( 1, l.size() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9329")
	public void testJoinCollectionTableWithClauseInSubquery() {
		insertData();
		Session session = openSession();
		session.beginTransaction();
		final String qry = "SELECT (SELECT d.id FROM Document d LEFT JOIN d.contacts c WITH KEY(c) = 1 WHERE d.id = doc.id) FROM Document doc";
		List l = session.createQuery( qry ).list();

		session.getTransaction().commit();
		session.close();

		Assert.assertEquals( 1, l.size() );
	}

}
