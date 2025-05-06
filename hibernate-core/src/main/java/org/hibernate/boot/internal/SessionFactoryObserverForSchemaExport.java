/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

/**
 * Responsible for calling the {@link SchemaManagementToolCoordinator}
 * when the {@link SessionFactory} is created and destroyed.
 *
 * @implNote This was added in order to clean up the constructor of
 *           {@link org.hibernate.internal.SessionFactoryImpl}, which
 *           was doing too many things.
 *
 * @author Gavin King
 */
class SessionFactoryObserverForSchemaExport implements SessionFactoryObserver {
	private final MetadataImplementor metadata;
	private DelayedDropAction delayedDropAction;

	SessionFactoryObserverForSchemaExport(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		SchemaManagementToolCoordinator.process(
				metadata,
				getRegistry( factory ),
				factory.getProperties(),
				action -> delayedDropAction = action
		);
	}

	@Override
	public void sessionFactoryClosing(SessionFactory factory) {
		if ( delayedDropAction != null ) {
			delayedDropAction.perform( getRegistry( factory ) );
		}
	}

	private static ServiceRegistryImplementor getRegistry(SessionFactory factory) {
		return ( (SessionFactoryImplementor) factory ).getServiceRegistry();
	}
}
