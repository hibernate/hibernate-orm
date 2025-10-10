/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.subselect;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sharath Reddy
 */
@DomainModel(
		annotatedClasses = {
				Item.class,
				Bid.class,
				HighestBid.class
		}
)
@SessionFactory
public class SubselectTest {

	@Test
	public void testSubselectWithSynchronize(SessionFactoryScope scope) {
		//We don't use auto-generated ids because these seem to cause the session to flush.
		//We want to test that the session flushes because of the 'synchronize' annotation
		scope.inTransaction(
				session -> {
					long itemId = 1;
					Item item = new Item();
					item.setName( "widget" );
					item.setId( itemId );
					session.persist( item );

					Bid bid1 = new Bid();
					bid1.setAmount( 100.0 );
					bid1.setItemId( itemId );
					bid1.setId( 1 );
					session.persist( bid1 );

					Bid bid2 = new Bid();
					bid2.setAmount( 200.0 );
					bid2.setItemId( itemId );
					bid2.setId( 2 );
					session.persist( bid2 );

					//Because we use 'synchronize' annotation, this query should trigger session flush
					var query = session.createQuery( "from HighestBid b where b.name = :name", HighestBid.class );
					query.setParameter( "name", "widget", StandardBasicTypes.STRING );
					HighestBid highestBid = query.list().iterator().next();

					assertEquals( 200.0, highestBid.getAmount(), 0.01 );
				}
		);
	}

}
