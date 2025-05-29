/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.ReplicateEvent;
import org.hibernate.event.spi.ReplicateEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Defines the default replicate event listener used by Hibernate to replicate
 * entities in response to generated replicate events.
 *
 * @author Steve Ebersole
 *
 * @deprecated since {@link org.hibernate.Session#replicate} is deprecated
 */
@Deprecated(since="6")
public class DefaultReplicateEventListener
		extends AbstractSaveEventListener<ReplicationMode>
		implements ReplicateEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultReplicateEventListener.class );

	/**
	 * Handle the given replicate event.
	 *
	 * @param event The replicate event to be handled.
	 *
	 * @throws TransientObjectException An invalid attempt to replicate a transient entity.
	 */
	@Override
	public void onReplicate(ReplicateEvent event) {
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		if ( persistenceContext.reassociateIfUninitializedProxy( event.getObject() ) ) {
			LOG.trace( "Uninitialized proxy passed to replicate()" );
		}
		else {
			final Object entity = persistenceContext.unproxyAndReassociate( event.getObject() );
			if ( persistenceContext.isEntryFor( entity ) ) {
				LOG.trace( "Ignoring persistent instance passed to replicate()" );
				//hum ... should we cascade anyway? throw an exception? fine like it is?
			}
			else {
				doReplicate( event, source, entity );
			}
		}
	}

	private void doReplicate(ReplicateEvent event, EventSource source, Object entity) {
		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity);
		final ReplicationMode replicationMode = event.getReplicationMode();

		// get the id from the object - we accept almost anything at all,
		// except null (that is, even ids which look like they're unsaved)
		final Object id = persister.getIdentifier( entity, source );
		if ( id == null ) {
			throw new TransientObjectException( "Cannot replicate instance of entity '" + persister.getEntityName()
					+ "' because it has a null identifier" );
		}

		final Object oldVersion = replicationMode == ReplicationMode.EXCEPTION
				? null // always do an INSERT, and let it fail by constraint violation
				: persister.getCurrentVersion( id, source); // what is the version on the database?

		if ( oldVersion != null ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Found existing row for " + infoString( persister, id, event.getFactory() ) );
			}
			// If the entity has no version, getCurrentVersion() just returns
			// a meaningless value to indicate that the row exists (HHH-2378)
			final Object realOldVersion = persister.isVersioned() ? oldVersion : null;
			if ( shouldOverwrite( replicationMode,
					persister.getVersion( entity ), realOldVersion,
					persister.getVersionType() ) ) {
				// execute a SQL UPDATE
				performReplication( entity, id, realOldVersion, persister, replicationMode, source );
			}
			else if ( LOG.isTraceEnabled() ) {
				// do nothing (don't even re-associate object!)
				LOG.trace( "No need to replicate" );
			}

			//TODO: would it be better to do a refresh from db?
		}
		else {
			// no existing row - execute a SQL INSERT
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "No existing row, replicating new instance "
							+ infoString( persister, id, event.getFactory() ) );
			}
			final boolean regenerate = persister.isIdentifierAssignedByInsert(); // prefer re-generation of identity!
			final EntityKey key = regenerate ? null : source.generateEntityKey( id, persister );
			performSaveOrReplicate( entity, key, persister, regenerate, replicationMode, source, false );
		}
	}

	private static <T> boolean shouldOverwrite(
			ReplicationMode replicationMode, Object entityVersion, Object realOldVersion, BasicType<T> versionType) {
		return replicationMode.shouldOverwriteCurrentVersion( (T) realOldVersion, (T) entityVersion, versionType );
	}

	@Override
	protected boolean visitCollectionsBeforeSave(
			Object entity,
			Object id,
			Object[] values,
			Type[] types,
			EventSource source) {
		//TODO: we use two visitors here, inefficient!
		OnReplicateVisitor visitor = new OnReplicateVisitor( source, id, entity, false );
		visitor.processEntityPropertyValues( entity, values, types );
		return super.visitCollectionsBeforeSave( entity, id, values, types, source );
	}

	@Override
	protected boolean substituteValuesIfNecessary(
			Object entity,
			Object id,
			Object[] values,
			EntityPersister persister,
			SessionImplementor source) {
		return false;
	}

	@Override
	protected boolean isVersionIncrementDisabled() {
		return true;
	}

	private void performReplication(
			Object entity,
			Object id,
			Object version,
			EntityPersister persister,
			ReplicationMode replicationMode,
			EventSource source) throws HibernateException {

		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Replicating changes to " + infoString( persister, id, source.getFactory() ) );
		}

		new OnReplicateVisitor( source, id, entity, true ).process( entity, persister );

		source.getPersistenceContextInternal().addEntity(
				entity,
				persister.isMutable() ? Status.MANAGED : Status.READ_ONLY,
				null,
				source.generateEntityKey( id, persister ),
				version,
				LockMode.NONE,
				true,
				persister,
				true
		);

		cascadeAfterReplicate( entity, persister, replicationMode, source );
	}

	private void cascadeAfterReplicate(
			Object entity,
			EntityPersister persister,
			ReplicationMode replicationMode,
			EventSource source) {
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		persistenceContext.incrementCascadeLevel();
		try {
			Cascade.cascade(
					CascadingActions.REPLICATE,
					CascadePoint.AFTER_UPDATE,
					source,
					persister,
					entity,
					replicationMode
			);
		}
		finally {
			persistenceContext.decrementCascadeLevel();
		}
	}

	@Override
	protected CascadingAction<ReplicationMode> getCascadeAction() {
		return CascadingActions.REPLICATE;
	}
}
