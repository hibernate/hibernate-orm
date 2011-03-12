/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.TransientObjectException;
import org.hibernate.ReplicationMode;
import org.hibernate.LockMode;
import org.hibernate.engine.Cascade;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.Status;
import org.hibernate.event.EventSource;
import org.hibernate.event.ReplicateEvent;
import org.hibernate.event.ReplicateEventListener;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the default replicate event listener used by Hibernate to replicate
 * entities in response to generated replicate events.
 *
 * @author Steve Ebersole
 */
public class DefaultReplicateEventListener extends AbstractSaveEventListener implements ReplicateEventListener {

	private static final Logger log = LoggerFactory.getLogger( DefaultReplicateEventListener.class );

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
			log.trace( "uninitialized proxy passed to replicate()" );
			return;
		}

		Object entity = source.getPersistenceContext().unproxyAndReassociate( event.getObject() );

		if ( source.getPersistenceContext().isEntryFor( entity ) ) {
			log.trace( "ignoring persistent instance passed to replicate()" );
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
			if ( log.isTraceEnabled() ) {
				log.trace(
						"found existing row for " +
								MessageHelper.infoString( persister, id, source.getFactory() )
				);
			}

			/// HHH-2378
			final Object realOldVersion = persister.isVersioned() ? oldVersion : null;
			
			boolean canReplicate = replicationMode.shouldOverwriteCurrentVersion(
					entity,
					realOldVersion,
					persister.getVersion( entity, source.getEntityMode() ),
					persister.getVersionType()
			);

			if ( canReplicate ) {
				//will result in a SQL UPDATE:
				performReplication( entity, id, realOldVersion, persister, replicationMode, source );
			}
			else {
				//else do nothing (don't even reassociate object!)
				log.trace( "no need to replicate" );
			}

			//TODO: would it be better to do a refresh from db?
		}
		else {
			// no existing row - do an insert
			if ( log.isTraceEnabled() ) {
				log.trace(
						"no existing row, replicating new instance " +
								MessageHelper.infoString( persister, id, source.getFactory() )
				);
			}

			final boolean regenerate = persister.isIdentifierAssignedByInsert(); // prefer re-generation of identity!
			final EntityKey key = regenerate ?
					null : new EntityKey( id, persister, source.getEntityMode() );

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

	protected boolean visitCollectionsBeforeSave(Object entity, Serializable id, Object[] values, Type[] types, EventSource source) {
		//TODO: we use two visitors here, inefficient!
		OnReplicateVisitor visitor = new OnReplicateVisitor( source, id, entity, false );
		visitor.processEntityPropertyValues( values, types );
		return super.visitCollectionsBeforeSave( entity, id, values, types, source );
	}

	protected boolean substituteValuesIfNecessary(
			Object entity,
			Serializable id,
			Object[] values,
			EntityPersister persister,
			SessionImplementor source) {
		return false;
	}

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

		if ( log.isTraceEnabled() ) {
			log.trace(
					"replicating changes to " +
							MessageHelper.infoString( persister, id, source.getFactory() )
			);
		}

		new OnReplicateVisitor( source, id, entity, true ).process( entity, persister );

		source.getPersistenceContext().addEntity(
				entity,
				( persister.isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				null,
				new EntityKey( id, persister, source.getEntityMode() ),
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

	protected CascadingAction getCascadeAction() {
		return CascadingAction.REPLICATE;
	}
}
