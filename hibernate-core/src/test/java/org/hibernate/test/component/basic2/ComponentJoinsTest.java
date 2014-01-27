/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
 * Boston, MA  02110-1301  USA\
 */
package org.hibernate.test.component.basic2;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * Tests related to specifying joins on components (embedded values).
 *
 * @author Steve Ebersole
 */
public class ComponentJoinsTest extends BaseCoreFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Name.class,
				Person.class,
				Component.class,
				Component.Emb.Stuff.class };
	}

	@Test
	public void testComponentJoins() {
		// Just checking proper query construction and syntax checking via database query parser...
		Session session = openSession();
		session.beginTransaction();
		// use it in WHERE
		session.createQuery( "select p from Person p join p.name as n where n.lastName like '%'" ).list();
		// use it in SELECT
		session.createQuery( "select n.lastName from Person p join p.name as n" ).list();
		session.createQuery( "select n from Person p join p.name as n" ).list();
		// use it in ORDER BY
		session.createQuery( "select n from Person p join p.name as n order by n.lastName" ).list();
		session.createQuery( "select n from Person p join p.name as n order by p" ).list();
		session.createQuery( "select n from Person p join p.name as n order by n" ).list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7849")
	public void testComponentJoinsHHH7849() {
		// Just checking proper query construction and syntax checking via database query parser...
		Session session = openSession();
		session.beginTransaction();
		// use it in WHERE
		session.createQuery( "select c from Component c join c.emb as e where e.stuffs is empty " ).list();

		session.getTransaction().commit();
		session.close();
	}
}
