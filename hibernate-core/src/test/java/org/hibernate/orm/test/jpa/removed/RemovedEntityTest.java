/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.removed;

import java.math.BigDecimal;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.hibernate.orm.test.jpa.model.Item;
import org.hibernate.orm.test.jpa.model.Part;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
public class RemovedEntityTest extends AbstractJPATest {

	@Test
	public void testRemoveThenContains() {
		Item item = new Item();
		inTransaction(
				session -> {
					item.setName( "dummy" );
					session.persist( item );
				}
		);

		boolean contains = fromTransaction(
				session -> {
					Item reference = session.getReference( item );
					session.remove( reference );
					return session.contains( reference );
				}
		);

		assertFalse( contains, "expecting removed entity to not be contained" );
	}

	@Test
	public void testRemoveThenGet() {
		Item it = new Item();
		inTransaction(
				session -> {
					it.setName( "dummy" );
					session.persist( it );
				}
		);

		Long id = it.getId();

		Item item = fromTransaction(
				session -> {
					session.remove( session.find( Item.class, id ) );
					return session.find( Item.class, id );
				}
		);

		assertNull( item, "expecting removed entity to be returned as null from get()" );
	}

	@Test
	public void testRemoveThenSave() {
		Item it = new Item();
		inTransaction(
				session -> {
					it.setName( "dummy" );
					session.persist( it );
				}
		);

		Long id = it.getId();

		inTransaction(
				session -> {
					Item item = session.find( Item.class, id );
					String sessionAsString = session.toString();

					session.remove( item );

					Item item2 = session.find( Item.class, id );
					assertNull( item2, "expecting removed entity to be returned as null from get()" );

					session.persist( item );
					assertEquals(  sessionAsString, session.toString(), "expecting session to be as it was before" );

					item.setName( "Rescued" );
					item = session.find( Item.class, id );
					assertNotNull( item, "expecting rescued entity to be returned from get()" );
				}
		);

		Item item = fromTransaction(
				session ->
						session.find( Item.class, id )
		);

		assertNotNull( item, "expecting removed entity to be returned as null from get()" );
		assertEquals( "Rescued", item.getName() );

		// clean up
		inTransaction(
				session ->
						session.remove( session.getReference(item) )
		);
	}

	@Test
	public void testRemoveThenSaveWithCascades() {
		Item item = new Item();
		inTransaction(
				session -> {
					item.setName( "dummy" );
					Part part = new Part( item, "child", "1234", BigDecimal.ONE );

					// persist cascades to part
					session.persist( item );

					// delete cascades to part also
					session.remove( item );
					assertFalse( session.contains( item ), "the item is contained in the session after deletion" );
					assertFalse( session.contains( part ), "the part is contained in the session after deletion" );

					// now try to persist again as a "unschedule removal" operation
					session.persist( item );
					assertTrue( session.contains( item ), "the item is contained in the session after deletion" );
					assertTrue( session.contains( part ), "the part is contained in the session after deletion" );
				}
		);

		// clean up
		inTransaction(
				session ->
						session.remove( session.getReference(item) )
		);
	}

	@Test
	public void testRemoveChildThenFlushWithCascadePersist() {

		Item item = new Item();
		inTransaction(
				session -> {
					item.setName( "dummy" );
					Part child = new Part( item, "child", "1234", BigDecimal.ONE );

					// persist cascades to part
					session.persist( item );

					// delete the part
					session.remove( child );
					assertFalse(
							session.contains( child ),
							"the child is contained in the session, since it is deleted"
					);

					// now try to flush, which will attempt to cascade persist again to child.
					session.flush();
					assertTrue(
							session.contains( child ),
							"Now it is consistent again since if was cascade-persisted by the flush()"
					);
				}
		);

		// clean up
		inTransaction(
				session ->
						session.remove( session.getReference(item) )
		);
	}
}
