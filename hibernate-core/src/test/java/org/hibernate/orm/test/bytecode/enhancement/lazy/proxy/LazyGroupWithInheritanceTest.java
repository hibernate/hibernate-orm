/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */

@JiraKey("HHH-11147")
@DomainModel(
		annotatedClasses = {
				Customer.class,
				ForeignCustomer.class,
				DomesticCustomer.class,
				Payment.class,
				CreditCardPayment.class,
				DebitCardPayment.class,
				Address.class,
				Order.class,
				OrderSupplemental.class,
				OrderSupplemental2.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class LazyGroupWithInheritanceTest {

	@Test
	public void loadEntityWithAssociationToAbstract(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		scope.inTransaction(
				(session) -> {
					final Order loaded = session.byId( Order.class ).load( 1 );
					assert Hibernate.isPropertyInitialized( loaded, "customer" );
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );
					assertThat( loaded, instanceOf( PersistentAttributeInterceptable.class ) );
					final PersistentAttributeInterceptor interceptor = ((PersistentAttributeInterceptable) loaded).$$_hibernate_getInterceptor();
					assertThat( interceptor, instanceOf( BytecodeLazyAttributeInterceptor.class ) );
					final BytecodeLazyAttributeInterceptor interceptor1 = (BytecodeLazyAttributeInterceptor) interceptor;

				}
		);
	}

	@Test
	public void queryEntityWithAssociationToAbstract(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		final AtomicInteger expectedQueryCount = new AtomicInteger( 0 );

		scope.inTransaction(
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
					expectedQueryCount.set( 1 );
					//expectedQueryCount.set( 4 );
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
	 * Same test as {@link #queryEntityWithAssociationToAbstract(SessionFactoryScope)}, but using runtime
	 * fetching to issues just a single select
	 */
	@Test
	public void queryEntityWithAssociationToAbstractRuntimeFetch(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		final AtomicInteger expectedQueryCount = new AtomicInteger( 0 );

		scope.inTransaction(
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

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
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

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.inTransaction(
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
}
