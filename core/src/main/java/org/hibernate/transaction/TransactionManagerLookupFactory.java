//$Id: TransactionManagerLookupFactory.java 11412 2007-04-17 14:38:51Z max.andersen@jboss.com $
package org.hibernate.transaction;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.util.ReflectHelper;

/**
 * @author Gavin King
 */
public final class TransactionManagerLookupFactory {

	private static final Log log = LogFactory.getLog(TransactionManagerLookupFactory.class);

	private TransactionManagerLookupFactory() {}

	public static final TransactionManager getTransactionManager(Properties props) throws HibernateException {
		log.info("obtaining TransactionManager");
		return getTransactionManagerLookup(props).getTransactionManager(props);
	}

	public static final TransactionManagerLookup getTransactionManagerLookup(Properties props) throws HibernateException {

		String tmLookupClass = props.getProperty(Environment.TRANSACTION_MANAGER_STRATEGY);
		if (tmLookupClass==null) {
			log.info("No TransactionManagerLookup configured (in JTA environment, use of read-write or transactional second-level cache is not recommended)");
			return null;
		}
		else {

			log.info("instantiating TransactionManagerLookup: " + tmLookupClass);

			try {
				TransactionManagerLookup lookup = (TransactionManagerLookup) ReflectHelper.classForName(tmLookupClass).newInstance();
				log.info("instantiated TransactionManagerLookup");
				return lookup;
			}
			catch (Exception e) {
				log.error("Could not instantiate TransactionManagerLookup", e);
				throw new HibernateException("Could not instantiate TransactionManagerLookup '" + tmLookupClass + "'");
			}
		}
	}
}
