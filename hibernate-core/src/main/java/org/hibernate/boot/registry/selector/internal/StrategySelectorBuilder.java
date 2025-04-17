/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategyLegacy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.StandardNamingStrategy;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonIntegration;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.hibernate.type.format.jackson.JacksonOsonFormatMapper;
import org.hibernate.type.format.jackson.JacksonXmlFormatMapper;
import org.hibernate.type.format.jaxb.JaxbXmlFormatMapper;
import org.hibernate.type.format.jakartajson.JsonBJsonFormatMapper;

import org.jboss.logging.Logger;

/**
 * Builder for {@link StrategySelector} instances.
 *
 * @author Steve Ebersole
 */
public class StrategySelectorBuilder {
	private static final Logger log = Logger.getLogger( StrategySelectorBuilder.class );

	private final List<StrategyRegistration<?>> explicitStrategyRegistrations = new ArrayList<>();

	/**
	 * Adds an explicit (as opposed to discovered) strategy registration.
	 *
	 * @param strategy The strategy
	 * @param implementation The strategy implementation
	 * @param name The registered name
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 */
	public <T> void addExplicitStrategyRegistration(Class<T> strategy, Class<? extends T> implementation, String name) {
		addExplicitStrategyRegistration( new SimpleStrategyRegistrationImpl<>( strategy, implementation, name ) );
	}

	/**
	 * Adds an explicit (as opposed to discovered) strategy registration.
	 *
	 * @param strategyRegistration The strategy implementation registration.
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 */
	public <T> void addExplicitStrategyRegistration(StrategyRegistration<T> strategyRegistration) {
		if ( !strategyRegistration.getStrategyRole().isInterface() ) {
			// not good form...
			if ( log.isDebugEnabled() ) {
				log.debugf( "Registering non-interface strategy : %s", strategyRegistration.getStrategyRole().getName() );
			}
		}

		if ( ! strategyRegistration.getStrategyRole().isAssignableFrom( strategyRegistration.getStrategyImplementation() ) ) {
			throw new StrategySelectionException(
					"Implementation class [" + strategyRegistration.getStrategyImplementation().getName()
							+ "] does not implement strategy interface ["
							+ strategyRegistration.getStrategyRole().getName() + "]"
			);
		}
		explicitStrategyRegistrations.add( strategyRegistration );
	}

	/**
	 * Builds the selector.
	 *
	 * @param classLoaderService The class loading service used to (attempt to) resolve any un-registered
	 * strategy implementations.
	 *
	 * @return The selector.
	 */
	public StrategySelector buildSelector(ClassLoaderService classLoaderService) {
		final StrategySelectorImpl strategySelector = new StrategySelectorImpl( classLoaderService );

		// build the baseline...
		strategySelector.registerStrategyLazily(
				Dialect.class,
				new AggregatedDialectSelector( classLoaderService.loadJavaServices( DialectSelector.class ) )
		);
		strategySelector.registerStrategyLazily( JtaPlatform.class, new DefaultJtaPlatformSelector() );
		addTransactionCoordinatorBuilders( strategySelector );
		addSqmMultiTableInsertStrategies( strategySelector );
		addSqmMultiTableMutationStrategies( strategySelector );
		addImplicitNamingStrategies( strategySelector );
		addColumnOrderingStrategies( strategySelector );
		addCacheKeysFactories( strategySelector );
		addJsonFormatMappers( strategySelector );
		addXmlFormatMappers( strategySelector );

		// apply auto-discovered registrations
		for ( StrategyRegistrationProvider provider : classLoaderService.loadJavaServices( StrategyRegistrationProvider.class ) ) {
			for ( StrategyRegistration<?> discoveredStrategyRegistration : provider.getStrategyRegistrations() ) {
				applyFromStrategyRegistration( strategySelector, discoveredStrategyRegistration );
			}
		}

		// apply customizations
		for ( StrategyRegistration<?> explicitStrategyRegistration : explicitStrategyRegistrations ) {
			applyFromStrategyRegistration( strategySelector, explicitStrategyRegistration );
		}

		return strategySelector;
	}

	private <T> void applyFromStrategyRegistration(StrategySelectorImpl strategySelector, StrategyRegistration<T> strategyRegistration) {
		for ( String name : strategyRegistration.getSelectorNames() ) {
			strategySelector.registerStrategyImplementor(
					strategyRegistration.getStrategyRole(),
					name,
					strategyRegistration.getStrategyImplementation()
			);
		}
	}

	private static void addTransactionCoordinatorBuilders(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor(
				TransactionCoordinatorBuilder.class,
				JdbcResourceLocalTransactionCoordinatorBuilderImpl.SHORT_NAME,
				JdbcResourceLocalTransactionCoordinatorBuilderImpl.class
		);
		strategySelector.registerStrategyImplementor(
				TransactionCoordinatorBuilder.class,
				JtaTransactionCoordinatorBuilderImpl.SHORT_NAME,
				JtaTransactionCoordinatorBuilderImpl.class
		);

		// add the legacy TransactionFactory impl names...
		strategySelector.registerStrategyImplementor(
				TransactionCoordinatorBuilder.class,
				"org.hibernate.transaction.JDBCTransactionFactory",
				JdbcResourceLocalTransactionCoordinatorBuilderImpl.class
		);
		strategySelector.registerStrategyImplementor(
				TransactionCoordinatorBuilder.class,
				"org.hibernate.transaction.JTATransactionFactory",
				JtaTransactionCoordinatorBuilderImpl.class
		);
		strategySelector.registerStrategyImplementor(
				TransactionCoordinatorBuilder.class,
				"org.hibernate.transaction.CMTTransactionFactory",
				JtaTransactionCoordinatorBuilderImpl.class
		);
	}

