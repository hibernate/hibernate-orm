/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.basic2;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.JiraKey;
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
	@JiraKey(value = "HHH-7849")
	public void testComponentJoinsHHH7849() {
		// Just checking proper query construction and syntax checking via database query parser...
		Session session = openSession();
		session.beginTransaction();
		// use it in WHERE
		session.createQuery( "select c from Component c join c.emb as e where e.stuffs is empty " ).list();

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void dummy() {
		Session session = openSession();
		session.beginTransaction();
		session.persist( new Person( 1, "Steve", "Ebersone" ) );
		session.getTransaction().commit();
		session.close();


		session = openSession();
		session.beginTransaction();
		Person person = session.get( Person.class, 1 );
		person.getName().setLastName( "Ebersole" );
		session.getTransaction().commit();
		session.close();


		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete Person" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}
