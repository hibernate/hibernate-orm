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
package org.hibernate.service.internal;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryImpl
		extends AbstractServiceRegistryImpl
		implements SessionFactoryServiceRegistry {

	private final SessionFactoryImplementor sessionFactory;
	private Configuration configuration;

	// for now we need to hold on to the Configuration... :(

	public SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			SessionFactoryImplementor sessionFactory,
			Configuration configuration) {
		super( parent );
		this.sessionFactory = sessionFactory;
		this.configuration = configuration;
	}

	@Override
	protected <T extends Service> T createService(Class<T> serviceRole) {
		return null; // todo : implement method body
	}

	@Override
	protected <T extends Service> void configureService(T service) {
		// todo : implement method body
	}
}
