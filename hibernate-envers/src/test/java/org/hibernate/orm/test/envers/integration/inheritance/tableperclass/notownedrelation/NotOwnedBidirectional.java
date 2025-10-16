/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.tableperclass.notownedrelation;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {Address.class, Contact.class, PersonalContact.class})
public class NotOwnedBidirectional {
	private Long pc_id;
	private Long a1_id;
	private Long a2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		pc_id = 1l;
		a1_id = 10l;
		a2_id = 100l;

		// Rev 1
		scope.inTransaction( em -> {
			PersonalContact pc = new PersonalContact( pc_id, "e", "f" );

			Address a1 = new Address( a1_id, "a1" );
			a1.setContact( pc );

			em.persist( pc );
			em.persist( a1 );
		} );

		// Rev 2
		scope.inTransaction( em -> {
			PersonalContact pc = em.find( PersonalContact.class, pc_id );

			Address a2 = new Address( a2_id, "a2" );
			a2.setContact( pc );

			em.persist( a2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( Contact.class, pc_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( PersonalContact.class, pc_id ) );

			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( Address.class, a1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( Address.class, a1_id ) );

			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( Address.class, a2_id ) );
			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( Address.class, a2_id ) );
		} );
	}

	@Test
	public void testHistoryOfContact(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					TestTools.makeSet( new Address( a1_id, "a1" ) ),
					auditReader.find( Contact.class, pc_id, 1 ).getAddresses()
			);

			assertEquals(
					TestTools.makeSet( new Address( a1_id, "a1" ), new Address( a2_id, "a2" ) ),
					auditReader.find( Contact.class, pc_id, 2 ).getAddresses()
			);
		} );
	}

	@Test
	public void testHistoryOfPersonalContact(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					TestTools.makeSet( new Address( a1_id, "a1" ) ),
					auditReader.find( PersonalContact.class, pc_id, 1 ).getAddresses()
			);

			assertEquals(
					TestTools.makeSet( new Address( a1_id, "a1" ), new Address( a2_id, "a2" ) ),
					auditReader.find( PersonalContact.class, pc_id, 2 ).getAddresses()
			);
		} );
	}
}
