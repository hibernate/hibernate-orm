/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7690")
public class RelationInsideEmbeddableTest extends BaseEnversJPAFunctionalTestCase {
	private Integer orderId = null;
	private ItemId itemId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PurchaseOrder.class, Item.class, ItemId.class, Producer.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		Producer producer = new Producer( 1, "Sony" );
		ItemId sonyId = new ItemId( "TV", 1, producer );
		Item item = new Item( sonyId, 100.50 );
		PurchaseOrder order = new PurchaseOrder( item, null );
		em.persist( producer );
		em.persist( item );
		em.persist( order );
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		order = em.find( PurchaseOrder.class, order.getId() );
		order.setComment( "fragile" );
		order = em.merge( order );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();
		item = em.find( Item.class, sonyId );
		item.setPrice( 110.00 );
		em.getTransaction().commit();

		orderId = order.getId();
		itemId = sonyId;

		em.close();
	}

	@Test
	public void testRevisionsCounts() throws Exception {
		Assert.assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( PurchaseOrder.class, orderId ) );
		Assert.assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( Item.class, itemId ) );
	}

	@Test
	public void testHistoryOfPurchaseOrder() {
		PurchaseOrder ver1 = new PurchaseOrder(
				orderId, new Item(
				new ItemId( "TV", 1, new Producer( 1, "Sony" ) ),
				100.50
		), null
		);
		PurchaseOrder ver2 = new PurchaseOrder(
				orderId, new Item(
				new ItemId( "TV", 1, new Producer( 1, "Sony" ) ),
				100.50
		), "fragile"
		);

		Assert.assertEquals( ver1, getAuditReader().find( PurchaseOrder.class, orderId, 1 ) );
		Assert.assertEquals( ver2, getAuditReader().find( PurchaseOrder.class, orderId, 2 ) );
	}

	@Test
	public void testHistoryOfItem() {
		Item ver1 = new Item( itemId, 100.50 );
		Item ver2 = new Item( itemId, 110.00 );

		Assert.assertEquals( ver1, getAuditReader().find( Item.class, itemId, 1 ) );
		Assert.assertEquals( ver2, getAuditReader().find( Item.class, itemId, 3 ) );
	}
}
