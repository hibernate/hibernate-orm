/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement.embeddables.withcustomenumdef;

import java.util.Iterator;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class TestBasicOps extends SessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Query.class };
	}

	@Test
	public void testLoadAndStore() {
		Query query = new Query( new Location( "first", Location.Type.COUNTY ) );
		inTransaction(
				session -> {
					session.save( query );
				}
		);


		inTransaction(
				session -> {
					Query q = session.get( Query.class, query.getId() );
					assertEquals( 1, q.getIncludedLocations().size() );
					Location l = q.getIncludedLocations().iterator().next();
					assertEquals( Location.Type.COUNTY, l.getType() );
					session.delete( q );
				}
		);

	}

	@Test
	@TestForIssue(jiraKey = "HHH-7072")
	public void testEmbeddableWithNullables() {
		inSession(
				session -> {
					Query q = new Query( new Location( null, Location.Type.COMMUNE ) );
					try {
						session.beginTransaction();
						session.save( q );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					session.clear();
					try {
						session.beginTransaction();
						q.getIncludedLocations().add( new Location( null, Location.Type.COUNTY ) );
						session.update( q );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
					session.clear();
					try {
						session.beginTransaction();
						q = session.get( Query.class, q.getId() );
//		assertEquals( 2, q.getIncludedLocations().size() );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
					session.clear();

					try {
						session.beginTransaction();
						Iterator<Location> itr = q.getIncludedLocations().iterator();
						itr.next();
						itr.remove();
						session.update( q );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
					session.clear();

					try {
						session.beginTransaction();
						q = session.get( Query.class, q.getId() );
						assertEquals( 1, q.getIncludedLocations().size() );
						session.delete( q );
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
}
