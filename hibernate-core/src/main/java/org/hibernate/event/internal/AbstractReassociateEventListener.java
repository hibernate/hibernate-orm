/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.internal.TypeHelper;

import org.jboss.logging.Logger;

/**
 * A convenience base class for listeners that respond to requests to reassociate an entity
 * to a session ( such as through lock() or update() ).
 *
 * @author Gavin King
 */
public abstract class AbstractReassociateEventListener implements Serializable {
	private static final Logger log = CoreLogging.logger( AbstractReassociateEventListener.class );

	/**
	 * Associates a given entity (either transient or associated with another session) to
	 * the given session.
	 *
	 * @param event The event triggering the re-association
	 * @param object The entity to be associated
	 * @param id The id of the entity.
	 * @param entityDescriptor The entity's Descriptor instance.
	 *
	 * @return An EntityEntry representing the entity within this session.
	 */
	protected final EntityEntry reassociate(
			AbstractEvent event,
			Object object,
			Object id,
			EntityTypeDescriptor entityDescriptor) {

		if ( log.isTraceEnabled() ) {
			log.tracev(
					"Reassociating transient instance: {0}",
					MessageHelper.infoString( entityDescriptor, id, event.getSession().getFactory() )
			);
		}

		final EventSource source = event.getSession();
		final EntityKey key = source.generateEntityKey( id, entityDescriptor );

		source.getPersistenceContext().checkUniqueness( key, object );

		//get a snapshot
		Object[] values = entityDescriptor.getPropertyValues( object );

		TypeHelper.deepCopy(
				entityDescriptor,
				values,
				values,
				StateArrayContributor::isUpdatable
		);

		Object version = Versioning.getVersion( values, entityDescriptor );

		EntityEntry newEntry = source.getPersistenceContext().addEntity(
				object,
				( entityDescriptor.getJavaTypeDescriptor().getMutabilityPlan().isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				values,
				key,
				version,
				LockMode.NONE,
				true,
				entityDescriptor,
				false
		);

		new OnLockVisitor( source, id, object ).process( object, entityDescriptor );

		entityDescriptor.afterReassociate( object, source );

		return newEntry;

	}
}
