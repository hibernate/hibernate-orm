/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.joined;

import java.util.List;


import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.CardPayment;
import org.hibernate.testing.orm.domain.retail.CashPayment;
import org.hibernate.testing.orm.domain.retail.DomesticVendor;
import org.hibernate.testing.orm.domain.retail.ForeignVendor;
import org.hibernate.testing.orm.domain.retail.Payment;
import org.hibernate.testing.orm.domain.retail.Vendor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( "HHH-11686" )
@DomainModel(standardModels = StandardDomainModel.RETAIL)
@SessionFactory
public class JoinedSubclassDuplicateFieldsWithTreatTest {
	@Test
	public void testRestrictedTreat(SessionFactoryScope scope) {
		// SINGLE_TABLE
		scope.inTransaction( (session) -> {
			final String qry = "from Vendor v where treat(v as DomesticVendor).name = 'Spacely'";
			final List<Vendor> vendors = session.createQuery( qry, Vendor.class ).getResultList();
			assertThat( vendors ).isEmpty();
		} );

		// JOINED
		scope.inTransaction( (session) -> {
			final String qry = "from Payment p where treat(p as CardPayment).transactionId = 123";
			final List<Payment> payments = session.createQuery( qry, Payment.class ).getResultList();
			assertThat( payments ).hasSize( 1 );
			assertThat( payments.get( 0 ) ).isInstanceOf( CardPayment.class );
		} );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			// SINGLE_TABLE
			final DomesticVendor acme = new DomesticVendor( 1, "Acme", "Acme, LLC" );
			final ForeignVendor spacely = new ForeignVendor( 2, "Spacely", "Spacely Space Sprockets, Inc" );
			session.persist( acme );
			session.persist( spacely );

			// JOINED
			final CardPayment cardPayment = new CardPayment( 1, 123, 123L, "USD" );
			final CashPayment cashPayment = new CashPayment( 2, 789L, "USD" );
			session.persist( cardPayment );
			session.persist( cashPayment );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}
}
