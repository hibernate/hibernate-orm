/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.inheritance.tableperclass.notownedrelation;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotOwnedBidirectional extends BaseEnversJPAFunctionalTestCase {
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
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( Contact.class, pc_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( PersonalContact.class, pc_id ) );

		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( Address.class, a1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( Address.class, a1_id ) );

		assert Arrays.asList( 2 ).equals( getAuditReader().getRevisions( Address.class, a2_id ) );
		assert Arrays.asList( 2 ).equals( getAuditReader().getRevisions( Address.class, a2_id ) );
	}

	@Test
	public void testHistoryOfContact() {
		assert getAuditReader().find( Contact.class, pc_id, 1 ).getAddresses().equals(
				TestTools.makeSet( new Address( a1_id, "a1" ) )
		);

		assert getAuditReader().find( Contact.class, pc_id, 2 ).getAddresses().equals(
				TestTools.makeSet( new Address( a1_id, "a1" ), new Address( a2_id, "a2" ) )
		);
	}

	@Test
	public void testHistoryOfPersonalContact() {
		assert getAuditReader().find( PersonalContact.class, pc_id, 1 ).getAddresses().equals(
				TestTools.makeSet( new Address( a1_id, "a1" ) )
		);

		assert getAuditReader().find( PersonalContact.class, pc_id, 2 ).getAddresses().equals(
				TestTools.makeSet( new Address( a1_id, "a1" ), new Address( a2_id, "a2" ) )
		);
	}
}