	private static void addSqmMultiTableInsertStrategies(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor(
				SqmMultiTableInsertStrategy.class,
				CteInsertStrategy.SHORT_NAME,
				CteInsertStrategy.class
		);
		strategySelector.registerStrategyImplementor(
				SqmMultiTableInsertStrategy.class,
				GlobalTemporaryTableInsertStrategy.SHORT_NAME,
				GlobalTemporaryTableInsertStrategy.class
		);
		strategySelector.registerStrategyImplementor(
				SqmMultiTableInsertStrategy.class,
				LocalTemporaryTableInsertStrategy.SHORT_NAME,
				LocalTemporaryTableInsertStrategy.class
		);
		strategySelector.registerStrategyImplementor(
				SqmMultiTableInsertStrategy.class,
				PersistentTableInsertStrategy.SHORT_NAME,
				PersistentTableInsertStrategy.class
		);
	}

	private static void addSqmMultiTableMutationStrategies(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor(
				SqmMultiTableMutationStrategy.class,
				CteMutationStrategy.SHORT_NAME,
				CteMutationStrategy.class
		);
		strategySelector.registerStrategyImplementor(
				SqmMultiTableMutationStrategy.class,
				GlobalTemporaryTableMutationStrategy.SHORT_NAME,
				GlobalTemporaryTableMutationStrategy.class
		);
		strategySelector.registerStrategyImplementor(
				SqmMultiTableMutationStrategy.class,
				LocalTemporaryTableMutationStrategy.SHORT_NAME,
				LocalTemporaryTableMutationStrategy.class
		);
		strategySelector.registerStrategyImplementor(
				SqmMultiTableMutationStrategy.class,
				PersistentTableMutationStrategy.SHORT_NAME,
				PersistentTableMutationStrategy.class
		);
	}

	private static void addImplicitNamingStrategies(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor(
				ImplicitNamingStrategy.class,
				"default",
				ImplicitNamingStrategyJpaCompliantImpl.class
		);
		strategySelector.registerStrategyImplementor(
				ImplicitNamingStrategy.class,
				"jpa",
				ImplicitNamingStrategyJpaCompliantImpl.class
		);
		strategySelector.registerStrategyImplementor(
				ImplicitNamingStrategy.class,
				"legacy-jpa",
				ImplicitNamingStrategyLegacyJpaImpl.class
		);
		strategySelector.registerStrategyImplementor(
				ImplicitNamingStrategy.class,
				"legacy-hbm",
				ImplicitNamingStrategyLegacyHbmImpl.class
		);
		strategySelector.registerStrategyImplementor(
				ImplicitNamingStrategy.class,
				"component-path",
				ImplicitNamingStrategyComponentPathImpl.class
		);


		strategySelector.registerStrategyImplementor(
				ImplicitDatabaseObjectNamingStrategy.class,
				StandardNamingStrategy.STRATEGY_NAME,
				StandardNamingStrategy.class
		);

		strategySelector.registerStrategyImplementor(
				ImplicitDatabaseObjectNamingStrategy.class,
				SingleNamingStrategy.STRATEGY_NAME,
				SingleNamingStrategy.class
		);

		strategySelector.registerStrategyImplementor(
				ImplicitDatabaseObjectNamingStrategy.class,
				LegacyNamingStrategy.STRATEGY_NAME,
				LegacyNamingStrategy.class
		);
	}

	private static void addColumnOrderingStrategies(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor(
				ColumnOrderingStrategy.class,
				"default",
				ColumnOrderingStrategyStandard.class
		);
		strategySelector.registerStrategyImplementor(
				ColumnOrderingStrategy.class,
				"legacy",
				ColumnOrderingStrategyLegacy.class
		);
	}

	private static void addCacheKeysFactories(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor(
			CacheKeysFactory.class,
			DefaultCacheKeysFactory.SHORT_NAME,
			DefaultCacheKeysFactory.class
		);
		strategySelector.registerStrategyImplementor(
			CacheKeysFactory.class,
			SimpleCacheKeysFactory.SHORT_NAME,
			SimpleCacheKeysFactory.class
		);
	}

	private static void addJsonFormatMappers(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor(
				FormatMapper.class,
				JsonBJsonFormatMapper.SHORT_NAME,
				JsonBJsonFormatMapper.class
		);
		strategySelector.registerStrategyImplementor(
				FormatMapper.class,
				JacksonJsonFormatMapper.SHORT_NAME,
				JacksonJsonFormatMapper.class
		);
		if ( JacksonIntegration.isJacksonOsonExtensionAvailable() ) {
			strategySelector.registerStrategyImplementor(
					FormatMapper.class,
					JacksonOsonFormatMapper.SHORT_NAME,
					JacksonOsonFormatMapper.class
			);
		}
	}

	private static void addXmlFormatMappers(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor(
				FormatMapper.class,
				JacksonXmlFormatMapper.SHORT_NAME,
				JacksonXmlFormatMapper.class
		);
		strategySelector.registerStrategyImplementor(
				FormatMapper.class,
				JaxbXmlFormatMapper.SHORT_NAME,
				JaxbXmlFormatMapper.class
		);
	}
}
