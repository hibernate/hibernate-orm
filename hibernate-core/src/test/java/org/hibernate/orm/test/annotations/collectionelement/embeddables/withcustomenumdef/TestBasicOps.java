/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.embeddables.withcustomenumdef;

import java.util.Iterator;

import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.JiraKey;
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
				session ->
						session.persist( q )
		);

		scope.inTransaction(
				session -> {
					Query q1 = session.get( Query.class, q.getId() );
					assertEquals( 1, q1.getIncludedLocations().size() );
					Location l = q1.getIncludedLocations().iterator().next();
					assertEquals( Location.Type.COUNTY, l.getType() );
					session.remove( q1 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-7072")
	public void testEmbeddableWithNullables(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = new Query( new Location( null, Location.Type.COMMUNE ) );
					session.persist( q );
					session.getTransaction().commit();
					session.clear();

					Transaction transaction = session.beginTransaction();
					q.getIncludedLocations().add( new Location( null, Location.Type.COUNTY ) );
					session.merge( q );
					transaction.commit();
					session.clear();

					transaction = session.beginTransaction();
					q = session.get( Query.class, q.getId() );
//		assertEquals( 2, q.getIncludedLocations().size() );
					transaction.commit();
					session.clear();

					transaction = session.beginTransaction();
					Iterator<Location> itr = q.getIncludedLocations().iterator();
					itr.next();
					itr.remove();
					session.merge( q );
					transaction.commit();
					session.clear();

					session.beginTransaction();
					q = session.get( Query.class, q.getId() );
					assertEquals( 1, q.getIncludedLocations().size() );
					session.remove( q );
				}
		);
	}
}
