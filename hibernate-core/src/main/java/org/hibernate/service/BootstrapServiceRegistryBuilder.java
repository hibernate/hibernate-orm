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
package org.hibernate.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.hibernate.integrator.internal.IntegratorServiceImpl;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.internal.BootstrapServiceRegistryImpl;

/**
 * Builder for bootstrap {@link ServiceRegistry} instances.
 *
 * @author Steve Ebersole
 *
 * @see BootstrapServiceRegistryImpl
 * @see ServiceRegistryBuilder#ServiceRegistryBuilder(BootstrapServiceRegistry)
 */
public class BootstrapServiceRegistryBuilder {
	private final LinkedHashSet<Integrator> providedIntegrators = new LinkedHashSet<Integrator>();
	private List<ClassLoader> providedClassLoaders;
	private ClassLoaderService providedClassLoaderService;

	/**
	 * Add an {@link Integrator} to be applied to the bootstrap registry.
	 *
	 * @param integrator The integrator to add.
	 * @return {@code this}, for method chaining
	 */
	public BootstrapServiceRegistryBuilder with(Integrator integrator) {
		providedIntegrators.add( integrator );
		return this;
	}

	/**
	 * Applies the specified {@link ClassLoader} as the application class loader for the bootstrap registry
	 *
	 * @param classLoader The class loader to use
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #with(ClassLoader)} instead
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	@Deprecated
	public BootstrapServiceRegistryBuilder withApplicationClassLoader(ClassLoader classLoader) {
		return with( classLoader );
	}

	/**
	 * Adds a provided {@link ClassLoader} for use in class-loading and resource-lookup
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
	 * Adds a provided {@link ClassLoaderService} for use in class-loading and resource-lookup
	 *
	 * @param classLoaderService The class loader to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public BootstrapServiceRegistryBuilder with(ClassLoaderService classLoaderService) {
		providedClassLoaderService = classLoaderService;
		return this;
	}


	/**
	 * Applies the specified {@link ClassLoader} as the resource class loader for the bootstrap registry
	 *
	 * @param classLoader The class loader to use
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
	 * Applies the specified {@link ClassLoader} as the Hibernate class loader for the bootstrap registry
	 *
	 * @param classLoader The class loader to use
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #with(ClassLoader)} instead
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	@Deprecated
	public BootstrapServiceRegistryBuilder withHibernateClassLoader(ClassLoader classLoader) {
		return with( classLoader );
	}

	/**
	 * Applies the specified {@link ClassLoader} as the environment (or system) class loader for the bootstrap registry
	 *
	 * @param classLoader The class loader to use
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #with(ClassLoader)} instead
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	@Deprecated
	public BootstrapServiceRegistryBuilder withEnvironmentClassLoader(ClassLoader classLoader) {
		return with( classLoader );
	}

	/**
	 * Build the bootstrap registry.
	 *
	 * @return The built bootstrap registry
	 */
	public BootstrapServiceRegistry build() {
		final ClassLoaderService classLoaderService;
		if ( providedClassLoaderService == null ) {
			classLoaderService = new ClassLoaderServiceImpl( providedClassLoaders );
		} else {
			classLoaderService = providedClassLoaderService;
		}

		final IntegratorServiceImpl integratorService = new IntegratorServiceImpl(
				providedIntegrators,
				classLoaderService
		);

		return new BootstrapServiceRegistryImpl( classLoaderService, integratorService );
	}
}
