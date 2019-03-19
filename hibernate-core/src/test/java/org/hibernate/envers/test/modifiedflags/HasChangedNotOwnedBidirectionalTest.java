/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.modifiedflags;

import org.hibernate.envers.test.support.domains.inheritance.joined.notownedrelation.Address;
import org.hibernate.envers.test.support.domains.inheritance.joined.notownedrelation.Contact;
import org.hibernate.envers.test.support.domains.inheritance.joined.notownedrelation.PersonalContact;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Disabled("NYI - Joined Inheritance Support")
public class HasChangedNotOwnedBidirectionalTest extends AbstractModifiedFlagsEntityTest {
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
					final PersonalContact personalContact = new PersonalContact( 1L, "e", "f" );
					final Address address = new Address( 10L, "a1" );
					address.setContact( personalContact );

					entityManager.persist( personalContact );
					entityManager.persist( address );

					this.a1_id = address.getId();
					this.pc_id = personalContact.getId();
				},

				// Revision 2
				entityManager -> {
					final PersonalContact personalContact = entityManager.find( PersonalContact.class, pc_id );

					final Address address = new Address( 100L, "a2" );
					address.setContact( personalContact );

					entityManager.persist( address );

					this.a2_id = address.getId();
				}
		);
	}

	@DynamicTest
	public void testReferencedEntityHasChanged() {
		assertThat(
				extractRevisions( queryForPropertyHasChanged( PersonalContact.class, pc_id, "addresses" ) ),
				contains( 1, 2 )
		);

		assertThat(
				extractRevisions( queryForPropertyHasChanged( Address.class, a1_id, "contact" ) ),
				contains( 1 )
		);

		assertThat(
				extractRevisions( queryForPropertyHasChanged( Address.class, a2_id, "contact" ) ),
				contains( 2 )
		);
	}
}