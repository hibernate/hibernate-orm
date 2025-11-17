/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naturalid;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-10246")
@EnversTest
@Jpa(annotatedClasses = {Account.class, Customer.class, Device.class})
public class JoinColumnNaturalIdTest {
	private Integer customerId;
	private Integer deviceId;
	private Integer accountId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			Customer customer = new Customer();
			customer.setCustomerNumber( "1234567" );
			customer.setName( "ACME" );
			em.persist( customer );
			customerId = customer.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			Customer customer = em.find( Customer.class, customerId );
			Device device = new Device();
			device.setCustomer( customer );
			Account account = new Account();
			account.setCustomer( customer );
			em.persist( account );
			em.persist( device );
			accountId = account.getId();
			deviceId = device.getId();
		} );

		// Revision 3
		scope.inTransaction( em -> {
			Account account = em.find( Account.class, accountId );
			em.remove( account );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 3, auditReader.getRevisions( Customer.class, customerId ).size() );
			assertEquals( 2, auditReader.getRevisions( Account.class, accountId ).size() );
			assertEquals( 1, auditReader.getRevisions( Device.class, deviceId ).size() );
		} );
	}

	@Test
	public void testRevisionHistoryOfCustomer(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			final Customer customer = new Customer( customerId, "1234567", "ACME" );
			Customer rev1 = auditReader.find( Customer.class, customerId, 1 );
			assertEquals( customer, rev1 );

			final Account account = new Account( accountId, customer );
			final Device device = new Device( deviceId, customer );
			customer.getAccounts().add( account );
			customer.getDevices().add( device );
			Customer rev2 = auditReader.find( Customer.class, customerId, 2 );
			assertEquals( customer, rev2 );

			customer.getAccounts().clear();
			Customer rev3 = auditReader.find( Customer.class, customerId, 3 );
			assertEquals( customer, rev3 );
		} );
	}
}
