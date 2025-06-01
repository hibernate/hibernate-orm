/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.bag;

import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to operations on a PersistentBag.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/collection/bag/Mappings.xml")
@SessionFactory
public class PersistentBagTest {

	@Test
	public void testWriteMethodDirtying(SessionFactoryScope scope) {
		BagOwner parent = new BagOwner( "root" );
		BagOwner child = new BagOwner( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		BagOwner otherChild = new BagOwner( "c2" );

		scope.inTransaction(
				session -> {
					session.persist( parent );
					session.flush();
					// at this point, the list on parent has now been replaced with a PersistentBag...
					PersistentBag children = (PersistentBag) parent.getChildren();

					assertFalse( children.remove( otherChild ) );
					assertFalse( children.isDirty() );

					ArrayList otherCollection = new ArrayList();
					otherCollection.add( child );
					assertFalse( children.retainAll( otherCollection ) );
					assertFalse( children.isDirty() );

					otherCollection = new ArrayList();
					otherCollection.add( otherChild );
					assertFalse( children.removeAll( otherCollection ) );
					assertFalse( children.isDirty() );

					children.clear();
					session.remove( child );
					assertTrue( children.isDirty() );

					session.flush();

					children.clear();
					assertFalse( children.isDirty() );

					session.remove( parent );
				}
		);
	}

	@Test
	public void testMergePersistentEntityWithNewOneToManyElements(SessionFactoryScope scope) {
		Long orderId = scope.fromTransaction(
				session -> {
					Order order = new Order();

					session.persist( order );
					session.flush();
					return order.getId();
				}
		);


		scope.inTransaction(
				session -> {
					Order order = session.get( Order.class, orderId );
					Item item1 = new Item();
					item1.setName( "i1" );
					Item item2 = new Item();
					item2.setName( "i2" );
					order.addItem( item1 );
					order.addItem( item2 );
					order = session.merge( order );
				}
		);
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}
}
