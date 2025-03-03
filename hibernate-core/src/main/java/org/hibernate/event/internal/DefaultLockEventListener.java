/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LockEvent;
import org.hibernate.event.spi.LockEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.type.TypeHelper;
import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

import static org.hibernate.engine.internal.Versioning.getVersion;
import static org.hibernate.loader.ast.internal.LoaderHelper.upgradeLock;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Defines the default lock event listeners used by hibernate to lock entities
 * in response to generated lock events.
 *
 * @author Steve Ebersole
 */
public class DefaultLockEventListener implements LockEventListener {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			DefaultLockEventListener.class.getName()
	);

	/**
	 * Handle the given lock event.
	 *
	 * @param event The lock event to be handled.
	 */
	@Override
	public void onLock(LockEvent event) throws HibernateException {

		if ( event.getObject() == null ) {
			throw new NullPointerException( "attempted to lock null" );
		}

		if ( event.getLockMode() == LockMode.WRITE ) {
			throw new HibernateException( "Invalid lock mode for lock()" );
		}

		if ( event.getLockMode() == LockMode.UPGRADE_SKIPLOCKED ) {
			LOG.explicitSkipLockedLockCombo();
		}

		final SessionImplementor source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		final Object entity = persistenceContext.unproxyAndReassociate( event.getObject() );
		//TODO: if object was an uninitialized proxy, this is inefficient,
		//      resulting in two SQL selects

		EntityEntry entry = persistenceContext.getEntry( entity );
		if ( entry == null ) {
			final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
			final Object id = persister.getIdentifier( entity, source );
			if ( !ForeignKeys.isNotTransient( event.getEntityName(), entity, Boolean.FALSE, source ) ) {
				throw new TransientObjectException( "Cannot lock unsaved transient instance of entity '"
						+ persister.getEntityName() + "'" );
			}
			entry = reassociate( event, entity, id, persister );
			cascadeOnLock( event, persister, entity );
		}

		upgradeLock( entity, entry, event.getLockOptions(), event.getSession() );
	}

	private void cascadeOnLock(LockEvent event, EntityPersister persister, Object entity) {
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		persistenceContext.incrementCascadeLevel();
		try {
			Cascade.cascade(
					CascadingActions.LOCK,
					CascadePoint.AFTER_LOCK,
					source,
					persister,
					entity,
					event.getLockOptions()
			);
		}
		finally {
			persistenceContext.decrementCascadeLevel();
		}
	}

	/**
	 * Associates a given entity (either transient or associated with another session)
	 * to the given session.
	 *
	 * @param event The event triggering the re-association
	 * @param object The entity to be associated
	 * @param id The id of the entity.
	 * @param persister The entity's persister instance.
	 *
	 * @return An EntityEntry representing the entity within this session.
	 */
	protected final EntityEntry reassociate(AbstractEvent event, Object object, Object id, EntityPersister persister) {

		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Reassociating transient instance: " + infoString( persister, id, event.getFactory() ) );
		}

		final EventSource source = event.getSession();
		final EntityKey key = source.generateEntityKey( id, persister );
		final PersistenceContext persistenceContext = source.getPersistenceContext();

		persistenceContext.checkUniqueness( key, object );

		//get a snapshot
		final Object[] values = persister.getValues( object );
		TypeHelper.deepCopy(
				values,
				persister.getPropertyTypes(),
				persister.getPropertyUpdateability(),
				values,
				source
		);

		final EntityEntry newEntry = persistenceContext.addEntity(
				object,
				persister.isMutable() ? Status.MANAGED : Status.READ_ONLY,
				values,
				key,
				getVersion( values, persister ),
				LockMode.NONE,
				true,
				persister,
				false
		);

		new OnLockVisitor( source, id, object ).process( object, persister );

		persister.afterReassociate( object, source );

		return newEntry;
	}
}
