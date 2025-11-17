/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cut/Transaction.hbm.xml"
		}
)
@SessionFactory
public class CompositeUserTypeTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCompositeUserType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Transaction tran = new Transaction();
					tran.setDescription( "a small transaction" );
					tran.setValue( new MonetoryAmount( new BigDecimal( 1.5 ), Currency.getInstance( "USD" ) ) );
					session.persist( tran );

					List<Transaction> result = session.createQuery(
									"from Transaction tran where tran.value.amount > 1.0 and tran.value.currency = 'USD'",
									Transaction.class
							)
							.list();
					assertEquals( 1, result.size() );

					tran.getValue().setCurrency( Currency.getInstance( "AUD" ) );

					result = session.createQuery(
									"from Transaction tran where tran.value.amount > 1.0 and tran.value.currency = 'AUD'",
									Transaction.class
							)
							.list();
					assertEquals( 1, result.size() );

					if ( !( scope.getSessionFactory().getJdbcServices().getDialect() instanceof HSQLDialect ) ) {
						result = session.createQuery(
										"from Transaction txn where txn.value = (1.5, 'AUD')",
										Transaction.class
								)
								.list();
						assertEquals( result.size(), 1 );
						result = session.createQuery(
										"" +
												"from Transaction where value = (1.5, 'AUD')",
										Transaction.class
								)
								.list();
						assertEquals( result.size(), 1 );
						result = session.createQuery(
								"from Transaction where value != (1.4, 'AUD')",
								Transaction.class
						).list();
						assertEquals( result.size(), 1 );
					}
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "HHH-6788")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "HHH-6867")
	public void testCustomColumnReadAndWrite(SessionFactoryScope scope) {
		final BigDecimal AMOUNT = new BigDecimal( 73000000d );
		final BigDecimal AMOUNT_MILLIONS = AMOUNT.divide( new BigDecimal( 1000000d ) );
		MutualFund f = new MutualFund();
		f.setHoldings( new MonetoryAmount( AMOUNT, Currency.getInstance( "USD" ) ) );

		scope.inTransaction(
				session -> {
					session.persist( f );
					session.flush();
					// Test value conversion during insert
					BigDecimal amountViaSql = (BigDecimal) session.createNativeQuery(
									"select amount_millions from MutualFund", BigDecimal.class )
							.uniqueResult();
					assertEquals( AMOUNT_MILLIONS.doubleValue(), amountViaSql.doubleValue(), 0.01d );

					// Test projection
					BigDecimal amountViaHql = session.createQuery(
									"select f.holdings.amount from MutualFund f", BigDecimal.class )
							.uniqueResult();
					assertEquals( AMOUNT.doubleValue(), amountViaHql.doubleValue(), 0.01d );

					BigDecimal one = new BigDecimal( 1 );

					// Test restriction and entity load via criteria
					// Test predicate and entity load via HQL
					MutualFund mutualFund = (MutualFund) session.createQuery(
									"from MutualFund f where f.holdings.amount between ?1 and ?2", MutualFund.class )
							.setParameter( 1, AMOUNT.subtract( one ), BigDecimal.class )
							.setParameter( 2, AMOUNT.add( one ), BigDecimal.class )
							.uniqueResult();
					assertEquals( AMOUNT.doubleValue(), mutualFund.getHoldings().getAmount().doubleValue(), 0.01d );

				}
		);
	}
}
