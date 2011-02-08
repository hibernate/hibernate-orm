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
import java.util.Map;

/**
 * Responsible for initiating services.
 *
 * @author Steve Ebersole
 */
public interface ServiceInitiator<T extends Service> {
	/**
	 * Obtains the service role initiated by this initiator.  Should be uniqie withion a registry
	 *
	 * @return The service role.
	 */
	public Class<T> getServiceInitiated();

	/**
	 * Initiates the managed service.
	 *
	 * @param configurationValues The configuration values in effect
	 * @param registry The service registry.  Can be used to locate services needed to fulfill initiation.
	 *
	 * @return The initiated service.
	 */
	public T initiateService(Map configurationValues, ServicesRegistry registry);
}
