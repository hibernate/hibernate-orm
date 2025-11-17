/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7690")
@EnversTest
@Jpa(annotatedClasses = {PurchaseOrder.class, Item.class, ItemId.class, Producer.class})
public class RelationInsideEmbeddableTest {
	private Integer orderId = null;
	private ItemId itemId = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			Producer producer = new Producer( 1, "Sony" );
			ItemId sonyId = new ItemId( "TV", 1, producer );
			Item item = new Item( sonyId, 100.50 );
			PurchaseOrder order = new PurchaseOrder( item, null );
			em.persist( producer );
			em.persist( item );
			em.persist( order );

			orderId = order.getId();
			itemId = sonyId;
		} );

		// Revision 2
		scope.inTransaction( em -> {
			PurchaseOrder order = em.find( PurchaseOrder.class, orderId );
			order.setComment( "fragile" );
			em.merge( order );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			Item item = em.find( Item.class, itemId );
			item.setPrice( 110.00 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( PurchaseOrder.class, orderId ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( Item.class, itemId ) );
		} );
	}

	@Test
	public void testHistoryOfPurchaseOrder(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

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

			assertEquals( ver1, auditReader.find( PurchaseOrder.class, orderId, 1 ) );
			assertEquals( ver2, auditReader.find( PurchaseOrder.class, orderId, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfItem(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			Item ver1 = new Item( itemId, 100.50 );
			Item ver2 = new Item( itemId, 110.00 );

			assertEquals( ver1, auditReader.find( Item.class, itemId, 1 ) );
			assertEquals( ver2, auditReader.find( Item.class, itemId, 3 ) );
		} );
	}
}
