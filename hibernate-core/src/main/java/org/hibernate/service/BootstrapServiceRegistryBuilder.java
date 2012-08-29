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
package org.hibernate.service;

import org.hibernate.integrator.spi.Integrator;

/**
 * @deprecated Use {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder} instead
 */
@Deprecated
public class BootstrapServiceRegistryBuilder extends org.hibernate.boot.registry.BootstrapServiceRegistryBuilder {
	@Override
	public BootstrapServiceRegistryBuilder with(Integrator integrator) {
		super.with( integrator );
		return this;
	}

	@Override
	public BootstrapServiceRegistryBuilder withApplicationClassLoader(ClassLoader classLoader) {
		super.withApplicationClassLoader( classLoader );
		return this;
	}

	@Override
	public BootstrapServiceRegistryBuilder withResourceClassLoader(ClassLoader classLoader) {
		super.withResourceClassLoader( classLoader );
		return this;
	}

	@Override
	public BootstrapServiceRegistryBuilder withHibernateClassLoader(ClassLoader classLoader) {
		super.withHibernateClassLoader( classLoader );
		return this;
	}

	@Override
	public BootstrapServiceRegistryBuilder withEnvironmentClassLoader(ClassLoader classLoader) {
		super.withEnvironmentClassLoader( classLoader );
		return this;
	}

	@Override
	public <T> BootstrapServiceRegistryBuilder withStrategySelector(Class<T> strategy, String name, Class<? extends T> implementation) {
		super.withStrategySelector( strategy, name, implementation );
		return this;
	}

	@Override
	public BootstrapServiceRegistry build() {
		return (BootstrapServiceRegistry) super.build();
	}
}
