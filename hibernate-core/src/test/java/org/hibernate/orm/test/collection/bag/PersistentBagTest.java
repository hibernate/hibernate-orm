/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.collection.spi.PersistentBag;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to operations on a PersistentBag.
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/collection/bag/Mappings.hbm.xml"
)
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
					order = (Order) session.merge( order );
				}
		);

		scope.inTransaction(
				session -> {
					Order order = session.get( Order.class, orderId );
					assertEquals( 2, order.getItems().size() );
					session.remove( order );
				}
		);
	}
}
