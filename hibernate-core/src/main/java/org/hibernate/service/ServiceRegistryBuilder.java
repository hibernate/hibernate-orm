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

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;

/**
 * @deprecated Use {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder} instead
 */
@Deprecated
public class ServiceRegistryBuilder extends org.hibernate.boot.registry.StandardServiceRegistryBuilder {
	public ServiceRegistryBuilder() {
		super();    //To change body of overridden methods use File | Settings | File Templates.
	}

	public ServiceRegistryBuilder(BootstrapServiceRegistry bootstrapServiceRegistry) {
		super( bootstrapServiceRegistry );    //To change body of overridden methods use File | Settings | File Templates.
	}

	@Override
	public ServiceRegistryBuilder loadProperties(String resourceName) {
		super.loadProperties( resourceName );
		return this;
	}

	@Override
	public ServiceRegistryBuilder configure() {
		super.configure();
		return this;
	}

	@Override
	public ServiceRegistryBuilder configure(String resourceName) {
		super.configure( resourceName );
		return this;
	}

	@Override
	public ServiceRegistryBuilder applySetting(String settingName, Object value) {
		super.applySetting( settingName, value );
		return this;
	}

	@Override
	public ServiceRegistryBuilder applySettings(Map settings) {
		super.applySettings( settings );
		return this;
	}

	@Override
	public ServiceRegistryBuilder addInitiator(StandardServiceInitiator initiator) {
		super.addInitiator( initiator );
		return this;
	}

	@Override
	public ServiceRegistryBuilder addService(Class serviceRole, Service service) {
		super.addService( serviceRole, service );
		return this;
	}

	public StandardServiceRegistry build() {
		return super.build();
	}
}
