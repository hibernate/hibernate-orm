/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.registry.selector.internal;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.JtaTransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;

/**
 * @author Steve Ebersole
 */
public class StrategySelectorBuilder {
	private static final Logger log = Logger.getLogger( StrategySelectorBuilder.class );

	private static class CustomRegistration<T> {
		private final Class<T> strategy;
		private final String name;
		private final Class<? extends T> implementation;

		private CustomRegistration(Class<T> strategy, String name, Class<? extends T> implementation) {
			this.strategy = strategy;
			this.name = name;
			this.implementation = implementation;
		}

		public void registerWith(StrategySelectorImpl strategySelector) {
			strategySelector.registerStrategyImplementor( strategy, name, implementation );
		}
	}

	private final List<CustomRegistration> customRegistrations = new ArrayList<CustomRegistration>();

	@SuppressWarnings("unchecked")
	public <T> void addCustomRegistration(Class<T> strategy, String name, Class<? extends T> implementation) {
		if ( !strategy.isInterface() ) {
			// not good form...
			log.debug( "Registering non-interface strategy implementation : " + strategy.getName()  );
		}

		if ( ! strategy.isAssignableFrom( implementation ) ) {
			throw new StrategySelectionException(
					"Implementation class [" + implementation.getName() + "] does not implement strategy interface ["
							+ strategy.getName() + "]"
			);
		}
		customRegistrations.add( new CustomRegistration( strategy, name, implementation ) );
	}

	public StrategySelector buildSelector(ClassLoaderServiceImpl classLoaderService) {
		StrategySelectorImpl strategySelector = new StrategySelectorImpl( classLoaderService );

		// build the baseline...
		addTransactionFactories( strategySelector );

		// todo : apply auto-discovered registrations

		// apply customizations
		for ( CustomRegistration customRegistration : customRegistrations ) {
			customRegistration.registerWith( strategySelector );
		}

		return strategySelector;
	}

	private void addTransactionFactories(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor( TransactionFactory.class, JdbcTransactionFactory.SHORT_NAME, JdbcTransactionFactory.class );
		strategySelector.registerStrategyImplementor( TransactionFactory.class, "org.hibernate.transaction.JDBCTransactionFactory", JdbcTransactionFactory.class );

		strategySelector.registerStrategyImplementor( TransactionFactory.class, JtaTransactionFactory.SHORT_NAME, JtaTransactionFactory.class );
		strategySelector.registerStrategyImplementor( TransactionFactory.class, "org.hibernate.transaction.JTATransactionFactory", JtaTransactionFactory.class );

		strategySelector.registerStrategyImplementor( TransactionFactory.class, CMTTransactionFactory.SHORT_NAME, CMTTransactionFactory.class );
		strategySelector.registerStrategyImplementor( TransactionFactory.class, "org.hibernate.transaction.CMTTransactionFactory", CMTTransactionFactory.class );
	}
}
