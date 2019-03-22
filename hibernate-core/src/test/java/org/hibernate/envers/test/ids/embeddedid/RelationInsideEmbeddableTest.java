/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.ids.embeddedid;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.embeddedid.Item;
import org.hibernate.envers.test.support.domains.ids.embeddedid.ItemId;
import org.hibernate.envers.test.support.domains.ids.embeddedid.Producer;
import org.hibernate.envers.test.support.domains.ids.embeddedid.PurchaseOrder;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7690")
@Disabled("NYI - SingularPersistentAttributeEntity#visitJdbcTypes")
public class RelationInsideEmbeddableTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer orderId = null;
	private ItemId itemId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PurchaseOrder.class, Item.class, ItemId.class, Producer.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					Producer producer = new Producer( 1, "Sony" );
					ItemId sonyId = new ItemId( "TV", 1, producer );
					Item item = new Item( sonyId, 100.50 );
					PurchaseOrder order = new PurchaseOrder( item, null );
					entityManager.persist( producer );
					entityManager.persist( item );
					entityManager.persist( order );

					orderId = order.getId();
					itemId = sonyId;
				},

				// Revision 2
				entityManager -> {
					PurchaseOrder order = entityManager.find( PurchaseOrder.class, orderId );
					order.setComment( "fragile" );
					entityManager.merge( order );
				},

				// Revision 3
				entityManager -> {
					Item item = entityManager.find( Item.class, itemId );
					item.setPrice( 110.00 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() throws Exception {
		assertThat( getAuditReader().getRevisions( PurchaseOrder.class, orderId ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( Item.class, itemId ), contains( 1, 3  ) );
	}

	@DynamicTest
	public void testHistoryOfPurchaseOrder() {
		final Item item = new Item( new ItemId( "TV", 1, new Producer( 1, "Sony" ) ), 100.50 );

		final PurchaseOrder ver1 = new PurchaseOrder( orderId, item, null );
		final PurchaseOrder ver2 = new PurchaseOrder( orderId, item, "fragile" );

		assertThat( getAuditReader().find( PurchaseOrder.class, orderId, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( PurchaseOrder.class, orderId, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfItem() {
		Item ver1 = new Item( itemId, 100.50 );
		Item ver2 = new Item( itemId, 110.00 );

		assertThat( getAuditReader().find( Item.class, itemId, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( Item.class, itemId, 3 ), equalTo( ver2 ) );
	}
}
