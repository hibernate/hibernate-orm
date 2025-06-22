/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.keymanytoone.bidir.component;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.DefaultLoadEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Steve Ebersole
 */
@DomainModel(xmlMappings = {
		"org/hibernate/orm/test/keymanytoone/bidir/component/EagerMapping.hbm.xml"
})
@SessionFactory(generateStatistics = true)
@BootstrapServiceRegistry( integrators = EagerKeyManyToOneTest.CustomLoadIntegrator.class )
public class EagerKeyManyToOneTest {

	public static class CustomLoadIntegrator implements Integrator {
		@Override
		public void integrate(
				Metadata metadata,
				BootstrapContext bootstrapContext,
				SessionFactoryImplementor sessionFactory) {
			integrate( sessionFactory );
		}

		private void integrate(SessionFactoryImplementor sessionFactory) {
			sessionFactory.getEventListenerRegistry().prependListeners(
					EventType.LOAD,
					new CustomLoadListener()
			);
		}
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSaveCascadedToKeyManyToOne(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// test cascading a save to an association with a key-many-to-one which refers to a
		// just saved entity
		scope.inTransaction(
				s -> {
					Customer cust = new Customer( "Acme, Inc." );
					Order order = new Order( new Order.Id( cust, 1 ) );
					cust.getOrders().add( order );
					s.persist( cust );
					s.flush();
					assertEquals( 2, statistics.getEntityInsertCount() );
				}
		);
	}

	@Test
	public void testLoadingStrategies(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				s -> {
					Customer cust = new Customer( "Acme, Inc." );
					Order order = new Order( new Order.Id( cust, 1 ) );
					cust.getOrders().add( order );
					s.persist( cust );
				}
		);

		scope.inTransaction(
				s -> {
					Customer cust = (Customer) s.createQuery( "from Customer" ).uniqueResult();
					assertEquals( 1, cust.getOrders().size() );
					s.clear();

					cust = (Customer) s.createQuery( "from Customer c join fetch c.orders" ).uniqueResult();
					assertEquals( 1, cust.getOrders().size() );
					s.clear();

					cust = (Customer) s.createQuery( "from Customer c join fetch c.orders as o join fetch o.id.customer" )
							.uniqueResult();
					assertEquals( 1, cust.getOrders().size() );
					s.clear();

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Customer> criteria = criteriaBuilder.createQuery( Customer.class );
					criteria.from( Customer.class );
					cust = s.createQuery( criteria ).uniqueResult();
					assertEquals( 1, cust.getOrders().size() );
					s.clear();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-2277")
	public void testLoadEntityWithEagerFetchingToKeyManyToOneReferenceBackToSelf(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// long winded method name to say that this is a test specifically for HHH-2277 ;)
		// essentially we have a bidirectional association where one side of the
		// association is actually part of a composite PK.
		//
		// The way these are mapped causes the problem because both sides
		// are defined as eager which leads to the infinite loop; if only
		// one side is marked as eager, then all is ok.  In other words the
		// problem arises when both pieces of instance data are coming from
		// the same result set.  This is because no "entry" can be placed
		// into the persistence context for the association with the
		// composite key because we are in the process of trying to build
		// the composite-id instance
		Customer customer = new Customer( "Acme, Inc." );
		Order order = new Order( new Order.Id( customer, 1 ) );
		customer.getOrders().add( order );
		scope.inTransaction(
				session -> session.persist( customer )
		);

		scope.inTransaction(
				session -> {
					try {
						session.get( Customer.class, customer.getId() );
					}
					catch (OverflowCondition overflow) {
						fail( "get()/load() caused overflow condition" );
					}
				}
		);
	}

	private static class OverflowCondition extends RuntimeException {
	}

	private static class CustomLoadListener extends DefaultLoadEventListener {
		private int internalLoadCount = 0;

		@Override
		public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException {
			if ( LoadEventListener.INTERNAL_LOAD_EAGER.getName().equals( loadType.getName() ) ) {
				internalLoadCount++;
				if ( internalLoadCount > 10 ) {
					throw new OverflowCondition();
				}
			}
			super.onLoad( event, loadType );
			internalLoadCount--;
		}
	}
}
