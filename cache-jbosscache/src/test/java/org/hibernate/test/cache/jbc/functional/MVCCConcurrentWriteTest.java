/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.jbc.functional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transaction;

import junit.framework.Test;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.jbc.JBossCacheRegionFactory;
import org.hibernate.cache.jbc.builder.SharedCacheInstanceManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.test.cache.jbc.functional.util.DualNodeConnectionProviderImpl;
import org.hibernate.test.cache.jbc.functional.util.DualNodeJtaTransactionManagerImpl;
import org.hibernate.test.cache.jbc.functional.util.DualNodeTestUtil;
import org.hibernate.test.cache.jbc.functional.util.DualNodeTransactionManagerLookup;
import org.hibernate.transaction.CMTTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nikita_tovstoles@mba.berkeley.edu
 */
public class MVCCConcurrentWriteTest extends CacheTestCaseBase {

    private static final String JBC_CONFIG = "org/hibernate/test/cache/jbc/functional/mvcc-treecache.xml";

    private static final Logger LOG = LoggerFactory.getLogger(MVCCConcurrentWriteTest.class);
    /**
     * when USER_COUNT==1, tests pass, when >4 tests fail
     */
    final private int USER_COUNT = 5;
    final private int ITERATION_COUNT = 150;
    final private int THINK_TIME_MILLIS = 10;
    final private long LAUNCH_INTERVAL_MILLIS = 10;
    final private Random random = new Random();
    /**
     * kill switch used to stop all users when one fails
     */
    private static volatile boolean TERMINATE_ALL_USERS = false;
    /**
     * collection of IDs of all customers participating in this test
     */
    private Set<Integer> customerIDs = new HashSet<Integer>();

    public MVCCConcurrentWriteTest(String x) {
        super(x);
    }

    protected Class<? extends RegionFactory> getCacheRegionFactory() {
        return JBossCacheRegionFactory.class;
    }    

    /**
     * Apply any region-factory specific configurations.
     * 
     * @param the Configuration to update.
     */
    protected void configureCacheFactory(Configuration cfg) {
        cfg.setProperty(SharedCacheInstanceManager.CACHE_RESOURCE_PROP, JBC_CONFIG);        
    }
    
    

    /**
     * test that DB can be queried
     * @throws java.lang.Exception
     */
    public void testPingDb() throws Exception {
        try {
            beginTx();
            getEnvironment().getSessionFactory().getCurrentSession().createQuery("from " + Customer.class.getName()).list();
        } catch (Exception e) {
            setRollbackOnly();
            fail("failed to query DB; exception=" + e);
        }
        finally {
           commitTx();
        }
    }

    @Override
    protected void prepareTest() throws Exception {
        super.prepareTest();
        TERMINATE_ALL_USERS = false;
    }

    @Override
    protected void cleanupTest() throws Exception {
        try {
            super.cleanupTest();

        } finally {
            cleanup();
        //DualNodeJtaTransactionManagerImpl.cleanupTransactions();
        //DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
        }
    }

    @Override
    public void configure(Configuration cfg) {
        super.configure(cfg);
        cfg.setProperty(DualNodeTestUtil.NODE_ID_PROP, DualNodeTestUtil.LOCAL);
    }

    @Override
    protected boolean getUseQueryCache() {
        return true;
    }

    @Override
    protected Class<?> getConnectionProviderClass() {
        return DualNodeConnectionProviderImpl.class;
    }

    @Override
    protected Class<?> getTransactionManagerLookupClass() {
        return DualNodeTransactionManagerLookup.class;
    }

    @Override
    protected Class<?> getTransactionFactoryClass() {
        return CMTTransactionFactory.class;
    }

