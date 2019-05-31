/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.inheritance.single.notownedrelation;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.single.notownedrelation.Address;
import org.hibernate.envers.test.support.domains.inheritance.single.notownedrelation.Contact;
import org.hibernate.envers.test.support.domains.inheritance.single.notownedrelation.PersonalContact;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Inheritance")
public class NotOwnedBidirectionalTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long pc_id;
	private Long a1_id;
	private Long a2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Address.class, Contact.class, PersonalContact.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					PersonalContact pc = new PersonalContact();
					pc.setEmail( "e" );
					pc.setFirstname( "f" );

					Address a1 = new Address();
					a1.setAddress1( "a1" );
					a1.setContact( pc );

					entityManager.persist( pc );
					entityManager.persist( a1 );

					pc_id = pc.getId();
					a1_id = a1.getId();
				},

				// Revision 2
				entityManager -> {
					PersonalContact pc = entityManager.find( PersonalContact.class, pc_id );

					Address a2 = new Address();
					a2.setAddress1( "a2" );
					a2.setContact( pc );

					entityManager.persist( a2 );

					a2_id = a2.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( Contact.class, pc_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( PersonalContact.class, pc_id ), contains( 1, 2 ) );

		assertThat( getAuditReader().getRevisions( Address.class, a1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( Address.class, a1_id ), contains( 1 ) );

		assertThat( getAuditReader().getRevisions( Address.class, a2_id ), contains( 2 ) );
		assertThat( getAuditReader().getRevisions( Address.class, a2_id ), contains( 2 ) );
	}

	@DynamicTest
	public void testHistoryOfContact() {
		final Address a1 = new Address( a1_id, "a1" );
		final Address a2 = new Address( a2_id, "a2" );

		assertThat( getAuditReader().find( Contact.class, pc_id, 1 ).getAddresses(), containsInAnyOrder( a1 ) );
		assertThat( getAuditReader().find( Contact.class, pc_id, 2 ).getAddresses(), containsInAnyOrder( a1, a2 ) );
	}

	@DynamicTest
	public void testHistoryOfPersonalContact() {
		final Address a1 = new Address( a1_id, "a1" );
		final Address a2 = new Address( a2_id, "a2" );

		assertThat( getAuditReader().find( PersonalContact.class, pc_id, 1 ).getAddresses(), containsInAnyOrder( a1 ) );
		assertThat( getAuditReader().find( PersonalContact.class, pc_id, 2 ).getAddresses(), containsInAnyOrder( a1, a2 ) );
	}
}