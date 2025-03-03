/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.boot.registry.selector.internal.StrategySelectorBuilder;
import org.hibernate.integrator.internal.IntegratorServiceImpl;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.ServiceRegistry;

/**
 * Builder for {@link BootstrapServiceRegistry} instances.
 * <p>
 * An instance of this class may be obtained simply by
 * {@linkplain #BootstrapServiceRegistryBuilder() instantiation}. Then a new
 * {@code BootstrapServiceRegistry} may be obtained by calling {@link #build()}.
 * It should be later destroyed by calling {@link #destroy(ServiceRegistry)}.
 * Alternatively, {@linkplain #enableAutoClose() auto-close} may be enabled.
 * <p>
 * Provides a registry of services needed for most operations.
 * Manages a {@link ClassLoaderService}, a set of {@link Integrator}s, and a
 * {@link StrategySelectorBuilder} responsible for creation and management
 * of {@link org.hibernate.boot.registry.selector.spi.StrategySelector}s.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 *
 * @see StandardServiceRegistryBuilder
 */
public class BootstrapServiceRegistryBuilder {
	private final LinkedHashSet<Integrator> providedIntegrators = new LinkedHashSet<>();

	private List<ClassLoader> providedClassLoaders;
	private ClassLoaderService providedClassLoaderService;
	private final StrategySelectorBuilder strategySelectorBuilder = new StrategySelectorBuilder();
	private TcclLookupPrecedence tcclLookupPrecedence = TcclLookupPrecedence.AFTER;

	private boolean autoCloseRegistry = true;

	public BootstrapServiceRegistryBuilder() {
	}

	/**
	 * Add an {@link Integrator} to be applied to the bootstrap registry.
	 *
	 * @param integrator The integrator to add.
	 *
	 * @return {@code this}, for method chaining
	 */
	public BootstrapServiceRegistryBuilder applyIntegrator(Integrator integrator) {
		providedIntegrators.add( integrator );
		return this;
	}

	/**
	 * Adds a provided {@link ClassLoader} for use in classloading and resource lookup.
	 *
	 * @param classLoader The class loader to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public BootstrapServiceRegistryBuilder applyClassLoader(ClassLoader classLoader) {
		if ( providedClassLoaders == null ) {
			providedClassLoaders = new ArrayList<>();
		}
		providedClassLoaders.add( classLoader );
		return this;
	}

	/**
	 * Defines when the lookup in the thread context {@code ClassLoader} is done.
	 *
	 * @param precedence The lookup precedence
	 */
	public void applyTcclLookupPrecedence(TcclLookupPrecedence precedence) {
		tcclLookupPrecedence = precedence;
	}

	/**
	 * Adds a provided {@link ClassLoaderService} for use in classloading and resource lookup.
	 *
	 * @param classLoaderService The class loader service to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public BootstrapServiceRegistryBuilder applyClassLoaderService(ClassLoaderService classLoaderService) {
		providedClassLoaderService = classLoaderService;
		return this;
	}

	/**
	 * Applies a named strategy implementation to the bootstrap registry.
	 *
	 * @param strategy The strategy
	 * @param name The registered name
	 * @param implementation The strategy implementation Class
	 * @param <T> Defines the strategy type and makes sure that the strategy and implementation
	 *            are of compatible types.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.boot.registry.selector.spi.StrategySelector#registerStrategyImplementor(Class, String, Class)
	 */
	public <T> BootstrapServiceRegistryBuilder applyStrategySelector(Class<T> strategy, String name, Class<? extends T> implementation) {
		this.strategySelectorBuilder.addExplicitStrategyRegistration( strategy, implementation, name );
		return this;
	}

	/**
	 * Applies one or more strategy selectors announced as available by the passed announcer.
	 *
	 * @param strategyRegistrationProvider A provider for one or more available selectors
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.boot.registry.selector.spi.StrategySelector#registerStrategyImplementor(Class, String, Class)
	 */
	public BootstrapServiceRegistryBuilder applyStrategySelectors(StrategyRegistrationProvider strategyRegistrationProvider) {
		for ( StrategyRegistration<?> strategyRegistration : strategyRegistrationProvider.getStrategyRegistrations() ) {
			this.strategySelectorBuilder.addExplicitStrategyRegistration( strategyRegistration );
		}
		return this;
	}

	/**
	 * By default, when a {@link ServiceRegistry} is no longer referenced by
	 * any other registries as a parent it will be closed.
	 * <p>
	 * Some applications that explicitly build "shared registries" may want
	 * to circumvent that behavior.
	 * <p>
	 * This method indicates that the registry being built should not be
	 * automatically closed.  The caller agrees to take responsibility to
	 * close it themselves.
	 *
	 * @return this, for method chaining
	 */
	public BootstrapServiceRegistryBuilder disableAutoClose() {
		this.autoCloseRegistry = false;
		return this;
	}

	/**
	 * See the discussion on {@link #disableAutoClose}. This method enables the auto-closing.
	 *
	 * @return this, for method chaining
	 */
	public BootstrapServiceRegistryBuilder enableAutoClose() {
		this.autoCloseRegistry = true;
		return this;
	}

	/**
	 * Build the bootstrap registry.
	 *
	 * @return The built bootstrap registry
	 */
	public BootstrapServiceRegistry build() {
		final ClassLoaderService classLoaderService;
		if ( providedClassLoaderService == null ) {
			// Use a set.  As an example, in JPA, OsgiClassLoader may be in both
			// the providedClassLoaders and the overriddenClassLoader.
			final Set<ClassLoader> classLoaders = new HashSet<>();

			if ( providedClassLoaders != null )  {
				classLoaders.addAll( providedClassLoaders );
			}

			classLoaderService = new ClassLoaderServiceImpl( classLoaders,tcclLookupPrecedence );
		}
		else {
			classLoaderService = providedClassLoaderService;
		}

		final IntegratorServiceImpl integratorService = IntegratorServiceImpl.create(
				providedIntegrators,
				classLoaderService
		);

		return new BootstrapServiceRegistryImpl(
				autoCloseRegistry,
				classLoaderService,
				strategySelectorBuilder.buildSelector( classLoaderService ),
				integratorService
		);
	}

	/**
	 * Destroy a service registry. Clients should only destroy registries they have created.
	 *
	 * @param serviceRegistry The registry to be closed.
	 */
	public static void destroy(ServiceRegistry serviceRegistry) {
		if ( serviceRegistry == null ) {
			return;
		}

		( (BootstrapServiceRegistryImpl) serviceRegistry ).destroy();
	}
}