    public void testSingleUser() throws Exception {
        //setup
        Customer customer = createCustomer(0);
        final Integer customerId = customer.getId();
        getCustomerIDs().add(customerId);

        assertNull("contact exists despite not being added", getFirstContact(customerId));

        //check that cache was hit
        SecondLevelCacheStatistics customerSlcs = getEnvironment().getSessionFactory().getStatistics().getSecondLevelCacheStatistics(
                getPrefixedRegionName(Customer.class.getName()));
        assertEquals(customerSlcs.getPutCount(), 1);
        assertEquals(customerSlcs.getElementCountInMemory(), 1);
        assertEquals(customerSlcs.getEntries().size(), 1);

        SecondLevelCacheStatistics contactsCollectionSlcs = getEnvironment().getSessionFactory().getStatistics().getSecondLevelCacheStatistics(
                getPrefixedRegionName(Customer.class.getName() + ".contacts"));
        assertEquals(1, contactsCollectionSlcs.getPutCount());
        assertEquals(1, contactsCollectionSlcs.getElementCountInMemory());
        assertEquals(1, contactsCollectionSlcs.getEntries().size());

        final Contact contact = addContact(customerId);
        assertNotNull("contact returned by addContact is null", contact);
        assertEquals("Customer.contacts cache was not invalidated after addContact",
                0, contactsCollectionSlcs.getElementCountInMemory());

        assertNotNull("Contact missing after successful add call", getFirstContact(customerId));


        //read everyone's contacts
        readEveryonesFirstContact();

        removeContact(customerId);
        assertNull("contact still exists after successful remove call", getFirstContact(customerId));

    }
    
    /**
     * This will fail until JBCACHE-1494 is done and integrated. Note that
     * having getUseQueryCache() return true will allows this to pass.
     * 
     * @throws Exception
     */
    public void testManyUsers() throws Exception {

        //setup - create users
        for (int i = 0; i < USER_COUNT; i++) {
            Customer customer = createCustomer(0);
            getCustomerIDs().add(customer.getId());
        }

        assertEquals("failed to create enough Customers", USER_COUNT, getCustomerIDs().size());

        final ExecutorService pool = Executors.newFixedThreadPool(USER_COUNT);
        CountDownLatch completionLatch = new CountDownLatch(USER_COUNT);
        
        Set<UserRunner> runners = new HashSet<UserRunner>();
        for (Integer customerId : getCustomerIDs()) {
            UserRunner r = new UserRunner(customerId, completionLatch);
            runners.add(r);
            pool.execute(r);
            LOG.info("launched " + r);
            Thread.sleep(LAUNCH_INTERVAL_MILLIS); //rampup
        }

        assertEquals("not all user threads launched", USER_COUNT, runners.size());
        
        boolean finishedInTime = completionLatch.await(10, TimeUnit.SECONDS);
        
        TERMINATE_ALL_USERS = true;
        
        if (!finishedInTime) { //timed out waiting for users to finish
            pool.shutdown();
            fail("Timed out waiting for user threads to finish. Their state at the time of forced shutdown: " + statusOfRunnersToString(runners));
        } else {
            //if here -> pool finished before timing out
            //check whether all runners suceeded
            boolean success = true;
            for (UserRunner r : runners) {
                if (!r.isSuccess()) {
                    success = false;
                    break;
                }
            }
            assertTrue("at least one UserRunner failed: " + statusOfRunnersToString(runners), success);
        }
    }

    public void cleanup() throws Exception {

        getCustomerIDs().clear();

        String deleteContactHQL = "delete from Contact";
        String deleteCustomerHQL = "delete from Customer";

        beginTx();
        try {
            Session session = getEnvironment().getSessionFactory().getCurrentSession();
            session.createQuery(deleteContactHQL).setFlushMode(FlushMode.AUTO).executeUpdate();
            session.createQuery(deleteCustomerHQL).setFlushMode(FlushMode.AUTO).executeUpdate();
        } catch (Exception e) {
            LOG.error("Caught exception in cleanup", e);
            setRollbackOnly();
        }
        finally {
           commitTx();
        }

    }

    private Customer createCustomer(int nameSuffix) throws Exception {
        Customer customer = null;
        beginTx();
        try {
            customer = new Customer();
            customer.setName("customer_" + nameSuffix);
            customer.setContacts(new HashSet<Contact>());

            getEnvironment().getSessionFactory().getCurrentSession().persist(customer);
        } catch (Exception e) {
            setRollbackOnly();
            throw e;
        }
        finally {
           commitTx();
        }
        return customer;
    }

    /**
     * delegate method since I'm trying to figure out which txManager to use
     * given that this test runs multiple threads (SimpleJtaTxMgrImpl isn't suited for that).
     *
     * What is needed is a thread-safe JTATransactionManager impl that can handle concurrent TXs
     * 
     * @throws java.lang.Exception
     */
    private void beginTx() throws Exception {
        DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestUtil.LOCAL).begin();
    }

    /**
     * @see #beginTx()
     * @throws java.lang.Exception
     */
    private void commitTx() throws Exception {
        DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestUtil.LOCAL).commit();
    }

