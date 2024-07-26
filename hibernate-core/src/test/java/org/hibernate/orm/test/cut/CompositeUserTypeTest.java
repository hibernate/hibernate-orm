/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cut;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
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
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Transaction" ).executeUpdate();
					session.createMutationQuery( "delete from MutualFund" ).executeUpdate();
				}
		);
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

	/**
	 * Tests the {@code =} operator on composite types.
	 */
	@Test
	public void testEqualOperator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Transaction txn = new Transaction();
					txn.setDescription( "foo" );
					txn.setValue( new MonetoryAmount( new BigDecimal( 42 ), Currency.getInstance( "AUD" ) ) );
					txn.setTimestamp( new CompositeDateTime( 2014, 8, 23, 14, 35, 0 ) );
					session.persist( txn );
					final Query<Transaction> q = session.createQuery(
							"from Transaction where value = :amount",
							Transaction.class
					);

					/* Both amount and currency match. */
					q.setParameter(
							"amount",
							new MonetoryAmount( new BigDecimal( 42 ), Currency.getInstance( "AUD" ) )
					);
					assertEquals( 1, q.list().size() );

					/* Only currency matches. */
					q.setParameter(
							"amount",
							new MonetoryAmount( new BigDecimal( 36 ), Currency.getInstance( "AUD" ) )
					);
					assertEquals( 0, q.list().size() );

					/* Only amount matches. */
					q.setParameter(
							"amount",
							new MonetoryAmount( new BigDecimal( 42 ), Currency.getInstance( "EUR" ) )
					);
					assertEquals( 0, q.list().size() );

					/* None match. */
					q.setParameter(
							"amount",
							new MonetoryAmount( new BigDecimal( 76 ), Currency.getInstance( "USD" ) )
					);
					assertEquals( 0, q.list().size() );

					final Query<Transaction> qTimestamp = session.createQuery(
							"from Transaction where timestamp = :timestamp",
							Transaction.class
					);

					/* All matches. */
					qTimestamp.setParameter( "timestamp", new CompositeDateTime( 2014, 8, 23, 14, 35, 0 ) );
					assertEquals( 1, qTimestamp.list().size() );

					/* None matches. */
					qTimestamp.setParameter( "timestamp", new CompositeDateTime( 2013, 9, 25, 12, 31, 25 ) );
					assertEquals( 0, qTimestamp.list().size() );

					/* Year doesn't match. */
					qTimestamp.setParameter( "timestamp", new CompositeDateTime( 2013, 8, 23, 14, 35, 0 ) );
					assertEquals( 0, qTimestamp.list().size() );

					/* Month doesn't match. */
					qTimestamp.setParameter( "timestamp", new CompositeDateTime( 2014, 9, 23, 14, 35, 0 ) );
					assertEquals( 0, qTimestamp.list().size() );

					/* Minute doesn't match. */
					qTimestamp.setParameter( "timestamp", new CompositeDateTime( 2014, 8, 23, 14, 41, 0 ) );
					assertEquals( 0, qTimestamp.list().size() );

					/* Second doesn't match. */
					qTimestamp.setParameter( "timestamp", new CompositeDateTime( 2014, 8, 23, 14, 35, 28 ) );
					assertEquals( 0, qTimestamp.list().size() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5946")
	public void testNotEqualOperator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Transaction t1 = new Transaction();
					t1.setDescription( "foo" );
					t1.setValue( new MonetoryAmount( new BigDecimal( 178 ), Currency.getInstance( "EUR" ) ) );
					t1.setTimestamp( new CompositeDateTime( 2014, 8, 23, 14, 23, 0 ) );
					session.persist( t1 );

					final Transaction t2 = new Transaction();
					t2.setDescription( "bar" );
					t2.setValue( new MonetoryAmount( new BigDecimal( 1000000 ), Currency.getInstance( "USD" ) ) );
					t2.setTimestamp( new CompositeDateTime( 2014, 8, 22, 14, 23, 0 ) );
					session.persist( t2 );

					final Transaction t3 = new Transaction();
					t3.setDescription( "bar" );
					t3.setValue( new MonetoryAmount( new BigDecimal( 1000000 ), Currency.getInstance( "EUR" ) ) );
					t3.setTimestamp( new CompositeDateTime( 2014, 8, 22, 14, 23, 01 ) );
					session.persist( t3 );

					final Query q1 = session.createQuery( "from Transaction where value <> :amount" );
					q1.setParameter(
							"amount",
							new MonetoryAmount( new BigDecimal( 178 ), Currency.getInstance( "EUR" ) )
					);
					assertEquals( 2, q1.list().size() );

					final Query q2 = session.createQuery(
							"from Transaction where value <> :amount and description = :str" );
					q2.setParameter(
							"amount",
							new MonetoryAmount( new BigDecimal( 1000000 ), Currency.getInstance( "USD" ) )
					);
					q2.setParameter( "str", "bar" );
					assertEquals( 1, q2.list().size() );

					final Query q3 = session.createQuery( "from Transaction where timestamp <> :timestamp" );
					q3.setParameter( "timestamp", new CompositeDateTime( 2014, 8, 23, 14, 23, 0 ) );
					assertEquals( 2, q3.list().size() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5946")
	public void testLessThanOperator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query q = session.createQuery( "from Transaction where value < :amount", Transaction.class );
					q.setParameter( "amount", new MonetoryAmount( BigDecimal.ZERO, Currency.getInstance( "EUR" ) ) );
					q.list();
				}
		);

	}

	@Test
	@JiraKey(value = "HHH-5946")
	public void testLessOrEqualOperator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query q = session.createQuery( "from Transaction where value <= :amount", Transaction.class );
					q.setParameter( "amount", new MonetoryAmount( BigDecimal.ZERO, Currency.getInstance( "USD" ) ) );
					q.list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5946")
	public void testGreaterThanOperator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query q = session.createQuery( "from Transaction where value > :amount", Transaction.class );
					q.setParameter( "amount", new MonetoryAmount( BigDecimal.ZERO, Currency.getInstance( "EUR" ) ) );
					q.list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5946")
	public void testGreaterOrEqualOperator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query q = session.createQuery( "from Transaction where value >= :amount", Transaction.class );
					q.setParameter( "amount", new MonetoryAmount( BigDecimal.ZERO, Currency.getInstance( "USD" ) ) );
					q.list();
				}
		);
	}

}
