/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import org.hibernate.annotations.CompositeType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = ImmutableCompositeUserTypeTest.Wallet.class
)
@SessionFactory
@JiraKey( value = "HHH-15554")
public class ImmutableCompositeUserTypeTest {

	@Test
	public void testImmutableCutMerge(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ImmutableMonetoryAmount monetoryAmount = new ImmutableMonetoryAmount(
							new BigDecimal( 1.5 ),
							Currency.getInstance( "USD" )
					);
					Wallet wallet = new Wallet( 1, monetoryAmount );
					session.merge( wallet );

					List<Wallet> wallets = session.createQuery(
							"from Wallet w where w.amount.amount > 1.0 and w.amount.currency = 'USD'" ).list();
					assertThat( wallets.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "Wallet")
	@Table(name = "Wallet_TABLE")
	public static class Wallet {
		@Id
		private Integer id;

		@CompositeType(value = ImmutableMonetoryAmountUserType.class)
		private ImmutableMonetoryAmount amount;

		public Wallet() {
		}

		public Wallet(Integer id, ImmutableMonetoryAmount amount) {
			this.id = id;
			this.amount = amount;
		}

		public Integer getId() {
			return id;
		}

		public ImmutableMonetoryAmount getAmount() {
			return amount;
		}
	}

}
