/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DomainModel(
		annotatedClasses = {
				Products.class,
				Widgets.class,
				Widgets.Widget1.class,
				Widgets.Widget2.class,
				BugSystem.class
		}
)
@SessionFactory
public class OrderByTest {

	@Test
	public void testOrderByName(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Products p = new Products();
					HashSet<Widgets> set = new HashSet<>();

					Widgets widget = new Widgets();
					widget.setName( "hammer" );
					set.add( widget );
					session.persist( widget );

					widget = new Widgets();
					widget.setName( "axel" );
					set.add( widget );
					session.persist( widget );

					widget = new Widgets();
					widget.setName( "screwdriver" );
					set.add( widget );
					session.persist( widget );

					p.setWidgets( set );
					session.persist( p );
					session.getTransaction().commit();

					session.beginTransaction();
					session.clear();
					p = session.get( Products.class, p.getId() );
					assertTrue( p.getWidgets().size() == 3, "has three Widgets" );
					Iterator iter = p.getWidgets().iterator();
					assertEquals( "axel", ( (Widgets) iter.next() ).getName() );
					assertEquals( "hammer", ( (Widgets) iter.next() ).getName() );
					assertEquals( "screwdriver", ( (Widgets) iter.next() ).getName() );
				}
		);
	}

	@Test
	public void testOrderByWithDottedNotation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					BugSystem bs = new BugSystem();
					HashSet<Bug> set = new HashSet<>();

					Bug bug = new Bug();
					bug.setDescription( "JPA-2 locking" );
					bug.setSummary( "JPA-2 impl locking" );
					Person p = new Person();
					p.setFirstName( "Scott" );
					p.setLastName( "Marlow" );
					bug.setReportedBy( p );
					set.add( bug );

					bug = new Bug();
					bug.setDescription( "JPA-2 annotations" );
					bug.setSummary( "JPA-2 impl annotations" );
					p = new Person();
					p.setFirstName( "Emmanuel" );
					p.setLastName( "Bernard" );
					bug.setReportedBy( p );
					set.add( bug );

					bug = new Bug();
					bug.setDescription( "Implement JPA-2 criteria" );
					bug.setSummary( "JPA-2 impl criteria" );
					p = new Person();
					p.setFirstName( "Steve" );
					p.setLastName( "Ebersole" );
					bug.setReportedBy( p );
					set.add( bug );

					bs.setBugs( set );
					session.persist( bs );
					session.getTransaction().commit();

					session.beginTransaction();
					session.clear();
					bs = session.get( BugSystem.class, bs.getId() );
					assertTrue( bs.getBugs().size() == 3, "has three bugs" );
					Iterator iter = bs.getBugs().iterator();
					assertEquals( "Emmanuel", ( (Bug) iter.next() ).getReportedBy().getFirstName() );
					assertEquals( "Steve", ( (Bug) iter.next() ).getReportedBy().getFirstName() );
					assertEquals( "Scott", ( (Bug) iter.next() ).getReportedBy().getFirstName() );
				}
		);

	}
}
