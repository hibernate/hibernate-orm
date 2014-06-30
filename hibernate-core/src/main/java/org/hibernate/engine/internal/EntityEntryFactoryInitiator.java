/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

package org.hibernate.engine.internal;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Standard initiator for the {@link org.hibernate.engine.spi.EntityEntryFactory}.
 * <p>
 * Implementation note:
 * A {@link org.hibernate.service.spi.SessionFactoryServiceInitiator} is used to allow
 * overriding implementations to depend on session factory level services:
 * OGM datastore provider is an example.
 * TODO: make sure it is required to be a SessionFactoryServiceInitiator
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class EntityEntryFactoryInitiator implements SessionFactoryServiceInitiator<EntityEntryFactory> {

	public static final EntityEntryFactoryInitiator INSTANCE = new EntityEntryFactoryInitiator();

	@Override
	public EntityEntryFactory initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		return new DefaultEntityEntryFactory();
	}

	@Override
	public EntityEntryFactory initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		return new DefaultEntityEntryFactory();
	}

	@Override
	public Class<EntityEntryFactory> getServiceInitiated() {
		return EntityEntryFactory.class;
	}
}
