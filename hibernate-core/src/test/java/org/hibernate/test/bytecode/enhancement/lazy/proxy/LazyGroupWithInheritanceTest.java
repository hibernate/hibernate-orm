/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.bytecode.enhancement.lazy.group.BidirectionalLazyGroupsInEmbeddableTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */

@TestForIssue(jiraKey = "HHH-11147")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions( lazyLoading = true )
public class LazyGroupWithInheritanceTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void queryEntityWithAssociationToAbstract() {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final AtomicInteger expectedQueryCount = new AtomicInteger( 0 );

		inTransaction(
				session -> {
					final List<Order> orders = session.createQuery( "select o from Order o", Order.class ).list();

					// todo (HHH-11147) : this is a regression from 4.x
					//		- the condition is that the association from Order to Customer points to the non-root
					//			entity (Customer) rather than one of its concrete sub-types (DomesticCustomer,
					//			ForeignCustomer).  We'd have to read the "other table" to be able to resolve the
					// 			concrete type.  The same holds true for associations to versioned entities as well.
					//			The only viable solution I see would be to join to the "other side" and read the
					//			version/discriminator[1].  But of course that means doing the join which is generally
					//			what the application is trying to avoid in the first place
					//expectedQueryCount.set( 1 );
					expectedQueryCount.set( 4 );
					assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );

					for ( Order order : orders ) {
						System.out.println( "############################################" );
						System.out.println( "Starting Order #" + order.getOid() );

						// accessing the many-to-one's id should not trigger a load
						if ( order.getCustomer().getOid() == null ) {
							System.out.println( "Got Order#customer: " + order.getCustomer().getOid() );
						}
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );

						// accessing the one-to-many should trigger a load
						final Set<Payment> orderPayments = order.getPayments();
						System.out.println( "Number of payments = " + orderPayments.size() );
						expectedQueryCount.getAndIncrement();
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );

						// access the non-inverse, logical 1-1
						order.getSupplemental();
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );
						if ( order.getSupplemental() != null ) {
							System.out.println( "Got Order#supplemental = " + order.getSupplemental().getOid() );
							assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );
						}

						// access the inverse, logical 1-1
						order.getSupplemental2();
						expectedQueryCount.getAndIncrement();
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );
						if ( order.getSupplemental2() != null ) {
							System.out.println( "Got Order#supplemental2 = " + order.getSupplemental2().getOid() );
							assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );
						}
					}
				}
		);
	}

	/**
	 * Same test as {@link #queryEntityWithAssociationToAbstract()}, but using runtime
	 * fetching to issues just a single select
	 */
	@Test
	public void queryEntityWithAssociationToAbstractRuntimeFetch() {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final AtomicInteger expectedQueryCount = new AtomicInteger( 0 );

		inTransaction(
				session -> {
					final String qry = "select o from Order o join fetch o.customer c join fetch o.payments join fetch o.supplemental join fetch o.supplemental2";

					final List<Order> orders = session.createQuery( qry, Order.class ).list();

					// oh look - just a single query for all the data we will need.  hmm, crazy
					expectedQueryCount.set( 1 );
					assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );

					for ( Order order : orders ) {
						System.out.println( "############################################" );
						System.out.println( "Starting Order #" + order.getOid() );

						// accessing the many-to-one's id should not trigger a load
						if ( order.getCustomer().getOid() == null ) {
							System.out.println( "Got Order#customer: " + order.getCustomer().getOid() );
						}
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );

						// accessing the one-to-many should trigger a load
						final Set<Payment> orderPayments = order.getPayments();
						System.out.println( "Number of payments = " + orderPayments.size() );

						// loaded already
						// expectedQueryCount.getAndIncrement();
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );

						// access the non-inverse, logical 1-1
						order.getSupplemental();
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );
						if ( order.getSupplemental() != null ) {
							System.out.println( "Got Order#supplemental = " + order.getSupplemental().getOid() );
							assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );
						}

						// access the inverse, logical 1-1
						order.getSupplemental2();

						// loaded already
						// expectedQueryCount.getAndIncrement();
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );
						if ( order.getSupplemental2() != null ) {
							System.out.println( "Got Order#supplemental2 = " + order.getSupplemental2().getOid() );
							assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );
						}
					}
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );

		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		ssrb.applySetting( AvailableSettings.USE_QUERY_CACHE, "false" );
	}


	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					final Address austin = new Address( 1, "Austin" );
					final Address london = new Address( 2, "London" );

					session.save( austin );
					session.save( london );

					final ForeignCustomer acme = new ForeignCustomer( 1, "Acme", london, "1234" );
					final ForeignCustomer acmeBrick = new ForeignCustomer( 2, "Acme Brick", london, "9876", acme );

					final ForeignCustomer freeBirds = new ForeignCustomer( 3, "Free Birds", austin, "13579" );

					session.save( acme );
					session.save( acmeBrick );
					session.save( freeBirds );

					final Order order1 = new Order( 1, "some text", freeBirds );
					freeBirds.getOrders().add( order1 );
					session.save( order1 );

					final OrderSupplemental orderSupplemental = new OrderSupplemental( 1, 1 );
					order1.setSupplemental( orderSupplemental );
					final OrderSupplemental2 orderSupplemental2_1 = new OrderSupplemental2( 2, 2 );
					order1.setSupplemental2( orderSupplemental2_1 );
					orderSupplemental2_1.setOrder( order1 );
					session.save( orderSupplemental );
					session.save( orderSupplemental2_1 );

					final Order order2 = new Order( 2, "some text", acme );
					acme.getOrders().add( order2 );
					session.save( order2 );

					final OrderSupplemental2 orderSupplemental2_2 = new OrderSupplemental2( 3, 3 );
					order2.setSupplemental2( orderSupplemental2_2 );
					orderSupplemental2_2.setOrder( order2 );
					session.save( orderSupplemental2_2 );

					final CreditCardPayment payment1 = new CreditCardPayment( 1, 1F, "1" );
					session.save( payment1 );
					order1.getPayments().add( payment1 );

					final DebitCardPayment payment2 = new DebitCardPayment( 2, 2F, "2" );
					session.save( payment2 );
					order1.getPayments().add( payment2 );



				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from CreditCardPayment" ).executeUpdate();
					session.createQuery( "delete from DebitCardPayment" ).executeUpdate();

					session.createQuery( "delete from OrderSupplemental2" ).executeUpdate();

					session.createQuery( "delete from Order" ).executeUpdate();

					session.createQuery( "delete from OrderSupplemental" ).executeUpdate();

					session.createQuery( "delete from DomesticCustomer" ).executeUpdate();
					session.createQuery( "delete from ForeignCustomer" ).executeUpdate();

					session.createQuery( "delete from Address" ).executeUpdate();
				}
		);
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );

		sources.addAnnotatedClass( Customer.class );
		sources.addAnnotatedClass( ForeignCustomer.class );
		sources.addAnnotatedClass( DomesticCustomer.class );

		sources.addAnnotatedClass( Payment.class );
		sources.addAnnotatedClass( CreditCardPayment.class );
		sources.addAnnotatedClass( DebitCardPayment.class );

		sources.addAnnotatedClass( Address.class );

		sources.addAnnotatedClass( Order.class );
		sources.addAnnotatedClass( OrderSupplemental.class );
		sources.addAnnotatedClass( OrderSupplemental2.class );
	}


}
