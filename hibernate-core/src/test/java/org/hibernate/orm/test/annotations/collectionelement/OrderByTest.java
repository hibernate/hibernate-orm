/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.dialect.TeradataDialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FailureExpected("Order by not yet implemented")
public class OrderByTest extends SessionFactoryBasedFunctionalTest {
	@Test
	public void testOrderByName() {
		inSession(
				session -> {
					Products p = new Products();
					try {
						session.getTransaction().begin();
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
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					try {
						session.beginTransaction();
						session.clear();
						p = session.get( Products.class, p.getId() );
						assertTrue( p.getWidgets().size() == 3, "has three Widgets" );
						Iterator iter = p.getWidgets().iterator();
						assertEquals( "axel", ( (Widgets) iter.next() ).getName() );
						assertEquals( "hammer", ( (Widgets) iter.next() ).getName() );
						assertEquals( "screwdriver", ( (Widgets) iter.next() ).getName() );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	@SkipForDialect(
			value = TeradataDialect.class,
			jiraKey = "HHH-8190",
			comment = "uses Teradata reserved word - summary"
	)
	public void testOrderByWithDottedNotation() {
		inSession(
				session -> {
					BugSystem bs = new BugSystem();
					try {
						session.getTransaction().begin();
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
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					try {
						session.beginTransaction();
						session.clear();
						bs = session.get( BugSystem.class, bs.getId() );
						assertTrue( bs.getBugs().size() == 3, "has three bugs" );
						Iterator iter = bs.getBugs().iterator();
						assertEquals( "Emmanuel", ( (Bug) iter.next() ).getReportedBy().getFirstName() );
						assertEquals( "Steve", ( (Bug) iter.next() ).getReportedBy().getFirstName() );
						assertEquals( "Scott", ( (Bug) iter.next() ).getReportedBy().getFirstName() );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Products.class,
				Widgets.class,
				BugSystem.class
		};
	}

}
