/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

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
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * Defines the default replicate event listener used by Hibernate to replicate
 * entities in response to generated replicate events.
 *
 * @author Steve Ebersole
 */
public class DefaultReplicateEventListener extends AbstractSaveEventListener implements ReplicateEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultReplicateEventListener.class );

	/**
	 * Handle the given replicate event.
	 *
	 * @param event The replicate event to be handled.
	 *
	 * @throws TransientObjectException An invalid attempt to replicate a transient entity.
	 */
	public void onReplicate(ReplicateEvent event) {
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		if ( persistenceContext.reassociateIfUninitializedProxy( event.getObject() ) ) {
			LOG.trace( "Uninitialized proxy passed to replicate()" );
			return;
		}

		Object entity = persistenceContext.unproxyAndReassociate( event.getObject() );

		if ( persistenceContext.isEntryFor( entity ) ) {
			LOG.trace( "Ignoring persistent instance passed to replicate()" );
			//hum ... should we cascade anyway? throw an exception? fine like it is?
			return;
		}

		EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );

		// get the id from the object
		/*if ( persister.isUnsaved(entity, source) ) {
			throw new TransientObjectException("transient instance passed to replicate()");
		}*/
		Serializable id = persister.getIdentifier( entity, source );
		if ( id == null ) {
			throw new TransientObjectException( "instance with null id passed to replicate()" );
		}

		final ReplicationMode replicationMode = event.getReplicationMode();

		final Object oldVersion;
		if ( replicationMode == ReplicationMode.EXCEPTION ) {
			//always do an INSERT, and let it fail by constraint violation
			oldVersion = null;
		}
		else {
			//what is the version on the database?
			oldVersion = persister.getCurrentVersion( id, source );
		}

		if ( oldVersion != null ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Found existing row for {0}", MessageHelper.infoString(
						persister,
						id,
						source.getFactory()
				)
				);
			}

			/// HHH-2378
			final Object realOldVersion = persister.isVersioned() ? oldVersion : null;

			boolean canReplicate = replicationMode.shouldOverwriteCurrentVersion(
					entity,
					realOldVersion,
					persister.getVersion( entity ),
					persister.getVersionType()
			);

			// if can replicate, will result in a SQL UPDATE
			// else do nothing (don't even reassociate object!)
			if ( canReplicate ) {
				performReplication( entity, id, realOldVersion, persister, replicationMode, source );
			}
			else if ( LOG.isTraceEnabled() ) {
				LOG.trace( "No need to replicate" );
			}

			//TODO: would it be better to do a refresh from db?
		}
		else {
			// no existing row - do an insert
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"No existing row, replicating new instance {0}",
						MessageHelper.infoString( persister, id, source.getFactory() )
				);
			}

			final boolean regenerate = persister.isIdentifierAssignedByInsert(); // prefer re-generation of identity!
			final EntityKey key = regenerate ? null : source.generateEntityKey( id, persister );

			performSaveOrReplicate(
					entity,
					key,
					persister,
					regenerate,
					replicationMode,
					source,
					true
			);

		}
	}

	@Override
	protected boolean visitCollectionsBeforeSave(
			Object entity,
			Serializable id,
			Object[] values,
			Type[] types,
			EventSource source) {
		//TODO: we use two visitors here, inefficient!
		OnReplicateVisitor visitor = new OnReplicateVisitor( source, id, entity, false );
		visitor.processEntityPropertyValues( values, types );
		return super.visitCollectionsBeforeSave( entity, id, values, types, source );
	}

	@Override
	protected boolean substituteValuesIfNecessary(
			Object entity,
			Serializable id,
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
			Serializable id,
			Object version,
			EntityPersister persister,
			ReplicationMode replicationMode,
			EventSource source) throws HibernateException {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Replicating changes to {0}", MessageHelper.infoString( persister, id, source.getFactory() ) );
		}

		new OnReplicateVisitor( source, id, entity, true ).process( entity, persister );

		source.getPersistenceContextInternal().addEntity(
				entity,
				( persister.isMutable() ? Status.MANAGED : Status.READ_ONLY ),
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
	protected CascadingAction getCascadeAction() {
		return CascadingActions.REPLICATE;
	}
}
