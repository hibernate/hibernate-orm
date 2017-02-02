/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.basic2;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Tests related to specifying joins on components (embedded values).
 *
 * @author Steve Ebersole
 */
public class ComponentJoinsTest extends BaseCoreFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
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
