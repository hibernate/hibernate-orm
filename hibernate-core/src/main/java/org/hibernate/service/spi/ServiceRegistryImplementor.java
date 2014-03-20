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
package org.hibernate.service.spi;

import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;

/**
 * Additional integration contracts for a service registry.
 *
 * @author Steve Ebersole
 */
public interface ServiceRegistryImplementor extends ServiceRegistry {
	/**
	 * Locate the binding for the given role.  Should, generally speaking, look into parent registry if one.
	 *
	 * @param serviceRole The service role for which to locate a binding.
	 * @param <R> generic return type.
	 *
	 * @return The located binding; may be {@code null}
	 */
	public <R extends Service> ServiceBinding<R> locateServiceBinding(Class<R> serviceRole);

	/**
	 * Release resources
	 */
	public void destroy();

	/**
	 * When a registry is created with a parent, the parent is notified of the child
	 * via this callback.
	 */
	public void registerChild(ServiceRegistryImplementor child);

	/**
	 * When a registry is created with a parent, the parent is notified of the child
	 * via this callback.
	 */
	public void deRegisterChild(ServiceRegistryImplementor child);
}
