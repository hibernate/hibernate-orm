/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement.embeddables.withcustomenumdef;

import java.util.Iterator;

import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				Query.class
		}
)
@SessionFactory
public class TestBasicOps {

	@Test
	public void testLoadAndStore(SessionFactoryScope scope) {
		Query q = new Query( new Location( "first", Location.Type.COUNTY ) );
		scope.inTransaction(
				session -> {
					session.save( q );
				}
		);

		scope.inTransaction(
				session -> {
					Query q1 = session.get( Query.class, q.getId() );
					assertEquals( 1, q1.getIncludedLocations().size() );
					Location l = q1.getIncludedLocations().iterator().next();
					assertEquals( Location.Type.COUNTY, l.getType() );
					session.delete( q1 );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7072")
	public void testEmbeddableWithNullables(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction transaction = session.beginTransaction();
					try {
						Query q = new Query( new Location( null, Location.Type.COMMUNE ) );
						session.save( q );
						transaction.commit();
						session.clear();

						transaction = session.beginTransaction();
						q.getIncludedLocations().add( new Location( null, Location.Type.COUNTY ) );
						session.update( q );
						transaction.commit();
						session.clear();

						transaction = session.beginTransaction();
						q = (Query) session.get( Query.class, q.getId() );
//		assertEquals( 2, q.getIncludedLocations().size() );
						transaction.commit();
						session.clear();

						transaction = session.beginTransaction();
						Iterator<Location> itr = q.getIncludedLocations().iterator();
						itr.next();
						itr.remove();
						session.update( q );
						transaction.commit();
						session.clear();

						transaction = session.beginTransaction();
						q = (Query) session.get( Query.class, q.getId() );
						assertEquals( 1, q.getIncludedLocations().size() );
						session.delete( q );
						transaction.commit();
					}
					catch (Exception e) {
						if ( transaction.isActive() ) {
							transaction.rollback();
						}
						throw e;
					}
				}
		);
	}
}
