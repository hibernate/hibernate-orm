/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomanyassociationclass.nestedreference;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = { "org/hibernate/orm/test/manytomanyassociationclass/nestedreference/Item.hbm.xml" }
)
@SessionFactory
public class ItemSelfReferenceTest {
	@Test
	public void testSimpleCreateAndDelete(SessionFactoryScope scope) {
		Item item = new Item( "turin", "tiger" );
		scope.inTransaction(
				session -> {
					session.persist( item );
				}
		);

		scope.inTransaction(
				session -> {
					Item innerItem = session.getReference(Item.class, item.getId());
					session.remove(innerItem);
				}
		);
	}
}