//    /**
//     * @see #beginTx()
//     * @throws java.lang.Exception
//     */
//    private void rollbackTx() throws Exception {
//        DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestUtil.LOCAL).rollback();
//    }
    
    private void setRollbackOnly() throws Exception {
       Transaction tx = DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestUtil.LOCAL).getCurrentTransaction();
       if (tx != null) {
          tx.setRollbackOnly();
       }
    }

    /**
     * read first contact of every Customer participating in this test.
     * this forces concurrent cache writes of Customer.contacts Collection cache node
     * 
     * @return who cares
     * @throws java.lang.Exception
     */
    private void readEveryonesFirstContact() throws Exception {
        beginTx();
        try {
            for (Integer customerId : getCustomerIDs()) {
               
               if (TERMINATE_ALL_USERS) {
                  setRollbackOnly();
                  return;
               }
               
                final Customer customer = (Customer) getEnvironment().getSessionFactory().getCurrentSession().load(Customer.class, customerId);
                Set<Contact> contacts = customer.getContacts();
                if (!contacts.isEmpty()) {
                   contacts.iterator().next();
                }
            }
        } catch (Exception e) {
            setRollbackOnly();
            throw e;
        }
        finally {
           commitTx();
        }
    }

    /**
     * -load existing Customer
     * -get customer's contacts; return 1st one
     * 
     * @param customerId
     * @return first Contact or null if customer has none
     */
    private Contact getFirstContact(Integer customerId) throws Exception {
        assert customerId != null;
        
        Contact firstContact = null;
        beginTx();
        try {
            final Customer customer = (Customer) getEnvironment().getSessionFactory().getCurrentSession().load(Customer.class, customerId);
            Set<Contact> contacts = customer.getContacts();
            firstContact = contacts.isEmpty() ? null : contacts.iterator().next();
            
            if (TERMINATE_ALL_USERS)
               setRollbackOnly();
            
        } catch (Exception e) {
            setRollbackOnly();
            throw e;
        }
        finally {
           commitTx();
        }
        return firstContact;
    }

    /**
     * -load existing Customer     
     * -create a new Contact and add to customer's contacts
     * 
     * @param customerId
     * @return added Contact
     */
    private Contact addContact(Integer customerId) throws Exception {
        assert customerId != null;

        Contact contact = null;
        beginTx();
        try {
            final Customer customer = (Customer) getEnvironment().getSessionFactory().getCurrentSession().load(Customer.class, customerId);

            contact = new Contact();
            contact.setName("contact name");
            contact.setTlf("wtf is tlf?");

            contact.setCustomer(customer);
            customer.getContacts().add(contact);

            //assuming contact is persisted via cascade from customer
            
            if (TERMINATE_ALL_USERS)
               setRollbackOnly();
            
        } catch (Exception e) {
            setRollbackOnly();
            throw e;
        }
        finally {
           commitTx();
        }
        return contact;
    }

    /**
     * remove existing 'contact' from customer's list of contacts
     * 
     * @param contact contact to remove from customer's contacts
     * @param customerId
     * @throws IllegalStateException if customer does not own a contact
     */
    private void removeContact(Integer customerId) throws Exception {
        assert customerId != null;

        beginTx();
        try {
            Customer customer = (Customer) getEnvironment().getSessionFactory().getCurrentSession().load(Customer.class, customerId);
            Set<Contact> contacts = customer.getContacts();
            if (contacts.size() != 1) {
                throw new IllegalStateException("can't remove contact: customer id=" + customerId + " expected exactly 1 contact, " +
                        "actual count=" + contacts.size());
            }

            Contact contact = contacts.iterator().next();
            contacts.remove(contact);
            contact.setCustomer(null);

            //explicitly delete Contact because hbm has no 'DELETE_ORPHAN' cascade?
            //getEnvironment().getSessionFactory().getCurrentSession().delete(contact); //appears to not be needed

            //assuming contact is persisted via cascade from customer
            
            if (TERMINATE_ALL_USERS)
               setRollbackOnly();
            
        } catch (Exception e) {
            setRollbackOnly();
            throw e;
        }
        finally {
           commitTx();
        }
    }

    /**
     * @return the customerIDs
     */
    public Set<Integer> getCustomerIDs() {
        return customerIDs;
    }

    private String statusOfRunnersToString(Set<UserRunner> runners) {
        assert runners != null;

        StringBuilder sb = new StringBuilder("TEST CONFIG [userCount=" + USER_COUNT +
                ", iterationsPerUser=" + ITERATION_COUNT +
                ", thinkTimeMillis=" + THINK_TIME_MILLIS + "] " +
                " STATE of UserRunners: ");

        for (UserRunner r : runners) {
            sb.append(r.toString() + System.getProperty("line.separator"));
        }
        return sb.toString();
    }

    class UserRunner implements Runnable {
        final private CountDownLatch completionLatch;
        final private Integer customerId;
        private int completedIterations = 0;
        private Throwable causeOfFailure;

        public UserRunner(final Integer cId, CountDownLatch completionLatch) {
            assert cId != null;
            assert completionLatch != null;
            this.customerId = cId;
            this.completionLatch = completionLatch;
        }

        private boolean contactExists() throws Exception {
            return getFirstContact(customerId) != null;
        }

        public void run() {

            //name this thread for easier log tracing
            Thread.currentThread().setName("UserRunnerThread-" + getCustomerId());
            try {
                for (int i = 0; i < ITERATION_COUNT && !TERMINATE_ALL_USERS; i++) {

                    if (contactExists()) {
                        throw new IllegalStateException("contact already exists before add, customerId=" + customerId);
                    }

                    addContact(customerId);
                    
                    thinkRandomTime();
                    
                    if (!contactExists()) {
                        throw new IllegalStateException("contact missing after successful add, customerId=" + customerId);
                    }

                    thinkRandomTime();

                    //read everyone's contacts
                    readEveryonesFirstContact();

                    thinkRandomTime();

                    removeContact(customerId);

                    if (contactExists()) {
                        throw new IllegalStateException("contact still exists after successful remove call, customerId=" + customerId);
                    }

                    thinkRandomTime();

                    ++completedIterations;
                }

            } catch (Throwable t) {

                this.causeOfFailure = t;
                TERMINATE_ALL_USERS = true;

                //rollback current transaction if any
                //really should not happen since above methods all follow begin-commit-rollback pattern
//                try {
//                    if (DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestUtil.LOCAL).getTransaction() != null) {
//                        DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestUtil.LOCAL).rollback();
//                    }
//                } catch (SystemException ex) {
//                    throw new RuntimeException("failed to rollback tx", ex);
//                }
            }
            finally {
               this.completionLatch.countDown();
            }
        }

        public boolean isSuccess() {
            return ITERATION_COUNT == getCompletedIterations();
        }

        public int getCompletedIterations() {
            return completedIterations;
        }

        public Throwable getCauseOfFailure() {
            return causeOfFailure;
        }

        public Integer getCustomerId() {
            return customerId;
        }

        @Override
        public String toString() {
            return super.toString() +
                    "[customerId=" + getCustomerId() +
                    " iterationsCompleted=" + getCompletedIterations() +
                    " completedAll=" + isSuccess() +
                    " causeOfFailure=" + (this.causeOfFailure != null ? getStackTrace(causeOfFailure) : "") + "] ";
        }
    }


	public static String getStackTrace(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw, true );
		throwable.printStackTrace( pw );
		return sw.getBuffer().toString();
	}

    /**
     * sleep between 0 and THINK_TIME_MILLIS.
     * @throws RuntimeException if sleep is interrupted or TERMINATE_ALL_USERS flag was set to true i
n the meantime
     */
    private void thinkRandomTime() {
        try {
            Thread.sleep(random.nextInt(THINK_TIME_MILLIS));
        } catch (InterruptedException ex) {
            throw new RuntimeException("sleep interrupted", ex);
        }

        if (TERMINATE_ALL_USERS) {
            throw new RuntimeException("told to terminate (because a UserRunner had failed)");
        }
    }

    public static Test suite() {
        return new FunctionalTestClassTestSuite(MVCCConcurrentWriteTest.class);
    }
}

