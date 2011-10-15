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
package org.hibernate.engine.transaction.internal;

import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.JtaTransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard instantiator for the standard {@link TransactionFactory} service.
 *
 * @author Steve Ebersole
 */
public class TransactionFactoryInitiator<T extends TransactionImplementor> implements BasicServiceInitiator<TransactionFactory> {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       TransactionFactoryInitiator.class.getName());

	public static final TransactionFactoryInitiator INSTANCE = new TransactionFactoryInitiator();

	@Override
	@SuppressWarnings( {"unchecked"})
	public Class<TransactionFactory> getServiceInitiated() {
		return TransactionFactory.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public TransactionFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object strategy = configurationValues.get( Environment.TRANSACTION_STRATEGY );
		if ( TransactionFactory.class.isInstance( strategy ) ) {
			return (TransactionFactory) strategy;
		}

		if ( strategy == null ) {
			LOG.usingDefaultTransactionStrategy();
			return new JdbcTransactionFactory();
		}

		final String strategyClassName = mapLegacyNames( strategy.toString() );
		LOG.transactionStrategy( strategyClassName );

		ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		try {
			return (TransactionFactory) classLoaderService.classForName( strategyClassName ).newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException( "Unable to instantiate specified TransactionFactory class [" + strategyClassName + "]", e );
		}
	}

	private String mapLegacyNames(String name) {
		if ( "org.hibernate.transaction.JDBCTransactionFactory".equals( name ) ) {
			return JdbcTransactionFactory.class.getName();
		}

		if ( "org.hibernate.transaction.JTATransactionFactory".equals( name ) ) {
			return JtaTransactionFactory.class.getName();
		}

		if ( "org.hibernate.transaction.CMTTransactionFactory".equals( name ) ) {
			return CMTTransactionFactory.class.getName();
		}

		return name;
	}
}

