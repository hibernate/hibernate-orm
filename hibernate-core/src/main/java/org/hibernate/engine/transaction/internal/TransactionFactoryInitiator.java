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

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for {@link TransactionFactory} service.
 *
 * @author Steve Ebersole
 */
public class TransactionFactoryInitiator<T extends TransactionImplementor>
		implements StandardServiceInitiator<TransactionFactory> {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( TransactionFactoryInitiator.class );

	public static final TransactionFactoryInitiator INSTANCE = new TransactionFactoryInitiator();

	@Override
	@SuppressWarnings({"unchecked"})
	public Class<TransactionFactory> getServiceInitiated() {
		return TransactionFactory.class;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public TransactionFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object strategy = configurationValues.get( AvailableSettings.TRANSACTION_STRATEGY );

		if ( strategy == null ) {
			LOG.usingDefaultTransactionStrategy();
			return new JdbcTransactionFactory();
		}

		return registry.getService( StrategySelector.class ).resolveStrategy( TransactionFactory.class, strategy );
	}
}

