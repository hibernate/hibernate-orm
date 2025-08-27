/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.basic2;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Tests related to specifying joins on components (embedded values).
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Person.class, Component.class, Component.Emb.Stuff.class})
@SessionFactory
public class ComponentJoinsTest {

	@Test
	public void testComponentJoins(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			// use it in WHERE
			session.createQuery( "select p from Person p join p.name as n where n.lastName like '%'" ).list();
			session.createQuery( "select c from Component c join c.emb as e where e.stuffs is empty " ).list();

			// use it in SELECT
			session.createQuery( "select n.lastName from Person p join p.name as n" ).list();
			session.createQuery( "select n from Person p join p.name as n" ).list();

			// use it in ORDER BY
			session.createQuery( "select n from Person p join p.name as n order by n.lastName" ).list();
			session.createQuery( "select n from Person p join p.name as n order by p" ).list();
			session.createQuery( "select n from Person p join p.name as n order by n" ).list();
		} );
	}
}
