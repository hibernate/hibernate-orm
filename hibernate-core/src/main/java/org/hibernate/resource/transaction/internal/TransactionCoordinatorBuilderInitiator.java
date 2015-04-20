/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
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
package org.hibernate.resource.transaction.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilderFactory;
import org.hibernate.resource.transaction.TransactionCoordinatorJtaBuilder;
import org.hibernate.resource.transaction.backend.store.internal.ResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Andrea Boriero
 */
public class TransactionCoordinatorBuilderInitiator implements StandardServiceInitiator<TransactionCoordinatorBuilder> {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger(TransactionCoordinatorBuilderInitiator.class);

	public static final TransactionCoordinatorBuilderInitiator INSTANCE = 	new TransactionCoordinatorBuilderInitiator();

	@Override
	public TransactionCoordinatorBuilder initiateService(
			Map configurationValues, ServiceRegistryImplementor registry) {
		final Object strategy = configurationValues.get( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY );

		if ( strategy == null ) {
			return new ResourceLocalTransactionCoordinatorBuilderImpl() ;
		}
//		TransactionCoordinatorJtaBuilder transactionCoordinatorJtaBuilder = TransactionCoordinatorBuilderFactory.INSTANCE
//				.forJta();

		TransactionCoordinatorBuilder transactionCoordinatorBuilder = registry.getService( StrategySelector.class )
				.resolveStrategy( TransactionCoordinatorBuilder.class, strategy );
		return transactionCoordinatorBuilder;
	}

	@Override
	public Class<TransactionCoordinatorBuilder> getServiceInitiated() {
		return TransactionCoordinatorBuilder.class;
	}
}
