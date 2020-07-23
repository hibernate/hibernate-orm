/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.Transaction;
import org.hibernate.dialect.TeradataDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DomainModel(
		annotatedClasses = {
				Products.class,
				Widgets.class,
				BugSystem.class
		}
)
@SessionFactory
public class OrderByTest {

	@Test
	public void testOrderByName(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {

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
						tx.commit();

						tx = session.beginTransaction();
						session.clear();
						p = session.get( Products.class, p.getId() );
						assertTrue( p.getWidgets().size() == 3, "has three Widgets" );
						Iterator iter = p.getWidgets().iterator();
						assertEquals( "axel", ( (Widgets) iter.next() ).getName() );
						assertEquals( "hammer", ( (Widgets) iter.next() ).getName() );
						assertEquals( "screwdriver", ( (Widgets) iter.next() ).getName() );
						tx.commit();
					}
					catch (Exception e) {
						if ( tx.isActive() ) {
							tx.rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	@SkipForDialect(
			dialectClass = TeradataDialect.class,
			matchSubTypes = true,
			reason = "HHH-8190, uses Teradata reserved word - summary"
	)
	public void testOrderByWithDottedNotation(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {
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
						tx.commit();

						tx = session.beginTransaction();
						session.clear();
						bs = session.get( BugSystem.class, bs.getId() );
						assertTrue( bs.getBugs().size() == 3, "has three bugs" );
						Iterator iter = bs.getBugs().iterator();
						assertEquals( "Emmanuel", ( (Bug) iter.next() ).getReportedBy().getFirstName() );
						assertEquals( "Steve", ( (Bug) iter.next() ).getReportedBy().getFirstName() );
						assertEquals( "Scott", ( (Bug) iter.next() ).getReportedBy().getFirstName() );
						tx.commit();
					}
					catch (Exception e) {
						if ( tx.isActive() ) {
							tx.rollback();
						}
						throw e;
					}
				}
		);

	}
}
