/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.transaction.jta.platform.internal.AtomikosJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.BitronixJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.BorlandEnterpriseServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JOTMJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JOnASJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JRun4JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.OC4JJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.OrionJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.ResinJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.SapNetWeaverJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.SunOneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WeblogicJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.persistent.PersistentTableBulkIdStrategy;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

import org.jboss.logging.Logger;

/**
 * Builder for StrategySelector instances.
 *
 * @author Steve Ebersole
 */
public class StrategySelectorBuilder {
	private static final Logger log = Logger.getLogger( StrategySelectorBuilder.class );

	private final List<StrategyRegistration> explicitStrategyRegistrations = new ArrayList<StrategyRegistration>();

	/**
	 * Adds an explicit (as opposed to discovered) strategy registration.
	 *
	 * @param strategy The strategy
	 * @param implementation The strategy implementation
	 * @param name The registered name
	 * @param <T> The type of the strategy.  Used to make sure that the strategy and implementation are type
	 * compatible.
	 */
	@SuppressWarnings("unchecked")
	public <T> void addExplicitStrategyRegistration(Class<T> strategy, Class<? extends T> implementation, String name) {
		addExplicitStrategyRegistration( new SimpleStrategyRegistrationImpl<T>( strategy, implementation, name ) );
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
			log.debug( "Registering non-interface strategy : " + strategyRegistration.getStrategyRole().getName()  );
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
		strategySelector.registerStrategyLazily( Dialect.class, new DefaultDialectSelector() );
		addJtaPlatforms( strategySelector );
		addTransactionCoordinatorBuilders( strategySelector );
		addMultiTableBulkIdStrategies( strategySelector );
		addImplicitNamingStrategies( strategySelector );
		addCacheKeysFactories( strategySelector );

		// apply auto-discovered registrations
		for ( StrategyRegistrationProvider provider : classLoaderService.loadJavaServices( StrategyRegistrationProvider.class ) ) {
			for ( StrategyRegistration discoveredStrategyRegistration : provider.getStrategyRegistrations() ) {
				applyFromStrategyRegistration( strategySelector, discoveredStrategyRegistration );
			}
		}

		// apply customizations
		for ( StrategyRegistration explicitStrategyRegistration : explicitStrategyRegistrations ) {
			applyFromStrategyRegistration( strategySelector, explicitStrategyRegistration );
		}

		return strategySelector;
	}

	@SuppressWarnings("unchecked")
	private <T> void applyFromStrategyRegistration(StrategySelectorImpl strategySelector, StrategyRegistration<T> strategyRegistration) {
		for ( String name : strategyRegistration.getSelectorNames() ) {
			strategySelector.registerStrategyImplementor(
					strategyRegistration.getStrategyRole(),
					name,
					strategyRegistration.getStrategyImplementation()
			);
		}
	}

	private void addJtaPlatforms(StrategySelectorImpl strategySelector) {
		addJtaPlatforms(
				strategySelector,
				AtomikosJtaPlatform.class,
				"Atomikos",
				"org.hibernate.service.jta.platform.internal.AtomikosJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				BorlandEnterpriseServerJtaPlatform.class,
				"Borland",
				"org.hibernate.service.jta.platform.internal.BorlandEnterpriseServerJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				BitronixJtaPlatform.class,
				"Bitronix",
				"org.hibernate.service.jta.platform.internal.BitronixJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JBossAppServerJtaPlatform.class,
				"JBossAS",
				"org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JBossStandAloneJtaPlatform.class,
				"JBossTS",
				"org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JOnASJtaPlatform.class,
				"JOnAS",
				"org.hibernate.service.jta.platform.internal.JOnASJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JOTMJtaPlatform.class,
				"JOTM",
				"org.hibernate.service.jta.platform.internal.JOTMJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JRun4JtaPlatform.class,
				"JRun4",
				"org.hibernate.service.jta.platform.internal.JRun4JtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				OC4JJtaPlatform.class,
				"OC4J",
				"org.hibernate.service.jta.platform.internal.OC4JJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				OrionJtaPlatform.class,
				"Orion",
				"org.hibernate.service.jta.platform.internal.OrionJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				ResinJtaPlatform.class,
				"Resin",
				"org.hibernate.service.jta.platform.internal.ResinJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				SapNetWeaverJtaPlatform.class,
				"SapNetWeaver",
				"org.hibernate.service.jta.platform.internal.SapNetWeaverJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				SunOneJtaPlatform.class,
				"SunOne",
				"org.hibernate.service.jta.platform.internal.SunOneJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				WeblogicJtaPlatform.class,
				"Weblogic",
				"org.hibernate.service.jta.platform.internal.WeblogicJtaPlatform"
		);
		
		addJtaPlatforms(
				strategySelector,
				WebSphereLibertyJtaPlatform.class,
				"WebSphereLiberty",
				"org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				WebSphereJtaPlatform.class,
				"WebSphere",
				"org.hibernate.service.jta.platform.internal.WebSphereJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				WebSphereExtendedJtaPlatform.class,
				"WebSphereExtended",
				"org.hibernate.service.jta.platform.internal.WebSphereExtendedJtaPlatform"
		);
	}

	private void addJtaPlatforms(StrategySelectorImpl strategySelector, Class<? extends JtaPlatform> impl, String... names) {
		for ( String name : names ) {
			strategySelector.registerStrategyImplementor( JtaPlatform.class, name, impl );
		}
	}

	private void addTransactionCoordinatorBuilders(StrategySelectorImpl strategySelector) {
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

	private void addMultiTableBulkIdStrategies(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor(
				MultiTableBulkIdStrategy.class,
				PersistentTableBulkIdStrategy.SHORT_NAME,
				PersistentTableBulkIdStrategy.class
		);
		strategySelector.registerStrategyImplementor(
				MultiTableBulkIdStrategy.class,
				GlobalTemporaryTableBulkIdStrategy.SHORT_NAME,
				GlobalTemporaryTableBulkIdStrategy.class
		);
		strategySelector.registerStrategyImplementor(
				MultiTableBulkIdStrategy.class,
				LocalTemporaryTableBulkIdStrategy.SHORT_NAME,
				LocalTemporaryTableBulkIdStrategy.class
		);
	}

	private void addImplicitNamingStrategies(StrategySelectorImpl strategySelector) {
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
	}

	private void addCacheKeysFactories(StrategySelectorImpl strategySelector) {
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
}
