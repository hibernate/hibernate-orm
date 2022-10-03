/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cut;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@TestForIssue( jiraKey = "HHH-15554")
public class ImmutableCompositeUserTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Wallet.class};
	}

	@Test
	public void testImmutableCutMerge() {
		inTransaction(
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

		@Type(type = "org.hibernate.test.cut.ImmutableMonetoryAmountUserType")
		@Columns(columns = {
				@Column(name = "amount"),
				@Column(name = "currency"),
		})
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
