/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.bytecode.enhancement.ondemandload;

import java.math.BigDecimal;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LazyCollectionWithClearedSessionTestTask extends AbstractEnhancerTestTask {

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		try (Session s = getFactory().openSession()) {
			s.beginTransaction();
			try {

				Store store = new Store( 1 ).setName( "Acme Super Outlet" );
				s.persist( store );

				Product product = new Product( "007" ).setName( "widget" ).setDescription( "FooBar" );
				s.persist( product );

				store.addInventoryProduct( product ).setQuantity( 10L ).setStorePrice( new BigDecimal( 500 ) );

				s.getTransaction().commit();
			}
			catch (Exception e) {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	public void cleanup() {
	}

	public void execute() {
		getFactory().getStatistics().clear();

		try (Session s = getFactory().openSession()) {
			s.beginTransaction();
			try {
				// first load the store, making sure collection is not initialized
				Store store = s.get( Store.class, 1 );
				assertNotNull( store );
				assertFalse( Hibernate.isPropertyInitialized( store, "inventories" ) );
				assertEquals( 1, getFactory().getStatistics().getSessionOpenCount() );
				assertEquals( 0, getFactory().getStatistics().getSessionCloseCount() );

				// then clear session and try to initialize collection
				s.clear();
				assertNotNull( store );
				assertFalse( Hibernate.isPropertyInitialized( store, "inventories" ) );
				store.getInventories().size();
				assertTrue( Hibernate.isPropertyInitialized( store, "inventories" ) );
				// the extra Session is the temp Session needed to perform the init
				assertEquals( 2, getFactory().getStatistics().getSessionOpenCount() );
				assertEquals( 1, getFactory().getStatistics().getSessionCloseCount() );

				// clear Session again.  The collection should still be recognized as initialized from above
				s.clear();
				assertNotNull( store );
				assertTrue( Hibernate.isPropertyInitialized( store, "inventories" ) );
				assertEquals( 2, getFactory().getStatistics().getSessionOpenCount() );
				assertEquals( 1, getFactory().getStatistics().getSessionCloseCount() );

				// lets clear the Session again and this time reload the Store
				s.clear();
				store = s.get( Store.class, 1 );
				s.clear();
				assertNotNull( store );
				// collection should be back to uninitialized since we have a new entity instance
				assertFalse( Hibernate.isPropertyInitialized( store, "inventories" ) );
				assertEquals( 2, getFactory().getStatistics().getSessionOpenCount() );
				assertEquals( 1, getFactory().getStatistics().getSessionCloseCount() );
				store.getInventories().size();
				assertTrue( Hibernate.isPropertyInitialized( store, "inventories" ) );
				// the extra Session is the temp Session needed to perform the init
				assertEquals( 3, getFactory().getStatistics().getSessionOpenCount() );
				assertEquals( 2, getFactory().getStatistics().getSessionCloseCount() );

				// clear Session again.  The collection should still be recognized as initialized from above
				s.clear();
				assertNotNull( store );
				assertTrue( Hibernate.isPropertyInitialized( store, "inventories" ) );
				assertEquals( 3, getFactory().getStatistics().getSessionOpenCount() );
				assertEquals( 2, getFactory().getStatistics().getSessionCloseCount() );

				s.getTransaction().commit();
			}
			catch (Exception e) {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	protected void configure(Configuration cfg) {
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Store.class,
				Inventory.class,
				Product.class
		};
	}

}
