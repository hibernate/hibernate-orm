/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.keymanytoone.bidir.component;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.DefaultLoadEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings( {"unchecked"})
public class EagerKeyManyToOneTest extends BaseCoreFunctionalTestCase {
	@Override
    public String[] getMappings() {
		return new String[] { "keymanytoone/bidir/component/EagerMapping.hbm.xml" };
	}

	@Override
    public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
		builder.with(
				new Integrator() {

				    @Override
					public void integrate(
							Configuration configuration,
							SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
                        integrate(serviceRegistry);
					}

                    @Override
				    public void integrate( MetadataImplementor metadata,
				                           SessionFactoryImplementor sessionFactory,
				                           SessionFactoryServiceRegistry serviceRegistry ) {
                        integrate(serviceRegistry);
				    }

                    private void integrate( SessionFactoryServiceRegistry serviceRegistry ) {
                        serviceRegistry.getService( EventListenerRegistry.class ).prependListeners(EventType.LOAD,
                                                                                                   new CustomLoadListener());
                    }

					@Override
					public void disintegrate(
							SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
					}
				}
		);
	}

	@Test
	public void testSaveCascadedToKeyManyToOne() {
		sessionFactory().getStatistics().clear();

		// test cascading a save to an association with a key-many-to-one which refers to a
		// just saved entity
		Session s = openSession();
		s.beginTransaction();
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( new Order.Id( cust, 1 ) );
		cust.getOrders().add( order );
		s.save( cust );
		s.flush();
		assertEquals( 2, sessionFactory().getStatistics().getEntityInsertCount() );
		s.delete( cust );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testLoadingStrategies() {
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( new Order.Id( cust, 1 ) );
		cust.getOrders().add( order );
		s.save( cust );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		cust = ( Customer ) s.createQuery( "from Customer" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createQuery( "from Customer c join fetch c.orders" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createQuery( "from Customer c join fetch c.orders as o join fetch o.id.customer" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createCriteria( Customer.class ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		s.delete( cust );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-2277")
	public void testLoadEntityWithEagerFetchingToKeyManyToOneReferenceBackToSelf() {
		sessionFactory().getStatistics().clear();

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
		Session s = openSession();
		s.beginTransaction();
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( new Order.Id( cust, 1 ) );
		cust.getOrders().add( order );
		s.save( cust );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		try {
			cust = ( Customer ) s.get( Customer.class, cust.getId() );
		}
		catch( OverflowCondition overflow ) {
			fail( "get()/load() caused overflow condition" );
		}
		s.delete( cust );
		s.getTransaction().commit();
		s.close();
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
