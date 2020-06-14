package org.hibernate.test.annotations.fetchprofile;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.fetchprofile.mappedby.Address;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@TestForIssue( jiraKey = "HHH-14071" )
public class MappedByFetchProfileFunctionTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testFetchWithOneToOneMappedBy() {
		final Session session = openSession();
		session.enableFetchProfile( "address-with-customer" );
		final Transaction transaction = session.beginTransaction();

		Address address = new Address();
		address.setStreet("Test Road 1");
		Customer6 customer = new Customer6();
		customer.setName("Tester");
		customer.setAddress(address);

		session.persist(address);
		session.persist(customer);

		session.flush();
		session.clear();

		address = session.get(Address.class, address.getId());
		assertTrue(Hibernate.isInitialized(address.getCustomer()));
		session.delete(address.getCustomer());
		session.delete(address);

		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Customer6.class,
				Address.class
		};
	}

}
