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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.util.ReflectHelper;

/**
 * Helper for creating {@link TransactionFactory} instances.
 *
 * @author Gavin King
 */
public final class TransactionFactoryFactory {

	private static final Logger log = LoggerFactory.getLogger( TransactionFactoryFactory.class );

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
			log.info( "Using default transaction strategy (direct JDBC transactions)" );
			return new JDBCTransactionFactory();
		}
		log.info( "Transaction strategy: " + strategyClassName );
		TransactionFactory factory;
		try {
			factory = ( TransactionFactory ) ReflectHelper.classForName( strategyClassName ).newInstance();
		}
		catch ( ClassNotFoundException e ) {
			log.error( "TransactionFactory class not found", e );
			throw new HibernateException( "TransactionFactory class not found: " + strategyClassName );
		}
		catch ( IllegalAccessException e ) {
			log.error( "Failed to instantiate TransactionFactory", e );
			throw new HibernateException( "Failed to instantiate TransactionFactory: " + e );
		}
		catch ( java.lang.InstantiationException e ) {
			log.error( "Failed to instantiate TransactionFactory", e );
			throw new HibernateException( "Failed to instantiate TransactionFactory: " + e );
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
