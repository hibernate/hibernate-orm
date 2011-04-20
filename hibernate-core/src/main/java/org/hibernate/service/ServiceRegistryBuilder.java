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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.spi.BasicServiceInitiator;

/**
 * @author Steve Ebersole
 */
public class ServiceRegistryBuilder {
	private final Map configurationValues;
	private final List<BasicServiceInitiator> initiators = standardInitiatorList();
	private final List<ProvidedService> services = new ArrayList<ProvidedService>();

	public ServiceRegistryBuilder() {
		this( new HashMap() );
	}

	public ServiceRegistryBuilder(Map configurationValues) {
		this.configurationValues = configurationValues;
	}

	private static List<BasicServiceInitiator> standardInitiatorList() {
		final List<BasicServiceInitiator> initiators = new ArrayList<BasicServiceInitiator>();
		initiators.addAll( StandardServiceInitiators.LIST );
		return initiators;
	}

	public ServiceRegistryBuilder addInitiator(BasicServiceInitiator initiator) {
		initiators.add( initiator );
		return this;
	}

	@SuppressWarnings( {"unchecked"})
	public ServiceRegistryBuilder addService(final Class serviceRole, final Service service) {
		services.add( new ProvidedService( serviceRole, service ) );
		return this;
	}

	public BasicServiceRegistry buildServiceRegistry() {
		return new BasicServiceRegistryImpl( initiators, services, configurationValues );
	}
}
