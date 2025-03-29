/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.modifiedflags;

import java.util.List;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.integration.inheritance.joined.notownedrelation.Address;
import org.hibernate.orm.test.envers.integration.inheritance.joined.notownedrelation.Contact;
import org.hibernate.orm.test.envers.integration.inheritance.joined.notownedrelation.PersonalContact;
import org.hibernate.orm.test.envers.integration.modifiedflags.AbstractModifiedFlagsEntityTest;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedNotOwnedBidirectional extends AbstractModifiedFlagsEntityTest {
	private Long pc_id;
	private Long a1_id;
	private Long a2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Address.class, Contact.class, PersonalContact.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		pc_id = 1l;
		a1_id = 10l;
		a2_id = 100l;

		// Rev 1
		em.getTransaction().begin();

		PersonalContact pc = new PersonalContact( pc_id, "e", "f" );

		Address a1 = new Address( a1_id, "a1" );
		a1.setContact( pc );

		em.persist( pc );
		em.persist( a1 );

		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();

		pc = em.find( PersonalContact.class, pc_id );

		Address a2 = new Address( a2_id, "a2" );
		a2.setContact( pc );

		em.persist( a2 );

		em.getTransaction().commit();
	}

	@Test
	public void testReferencedEntityHasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				PersonalContact.class, pc_id,
				"addresses"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( Address.class, a1_id, "contact" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( Address.class, a2_id, "contact" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );
	}
}
