package org.hibernate.cache.impl.jbc;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.hibernate.transaction.TransactionManagerLookup;

/**
 * An adapter between JBossCache's notion of a TM lookup and Hibernate's.
 *
 * @author Steve Ebersole
 */
public class TransactionManagerLookupAdaptor implements org.jboss.cache.transaction.TransactionManagerLookup {
	private final TransactionManagerLookup tml;
	private final Properties props;

	TransactionManagerLookupAdaptor(TransactionManagerLookup tml, Properties props) {
		this.tml = tml;
		this.props = props;
	}

	public TransactionManager getTransactionManager() throws Exception {
		return tml.getTransactionManager( props );
	}
}
