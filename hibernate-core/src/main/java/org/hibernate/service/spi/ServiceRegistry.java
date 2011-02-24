/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

/**
 * The registry of services used by Hibernate
 *
 * @author Steve Ebersole
 */
public interface ServiceRegistry {
	/**
	 * Retrieve a service by role.  If service is not found, but a {@link ServiceInitiator} is registered for
	 * this service role, the service will be initialized and returned.
	 *
	 * @param serviceRole The service role
	 * @param <T> The type of the service
	 *
	 * @return The requested service.
	 *
	 * @throws UnknownServiceException Indicates the service was not known.
	 */
	public <T extends Service> T getService(Class<T> serviceRole);

	/**
	 * Register a service into the registry.
	 *
	 * @param serviceRole The service role.
	 * @param service The service to register
	 */
	public <T extends Service> void registerService(Class<T> serviceRole, T service);

	/**
	 * Register a service initiator.
	 *
	 * @param initiator The initiator of a service
	 */
	public void registerServiceInitiator(ServiceInitiator initiator);
}
