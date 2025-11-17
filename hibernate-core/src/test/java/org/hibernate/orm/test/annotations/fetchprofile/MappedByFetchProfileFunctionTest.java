/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;

import org.hibernate.Hibernate;
import org.hibernate.orm.test.annotations.fetchprofile.mappedby.Address;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JiraKey(value = "HHH-14071")
@DomainModel(
		annotatedClasses = {
				Customer6.class,
				Address.class
		}
)
@SessionFactory
public class MappedByFetchProfileFunctionTest {

	@Test
	public void testFetchWithOneToOneMappedBy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.enableFetchProfile( "address-with-customer" );
					Address address = new Address();
					address.setStreet( "Test Road 1" );
					Customer6 customer = new Customer6();
					customer.setName( "Tester" );
					customer.setAddress( address );

					session.persist( address );
					session.persist( customer );

					session.flush();
					session.clear();

					address = session.get( Address.class, address.getId() );
					assertThat( Hibernate.isInitialized( address.getCustomer() ) ).isTrue();
					session.remove( address.getCustomer() );
					session.remove( address );
				}
		);
	}

}
