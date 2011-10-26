/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.event.internal;

import java.io.Serializable;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.ReplicateEvent;
import org.hibernate.event.spi.ReplicateEventListener;
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

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       DefaultReplicateEventListener.class.getName());

	/**
	 * Handle the given replicate event.
	 *
	 * @param event The replicate event to be handled.
	 *
	 * @throws TransientObjectException An invalid attempt to replicate a transient entity.
	 */
	public void onReplicate(ReplicateEvent event) {
		final EventSource source = event.getSession();
		if ( source.getPersistenceContext().reassociateIfUninitializedProxy( event.getObject() ) ) {
			LOG.trace( "Uninitialized proxy passed to replicate()" );
			return;
		}

		Object entity = source.getPersistenceContext().unproxyAndReassociate( event.getObject() );

		if ( source.getPersistenceContext().isEntryFor( entity ) ) {
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
				LOG.tracev( "Found existing row for {0}", MessageHelper.infoString( persister, id, source.getFactory() ) );
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
			if ( canReplicate )
				performReplication( entity, id, realOldVersion, persister, replicationMode, source );
			else
				LOG.trace( "No need to replicate" );

			//TODO: would it be better to do a refresh from db?
		}
		else {
			// no existing row - do an insert
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "No existing row, replicating new instance {0}",
						MessageHelper.infoString( persister, id, source.getFactory() ) );
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
    protected boolean visitCollectionsBeforeSave(Object entity, Serializable id, Object[] values, Type[] types, EventSource source) {
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

		source.getPersistenceContext().addEntity(
				entity,
				( persister.isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				null,
				source.generateEntityKey( id, persister ),
				version,
				LockMode.NONE,
				true,
				persister,
				true,
				false
		);

		cascadeAfterReplicate( entity, persister, replicationMode, source );
	}

	private void cascadeAfterReplicate(
			Object entity,
			EntityPersister persister,
			ReplicationMode replicationMode,
			EventSource source) {
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( CascadingAction.REPLICATE, Cascade.AFTER_UPDATE, source )
					.cascade( persister, entity, replicationMode );
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

	@Override
    protected CascadingAction getCascadeAction() {
		return CascadingAction.REPLICATE;
	}
}
