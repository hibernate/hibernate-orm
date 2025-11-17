/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.ordered.joinedInheritence;

import org.hibernate.internal.util.collections.CollectionHelper;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
@DomainModel(
		annotatedClasses = {
				Animal.class, Lion.class, Tiger.class, Zoo.class
		}
)
@SessionFactory
public class OrderCollectionOfJoinedHierarchyTest {

	@Test
	public void testQuerySyntaxCheck(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.get( Zoo.class, 1L )
		);
	}

	@Test
	public void testOrdering(SessionFactoryScope scope) {
		Zoo zoo = new Zoo();
		Lion lion1 = new Lion();
		lion1.setWeight( 2 );
		Lion lion2 = new Lion();
		lion2.setWeight( 1 );
		zoo.getLions().add( lion1 );
		zoo.getLions().add( lion2 );
		zoo.getAnimalsById().add( lion1 );
		zoo.getAnimalsById().add( lion2 );

		Zoo zoo1 = scope.fromTransaction(
				session -> {
					session.persist( lion1 );
					session.persist( lion2 );
					session.persist( zoo );
					session.getTransaction().commit();
					session.clear();

					session.beginTransaction();
					Zoo z = session.get( Zoo.class, zoo.getId() );
					z.getLions().size();
					z.getTigers().size();
					z.getAnimalsById().size();
					return z;
				}
		);

		assertNotNull( zoo1 );
		assertTrue( CollectionHelper.isNotEmpty( zoo1.getLions() ) && zoo1.getLions().size() == 2 );
		assertTrue( CollectionHelper.isNotEmpty( zoo1.getAnimalsById() ) && zoo1.getAnimalsById().size() == 2 );
		assertEquals( zoo1.getLions().iterator().next().getId(), lion2.getId() );
		assertEquals( zoo1.getAnimalsById().iterator().next().getId(), lion1.getId() );
	}
}
