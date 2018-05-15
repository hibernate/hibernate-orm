/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.bytecode.enhancement.ondemandload;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.test.bytecode.enhancement.merge.MergeEnhancedEntityTest;

import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-10055" )
@RunWith( BytecodeEnhancerRunner.class )
public class OnDemandLoadTest extends BaseCoreFunctionalTestCase {

    @Override
    public Class[] getAnnotatedClasses() {
        return new Class[]{Store.class, Inventory.class, Product.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
        configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            Store store = new Store( 1L ).setName( "Acme Super Outlet" );
            s.persist( store );

            Product product = new Product( "007" ).setName( "widget" ).setDescription( "FooBar" );
            s.persist( product );

            store.addInventoryProduct( product ).setQuantity( 10L ).setStorePrice( new BigDecimal( 500 ) );
        } );
    }

    @Test
    public void testClosedSession() {
        sessionFactory().getStatistics().clear();
        Store[] store = new Store[1];

        doInHibernate( this::sessionFactory, s -> {
            // first load the store, making sure it is not initialized
            store[0] = s.load( Store.class, 1L );
            assertNotNull( store[0] );
            assertFalse( isPropertyInitialized( store[0], "inventories" ) );

            assertEquals( 1, sessionFactory().getStatistics().getSessionOpenCount() );
            assertEquals( 0, sessionFactory().getStatistics().getSessionCloseCount() );
        } );

        assertEquals( 1, sessionFactory().getStatistics().getSessionOpenCount() );
        assertEquals( 1, sessionFactory().getStatistics().getSessionCloseCount() );

        store[0].getInventories();
        assertTrue( isPropertyInitialized( store[0], "inventories" ) );

        assertEquals( 2, sessionFactory().getStatistics().getSessionOpenCount() );
        assertEquals( 2, sessionFactory().getStatistics().getSessionCloseCount() );
    }

    @Test
    public void testClearedSession() {
        sessionFactory().getStatistics().clear();

        doInHibernate( this::sessionFactory, s -> {
            // first load the store, making sure collection is not initialized
            Store store = s.get( Store.class, 1L );
            assertNotNull( store );
            assertFalse( isPropertyInitialized( store, "inventories" ) );
            assertEquals( 1, sessionFactory().getStatistics().getSessionOpenCount() );
            assertEquals( 0, sessionFactory().getStatistics().getSessionCloseCount() );

            // then clear session and try to initialize collection
            s.clear();
            assertNotNull( store );
            assertFalse( isPropertyInitialized( store, "inventories" ) );
            store.getInventories().size();
            assertTrue( isPropertyInitialized( store, "inventories" ) );

            // the extra Session is the temp Session needed to perform the init
            assertEquals( 2, sessionFactory().getStatistics().getSessionOpenCount() );
            assertEquals( 1, sessionFactory().getStatistics().getSessionCloseCount() );

            // clear Session again.  The collection should still be recognized as initialized from above
            s.clear();
            assertNotNull( store );
            assertTrue( isPropertyInitialized( store, "inventories" ) );
            assertEquals( 2, sessionFactory().getStatistics().getSessionOpenCount() );
            assertEquals( 1, sessionFactory().getStatistics().getSessionCloseCount() );

            // lets clear the Session again and this time reload the Store
            s.clear();
            store = s.get( Store.class, 1L );
            s.clear();
            assertNotNull( store );

            // collection should be back to uninitialized since we have a new entity instance
            assertFalse( isPropertyInitialized( store, "inventories" ) );
            assertEquals( 2, sessionFactory().getStatistics().getSessionOpenCount() );
            assertEquals( 1, sessionFactory().getStatistics().getSessionCloseCount() );
            store.getInventories().size();
            assertTrue( isPropertyInitialized( store, "inventories" ) );

            // the extra Session is the temp Session needed to perform the init
            assertEquals( 3, sessionFactory().getStatistics().getSessionOpenCount() );
            assertEquals( 2, sessionFactory().getStatistics().getSessionCloseCount() );

            // clear Session again.  The collection should still be recognized as initialized from above
            s.clear();
            assertNotNull( store );
            assertTrue( isPropertyInitialized( store, "inventories" ) );
            assertEquals( 3, sessionFactory().getStatistics().getSessionOpenCount() );
            assertEquals( 2, sessionFactory().getStatistics().getSessionCloseCount() );
        } );
    }

    @After
    public void cleanup() throws Exception {
        doInHibernate( this::sessionFactory, s -> {
            Store store = s.find( Store.class, 1L );
            s.delete( store );

            Product product= s.find( Product.class, "007" );
            s.delete( product );
        } );
    }

    // --- //

    @Entity
    @Table( name = "STORE" )
    private static class Store {
        @Id
        Long id;

        String name;

        @OneToMany( mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
        List<Inventory> inventories = new ArrayList<>();

        @Version
        Integer version;

        Store() {
        }

        Store(long id) {
            this.id = id;
        }

        Store setName(String name) {
            this.name = name;
            return this;
        }

        Inventory addInventoryProduct(Product product) {
            Inventory inventory = new Inventory( this, product );
            inventories.add( inventory );
            return inventory;
        }

        public List<Inventory> getInventories() {
            return Collections.unmodifiableList( inventories );
        }
    }

    @Entity
    @Table( name = "INVENTORY" )
    private static class Inventory {

        @Id
        @GeneratedValue
        @GenericGenerator( name = "increment", strategy = "increment" )
        Long id = -1L;

        @ManyToOne
        @JoinColumn( name = "STORE_ID" )
        Store store;

        @ManyToOne
        @JoinColumn( name = "PRODUCT_ID" )
        Product product;

        Long quantity;

        BigDecimal storePrice;

        public Inventory() {
        }

        public Inventory(Store store, Product product) {
            this.store = store;
            this.product = product;
        }

        Inventory setStore(Store store) {
            this.store = store;
            return this;
        }

        Inventory setProduct(Product product) {
            this.product = product;
            return this;
        }

        Inventory setQuantity(Long quantity) {
            this.quantity = quantity;
            return this;
        }

        Inventory setStorePrice(BigDecimal storePrice) {
            this.storePrice = storePrice;
            return this;
        }
    }

    @Entity
    @Table( name = "PRODUCT" )
    private static class Product {
        @Id
        String id;

        String name;

        String description;

        BigDecimal msrp;

        @Version
        Long version;

        Product() {
        }

        Product(String id) {
            this.id = id;
        }

        Product setName(String name) {
            this.name = name;
            return this;
        }

        Product setDescription(String description) {
            this.description = description;
            return this;
        }

        Product setMsrp(BigDecimal msrp) {
            this.msrp = msrp;
            return this;
        }
    }
}
