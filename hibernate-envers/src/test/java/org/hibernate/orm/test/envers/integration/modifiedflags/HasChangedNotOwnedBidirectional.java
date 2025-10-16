/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.integration.inheritance.joined.notownedrelation.Address;
import org.hibernate.orm.test.envers.integration.inheritance.joined.notownedrelation.Contact;
import org.hibernate.orm.test.envers.integration.inheritance.joined.notownedrelation.PersonalContact;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {Address.class, Contact.class, PersonalContact.class})
public class HasChangedNotOwnedBidirectional extends AbstractModifiedFlagsEntityTest {
	private Long pc_id;
	private Long a1_id;
	private Long a2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			pc_id = 1L;
			a1_id = 10L;
			a2_id = 100L;

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

			PersonalContact pcRef = em.find( PersonalContact.class, pc_id );

			Address a2 = new Address( a2_id, "a2" );
			a2.setContact( pcRef );

			em.persist( a2 );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testReferencedEntityHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged(
					auditReader,
					PersonalContact.class,
					pc_id,
					"addresses"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged( auditReader, Address.class, a1_id, "contact" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

			list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged( auditReader, Address.class, a2_id, "contact" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );
		} );
	}
}
