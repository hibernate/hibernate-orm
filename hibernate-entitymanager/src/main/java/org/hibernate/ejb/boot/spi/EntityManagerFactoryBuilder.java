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
package org.hibernate.ejb.boot.spi;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.ClassTransformer;

import org.hibernate.service.ServiceRegistryBuilder;

/**
 * Ultimately the goal of this contract is to build {@link EntityManagerFactory} instances, which is done through
 * the {@link #buildEntityManagerFactory()} method.  But we also expose metadata about the semi-parsed persistence unit
 * (as well as integration settings) that determine how the {@link EntityManagerFactory} will be built.
 *
 * @author Steve Ebersole
 */
public interface EntityManagerFactoryBuilder {
	/**
	 * Build {@link EntityManagerFactory} instance
	 *
	 * @return The built {@link EntityManagerFactory}
	 */
	public EntityManagerFactory buildEntityManagerFactory();

	/**
	 * Get the (read-only) view of in-effect settings that would be used when {@link #buildEntityManagerFactory()}
	 * is called.
	 *
	 * @return The in-effect settings.
	 */
	public Settings getSettings();

	/**
	 * Get the ServiceRegistryBuilder that backs this EntityManagerFactoryBuilderImpl instance.  The builder can be mutated
	 * if need be to add additional services ({@link ServiceRegistryBuilder#addService}) or additional
	 * service-initiators ({@link ServiceRegistryBuilder#addInitiator}).
	 *
	 * Note: {@link ServiceRegistryBuilder} also exposes the ability to add (which can also over-write) settings via
	 * the {@link ServiceRegistryBuilder#applySetting} and {@link ServiceRegistryBuilder#applySettings} methods,
	 * but use of those methods from here is discouraged as it will not effect {@link #getSettings()}
	 *
	 * @return The service registry builder.
	 */
	public ServiceRegistryBuilder getServiceRegistryBuilder();
}
