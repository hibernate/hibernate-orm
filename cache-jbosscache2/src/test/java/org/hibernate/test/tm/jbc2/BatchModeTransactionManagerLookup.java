//$Id: DummyTransactionManagerLookup.java 5693 2005-02-13 01:59:07Z oneovthafew $
package org.hibernate.test.tm.jbc2;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.transaction.TransactionManagerLookup;
import org.jboss.cache.transaction.BatchModeTransactionManager;

/**
 * Uses the JBoss Cache BatchModeTransactionManager. Should not be used in
 * any tests that simulate usage of database connections.
 * 
 * @author Brian Stansberry
 */
public class BatchModeTransactionManagerLookup
    implements TransactionManagerLookup {

    public TransactionManager getTransactionManager(Properties props) throws HibernateException {
        try {
            return BatchModeTransactionManager.getInstance();
        }
        catch (Exception e) {
            throw new HibernateException("Failed getting BatchModeTransactionManager", e);
        }
    }

    public String getUserTransactionName() {
        throw new UnsupportedOperationException();
    }

}
