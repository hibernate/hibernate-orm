/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.tm;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

/**
 * Hibernate transaction manager lookup class for Infinispan, so that
 * Hibernate's transaction manager can be hooked onto Infinispan.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class HibernateTransactionManagerLookup implements org.infinispan.transaction.lookup.TransactionManagerLookup {
	private final JtaPlatform jtaPlatform;

   /**
    * Transaction manager lookup constructor.
    *
    * @param settings for the Hibernate application
    * @param properties for the Hibernate application
    */
	public HibernateTransactionManagerLookup(SessionFactoryOptions settings, Properties properties) {
		this.jtaPlatform = settings != null ? settings.getServiceRegistry().getService( JtaPlatform.class ) : null;
	}

	@Override
	public TransactionManager getTransactionManager() throws Exception {
		return jtaPlatform == null ? null : jtaPlatform.retrieveTransactionManager();
	}

}
