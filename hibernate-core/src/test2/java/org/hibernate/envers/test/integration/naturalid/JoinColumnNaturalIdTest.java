/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naturalid;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-10246")
public class JoinColumnNaturalIdTest extends BaseEnversJPAFunctionalTestCase {
	private Integer customerId;
	private Integer deviceId;
	private Integer accountId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Account.class,
				Customer.class,
				Device.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getOrCreateEntityManager();
		try {
			// Revision 1
			em.getTransaction().begin();
			Customer customer = new Customer();
			customer.setCustomerNumber( "1234567" );
			customer.setName( "ACME" );
			em.persist( customer );
			em.getTransaction().commit();
			customerId = customer.getId();
			// Revision 2
			em.getTransaction().begin();
			Device device = new Device();
			device.setCustomer( customer );
			Account account = new Account();
			account.setCustomer( customer );
			em.persist( account );
			em.persist( device );
			em.getTransaction().commit();
			accountId = account.getId();
			deviceId = device.getId();
			// Revision 3
			em.getTransaction().begin();
			em.remove( account );
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( 3, getAuditReader().getRevisions( Customer.class, customerId ).size() );
		assertEquals( 2, getAuditReader().getRevisions( Account.class, accountId ).size() );
		assertEquals( 1, getAuditReader().getRevisions( Device.class, deviceId ).size() );
	}

	@Test
	public void testRevisionHistoryOfCustomer() {
		final Customer customer = new Customer( customerId, "1234567", "ACME" );
		Customer rev1 = getAuditReader().find( Customer.class, customerId, 1 );
		assertEquals( customer, rev1 );

		final Account account = new Account( accountId, customer );
		final Device device = new Device( deviceId, customer );
		customer.getAccounts().add( account );
		customer.getDevices().add( device );
		Customer rev2 = getAuditReader().find( Customer.class, customerId, 2 );
		assertEquals( customer, rev2 );

		customer.getAccounts().clear();
		Customer rev3 = getAuditReader().find( Customer.class, customerId, 3 );
		assertEquals( customer, rev3 );
	}
}
