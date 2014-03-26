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
package org.hibernate.boot.registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.boot.registry.selector.internal.StrategySelectorBuilder;
import org.hibernate.integrator.internal.IntegratorServiceImpl;
import org.hibernate.integrator.spi.Integrator;

/**
 * Builder for {@link BootstrapServiceRegistry} instances.  Provides registry for services needed for
 * most operations.  This includes {@link Integrator} handling and ClassLoader handling.
 *
 * Additionally responsible for building and managing the {@link org.hibernate.boot.registry.selector.spi.StrategySelector}
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 *
 * @see StandardServiceRegistryBuilder
 */
public class BootstrapServiceRegistryBuilder {
	private final LinkedHashSet<Integrator> providedIntegrators = new LinkedHashSet<Integrator>();

	private List<ClassLoader> providedClassLoaders;
	private ClassLoaderService providedClassLoaderService;
	private StrategySelectorBuilder strategySelectorBuilder = new StrategySelectorBuilder();

	private boolean autoCloseRegistry = true;

	/**
	 * Add an {@link Integrator} to be applied to the bootstrap registry.
	 *
	 * @param integrator The integrator to add.
	 *
	 * @return {@code this}, for method chaining
	 */
	public BootstrapServiceRegistryBuilder with(Integrator integrator) {
		providedIntegrators.add( integrator );
		return this;
	}

	/**
	 * Adds a provided {@link ClassLoader} for use in class-loading and resource-lookup.
	 *
	 * @param classLoader The class loader to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public BootstrapServiceRegistryBuilder with(ClassLoader classLoader) {
		if ( providedClassLoaders == null ) {
			providedClassLoaders = new ArrayList<ClassLoader>();
		}
		providedClassLoaders.add( classLoader );
		return this;
	}

	/**
	 * Adds a provided {@link ClassLoaderService} for use in class-loading and resource-lookup.
	 *
	 * @param classLoaderService The class loader service to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public BootstrapServiceRegistryBuilder with(ClassLoaderService classLoaderService) {
		providedClassLoaderService = classLoaderService;
		return this;
	}

	/**
	 * Applies the specified {@link ClassLoader} as the application class loader for the bootstrap registry.
	 *
	 * @param classLoader The class loader to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #with(ClassLoader)} instead
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public BootstrapServiceRegistryBuilder withApplicationClassLoader(ClassLoader classLoader) {
		return with( classLoader );
	}

	/**
	 * Applies the specified {@link ClassLoader} as the resource class loader for the bootstrap registry.
	 *
	 * @param classLoader The class loader to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #with(ClassLoader)} instead
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public BootstrapServiceRegistryBuilder withResourceClassLoader(ClassLoader classLoader) {
		return with( classLoader );
	}

	/**
	 * Applies the specified {@link ClassLoader} as the Hibernate class loader for the bootstrap registry.
	 *
	 * @param classLoader The class loader to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #with(ClassLoader)} instead
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public BootstrapServiceRegistryBuilder withHibernateClassLoader(ClassLoader classLoader) {
		return with( classLoader );
	}

	/**
	 * Applies the specified {@link ClassLoader} as the environment (or system) class loader for the bootstrap registry.
	 *
	 * @param classLoader The class loader to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #with(ClassLoader)} instead
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public BootstrapServiceRegistryBuilder withEnvironmentClassLoader(ClassLoader classLoader) {
		return with( classLoader );
	}

	/**
	 * Applies a named strategy implementation to the bootstrap registry.
	 *
	 * @param strategy The strategy
	 * @param name The registered name
	 * @param implementation The strategy implementation Class
	 * @param <T> Defines the strategy type and makes sure that the strategy and implementation are of
	 * compatible types.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.boot.registry.selector.spi.StrategySelector#registerStrategyImplementor(Class, String, Class)
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public <T> BootstrapServiceRegistryBuilder withStrategySelector(Class<T> strategy, String name, Class<? extends T> implementation) {
		this.strategySelectorBuilder.addExplicitStrategyRegistration( strategy, implementation, name );
		return this;
	}

	/**
	 * Applies one or more strategy selectors announced as available by the passed announcer.
	 *
	 * @param strategyRegistrationProvider An provider for one or more available selectors
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.boot.registry.selector.spi.StrategySelector#registerStrategyImplementor(Class, String, Class)
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public BootstrapServiceRegistryBuilder withStrategySelectors(StrategyRegistrationProvider strategyRegistrationProvider) {
		for ( StrategyRegistration strategyRegistration : strategyRegistrationProvider.getStrategyRegistrations() ) {
			this.strategySelectorBuilder.addExplicitStrategyRegistration( strategyRegistration );
		}
		return this;
	}

	/**
	 * By default, when a ServiceRegistry is no longer referenced by any other
	 * registries as a parent it will be closed.
	 * <p/>
	 * Some applications that explicitly build "shared registries" may want to
	 * circumvent that behavior.
	 * <p/>
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
	 * See the discussion on {@link #disableAutoClose}.  This method enables
	 * the auto-closing.
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
			// the providedClassLoaders and the overridenClassLoader.
			final Set<ClassLoader> classLoaders = new HashSet<ClassLoader>();

			if ( providedClassLoaders != null )  {
				classLoaders.addAll( providedClassLoaders );
			}
			
			classLoaderService = new ClassLoaderServiceImpl( classLoaders );
		}
		else {
			classLoaderService = providedClassLoaderService;
		}

		final IntegratorServiceImpl integratorService = new IntegratorServiceImpl(
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
}
