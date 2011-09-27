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

import java.util.LinkedHashSet;

import org.hibernate.integrator.internal.IntegratorServiceImpl;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.classloading.internal.ClassLoaderServiceImpl;
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
	private ClassLoader applicationClassLoader;
	private ClassLoader resourcesClassLoader;
	private ClassLoader hibernateClassLoader;
	private ClassLoader environmentClassLoader;

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
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public BootstrapServiceRegistryBuilder withApplicationClassLoader(ClassLoader classLoader) {
		this.applicationClassLoader = classLoader;
		return this;
	}

	/**
	 * Applies the specified {@link ClassLoader} as the resource class loader for the bootstrap registry
	 *
	 * @param classLoader The class loader to use
	 * @return {@code this}, for method chaining
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public BootstrapServiceRegistryBuilder withResourceClassLoader(ClassLoader classLoader) {
		this.resourcesClassLoader = classLoader;
		return this;
	}

	/**
	 * Applies the specified {@link ClassLoader} as the Hibernate class loader for the bootstrap registry
	 *
	 * @param classLoader The class loader to use
	 * @return {@code this}, for method chaining
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public BootstrapServiceRegistryBuilder withHibernateClassLoader(ClassLoader classLoader) {
		this.hibernateClassLoader = classLoader;
		return this;
	}

	/**
	 * Applies the specified {@link ClassLoader} as the environment (or system) class loader for the bootstrap registry
	 *
	 * @param classLoader The class loader to use
	 * @return {@code this}, for method chaining
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public BootstrapServiceRegistryBuilder withEnvironmentClassLoader(ClassLoader classLoader) {
		this.environmentClassLoader = classLoader;
		return this;
	}

	/**
	 * Build the bootstrap registry.
	 *
	 * @return The built bootstrap registry
	 */
	public BootstrapServiceRegistry build() {
		final ClassLoaderServiceImpl classLoaderService = new ClassLoaderServiceImpl(
				applicationClassLoader,
				resourcesClassLoader,
				hibernateClassLoader,
				environmentClassLoader
		);

		final IntegratorServiceImpl integratorService = new IntegratorServiceImpl(
				providedIntegrators,
				classLoaderService
		);

		return new BootstrapServiceRegistryImpl( classLoaderService, integratorService );
	}
}
