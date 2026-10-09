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
	private MetadataImplementor metadata;
	private DelayedDropAction delayedDropAction;

	SessionFactoryObserverForSchemaExport(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		try {
			SchemaManagementToolCoordinator.process(
					metadata,
					getRegistry( factory ),
					factory.getProperties(),
					action -> delayedDropAction = action
			);
		}
		finally {
			// only the DelayedDropAction is needed after this point,
			// and holding on to the boot model would keep it reachable
			// for the whole lifetime of the SessionFactory
			metadata = null;
		}
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
