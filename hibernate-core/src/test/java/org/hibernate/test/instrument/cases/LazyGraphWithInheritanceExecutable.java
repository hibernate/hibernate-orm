package org.hibernate.test.instrument.cases;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;
import org.hibernate.test.instrument.domain.Address;
import org.hibernate.test.instrument.domain.CreditCardPayment;
import org.hibernate.test.instrument.domain.Customer;
import org.hibernate.test.instrument.domain.DebitCardPayment;
import org.hibernate.test.instrument.domain.DomesticCustomer;
import org.hibernate.test.instrument.domain.ForeignCustomer;
import org.hibernate.test.instrument.domain.Order;
import org.hibernate.test.instrument.domain.OrderSupplemental;
import org.hibernate.test.instrument.domain.OrderSupplemental2;
import org.hibernate.test.instrument.domain.Payment;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.hibernate.testing.transaction.TransactionUtil.inTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LazyGraphWithInheritanceExecutable extends AbstractExecutable{
	@Override
	public void execute() throws Exception {
		prepareTestData();

		try {
			queryEntityWithAssociationToAbstract();
			queryEntityWithAssociationToAbstractRuntimeFetch();
		}
		finally {
			cleanUpTestData();
		}
	}

	private void queryEntityWithAssociationToAbstract() {
		final Statistics stats = getFactory().getStatistics();
		stats.clear();

		final AtomicInteger expectedQueryCount = new AtomicInteger(0);

		inTransaction(
				getFactory(),
				new Consumer<Session>() {
					@Override
					public void accept(Session session) {
						final List orders = session.createQuery("select o from Order o").list();
						expectedQueryCount.set(1);
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );

						for (Object object : orders) {
							final Order order = (Order) object;

							System.out.println( "############################################");
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
							assertEquals(expectedQueryCount.get(), stats.getPrepareStatementCount());
							if ( order.getSupplemental() != null ) {
								assertTrue( order.getSupplemental() instanceof HibernateProxy );
								System.out.println("Got Order#supplemental = " + order.getSupplemental().getOid());
								assertEquals(expectedQueryCount.get(), stats.getPrepareStatementCount());
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
				}
		);
	}

	private void queryEntityWithAssociationToAbstractRuntimeFetch() {
		final Statistics stats = getFactory().getStatistics();
		stats.clear();

		final AtomicInteger expectedQueryCount = new AtomicInteger(0);

		inTransaction(
				getFactory(),
				new Consumer<Session>() {
					@Override
					public void accept(Session session) {
						final String qry = "select o from Order o join fetch o.customer c join fetch o.payments join fetch o.supplemental join fetch o.supplemental2";

						final List orders = session.createQuery( qry ).list();

						// oh look - just a single query for all the data we will need.  hmm, crazy
						expectedQueryCount.set(1);
						assertEquals( expectedQueryCount.get(), stats.getPrepareStatementCount() );

						for (Object object : orders) {
							final Order order = (Order) object;

							System.out.println( "############################################");
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
							assertEquals(expectedQueryCount.get(), stats.getPrepareStatementCount());
							if ( order.getSupplemental() != null ) {
								System.out.println("Got Order#supplemental = " + order.getSupplemental().getOid());
								assertEquals(expectedQueryCount.get(), stats.getPrepareStatementCount());
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
				}
		);
	}


	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);

		configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, "true" );
		configuration.setProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		configuration.setProperty(AvailableSettings.USE_QUERY_CACHE, "false" );
	}

	@Override
	protected void applyAnnotatedClasses(Configuration cfg) {
		super.applyAnnotatedClasses(cfg);
		cfg.addAnnotatedClass( Customer.class );
		cfg.addAnnotatedClass( ForeignCustomer.class );
		cfg.addAnnotatedClass( DomesticCustomer.class );

		cfg.addAnnotatedClass( Payment.class );
		cfg.addAnnotatedClass( CreditCardPayment.class );
		cfg.addAnnotatedClass( DebitCardPayment.class );

		cfg.addAnnotatedClass( Address.class );

		cfg.addAnnotatedClass( Order.class );
		cfg.addAnnotatedClass( OrderSupplemental.class );
		cfg.addAnnotatedClass( OrderSupplemental2.class );
	}

	private void prepareTestData() {
		inTransaction(
				getFactory(),
				new Consumer<Session>() {
					@Override
					public void accept(Session session) {
						final Address austin = new Address(1, "Austin");
						final Address london = new Address(2, "London");

						session.save( austin );
						session.save( london );

						final ForeignCustomer acme = new ForeignCustomer(1, "Acme", london, "1234");
						final ForeignCustomer acmeBrick = new ForeignCustomer(2, "Acme Brick", london, "9876", acme );

						final ForeignCustomer freeBirds = new ForeignCustomer(3, "Free Birds", austin, "13579");

						session.save(acme);
						session.save(acmeBrick);
						session.save(freeBirds);

						final Order order1 = new Order(1, "some text", freeBirds);
						freeBirds.getOrders().add(order1);

						final Order order2 = new Order(2, "some text", acme);
						acme.getOrders().add(order2);

						session.save(order1);
						session.save(order2);

						final CreditCardPayment payment1 = new CreditCardPayment(1, 1F, "1");
						session.save(payment1);
						order1.getPayments().add(payment1);

						final DebitCardPayment payment2 = new DebitCardPayment(2, 2F, "2");
						session.save(payment2);
						order1.getPayments().add(payment2);

						final OrderSupplemental orderSupplemental = new OrderSupplemental(1, 1);
						order1.setSupplemental( orderSupplemental );
						session.save(orderSupplemental);

						final OrderSupplemental2 orderSupplemental2_1 = new OrderSupplemental2(2, 2);
						order1.setSupplemental2( orderSupplemental2_1 );
						orderSupplemental2_1.setOrder( order1 );
						session.save(orderSupplemental2_1);

						final OrderSupplemental2 orderSupplemental2_2 = new OrderSupplemental2(3, 3);
						order2.setSupplemental2( orderSupplemental2_2 );
						orderSupplemental2_2.setOrder( order2 );
						session.save(orderSupplemental2_2);
					}
				}
		);
	}

	private void cleanUpTestData() {

	}
}
