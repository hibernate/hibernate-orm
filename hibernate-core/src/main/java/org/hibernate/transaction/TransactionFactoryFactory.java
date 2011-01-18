/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 *
 */
package org.hibernate.transaction;

import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.Logger;
import org.hibernate.cfg.Environment;
import org.hibernate.util.ReflectHelper;

/**
 * Helper for creating {@link TransactionFactory} instances.
 *
 * @author Gavin King
 */
public final class TransactionFactoryFactory {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                TransactionFactoryFactory.class.getPackage().getName());

	/**
	 * Create an appropriate transaction factory based on the given configuration
	 * properties.
	 *
	 * @param transactionProps transaction properties
	 *
	 * @return The appropriate transaction factory.
	 *
	 * @throws HibernateException Indicates a problem creating the appropriate
	 * transaction factory.
	 */
	public static TransactionFactory buildTransactionFactory(Properties transactionProps) throws HibernateException {
		String strategyClassName = transactionProps.getProperty( Environment.TRANSACTION_STRATEGY );
		if ( strategyClassName == null ) {
            LOG.usingDefaultTransactionStrategy();
			return new JDBCTransactionFactory();
		}
        LOG.transactionStrategy(strategyClassName);
		TransactionFactory factory;
		try {
			factory = ( TransactionFactory ) ReflectHelper.classForName( strategyClassName ).newInstance();
		}
		catch ( ClassNotFoundException e ) {
            LOG.error(LOG.transactionFactoryClassNotFound(), e);
            throw new HibernateException(LOG.transactionFactoryClassNotFound(strategyClassName));
		}
		catch ( IllegalAccessException e ) {
            LOG.error(LOG.unableToInstantiateTransactionFactory(), e);
            throw new HibernateException(LOG.unableToInstantiateTransactionFactory(e));
		}
		catch ( java.lang.InstantiationException e ) {
            LOG.error(LOG.unableToInstantiateTransactionFactory(), e);
            throw new HibernateException(LOG.unableToInstantiateTransactionFactory(e));
		}
		factory.configure( transactionProps );
		return factory;
	}

	/**
	 * Disallow instantiation
	 */
	private TransactionFactoryFactory() {
	}
}
