/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.query;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import java.util.List;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;

/**
 * @author Oleksandr Dukhno
 */
public class AggregateFunctionsTest extends BaseEntityManagerFunctionalTestCase {

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Document.class,
				Person.class
		};
	}

	@Before
	public void init() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Person p1 = new Person();
		Person p2 = new Person();
		Document d = new Document();

		p1.getLocalized().put(1, "p1.1");
		p1.getLocalized().put(2, "p1.2");
		p2.getLocalized().put(1, "p2.1");
		p2.getLocalized().put(2, "p2.2");

		d.getContacts().put(1, p1);
		d.getContacts().put(2, p2);

		em.persist(p1);
		em.persist(p2);
		em.persist(d);

		em.flush();
		tx.commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9331")
	public void testSum() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List l = em.createQuery(
				"SELECT d.id, " +
						"SUM((SELECT COUNT(localized) " +
						"		FROM Person p " +
						"			LEFT JOIN p.localized localized " +
						"		WHERE p.id = c.id)) AS localizedCount " +
						"FROM Document d " +
						"	LEFT JOIN d.contacts c " +
						"GROUP BY d.id").getResultList();
		em.getTransaction().commit();
		em.close();
		assertEquals( 2, l.size() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9331")
	public void testMin() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List l = em.createQuery(
				"SELECT d.id, " +
						"MIN((SELECT COUNT(localized) " +
						"		FROM Person p " +
						"			LEFT JOIN p.localized localized " +
						"		WHERE p.id = c.id)) AS localizedCount " +
						"FROM Document d " +
						"	LEFT JOIN d.contacts c " +
						"GROUP BY d.id").getResultList();
		em.getTransaction().commit();
		em.close();
		assertEquals( 2, l.size() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9331")
	public void testMax() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List l = em.createQuery(
				"SELECT d.id, " +
						"MAX((SELECT COUNT(localized) " +
						"		FROM Person p " +
						"			LEFT JOIN p.localized localized " +
						"		WHERE p.id = c.id)) AS localizedCount " +
						"FROM Document d " +
						"	LEFT JOIN d.contacts c " +
						"GROUP BY d.id").getResultList();
		em.getTransaction().commit();
		em.close();
		assertEquals( 2, l.size() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9331")
	public void testAvg() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List l = em.createQuery(
				"SELECT d.id, " +
						"AVG((SELECT COUNT(localized) " +
						"		FROM Person p " +
						"			LEFT JOIN p.localized localized " +
						"		WHERE p.id = c.id)) AS localizedCount " +
						"FROM Document d " +
						"	LEFT JOIN d.contacts c " +
						"GROUP BY d.id").getResultList();
		em.getTransaction().commit();
		em.close();
		assertEquals( 2, l.size() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9331")
	public void testCount() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List l = em.createQuery(
				"SELECT d.id, " +
						"COUNT((SELECT COUNT(localized) " +
						"		FROM Person p " +
						"			LEFT JOIN p.localized localized " +
						"		WHERE p.id = c.id)) AS localizedCount " +
						"FROM Document d " +
						"	LEFT JOIN d.contacts c " +
						"GROUP BY d.id").getResultList();
		em.getTransaction().commit();
		em.close();
		assertEquals( 2, l.size() );
	}

}
