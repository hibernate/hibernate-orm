/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.synchronization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.hibernate.FlushMode;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.tools.Pair;
import org.jboss.logging.Logger;

import static org.hibernate.ConnectionAcquisitionMode.AS_NEEDED;
import static org.hibernate.ConnectionReleaseMode.AFTER_TRANSACTION;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class AuditProcess implements BeforeTransactionCompletionProcess {
	private static final Logger log = Logger.getLogger( AuditProcess.class );

	private final RevisionInfoGenerator revisionInfoGenerator;
	private final SharedSessionContractImplementor session;

	private final LinkedList<AuditWorkUnit> workUnits;
	private final Queue<AuditWorkUnit> undoQueue;
	private final Map<Pair<String, Object>, AuditWorkUnit> usedIds;
	private final Map<Pair<String, Object>, Object[]> entityStateCache;
	private final EntityChangeNotifier entityChangeNotifier;

	private Object revisionData;
	private boolean revisionDataSaved;

	public AuditProcess(RevisionInfoGenerator revisionInfoGenerator, SharedSessionContractImplementor session) {
		this.revisionInfoGenerator = revisionInfoGenerator;
		this.session = session;

		workUnits = new LinkedList<>();
		undoQueue = new LinkedList<>();
		usedIds = new HashMap<>();
		entityStateCache = new HashMap<>();
		entityChangeNotifier = new EntityChangeNotifier( revisionInfoGenerator, session );
	}

	public void cacheEntityState(Object id, String entityName, Object[] snapshot) {
		final Pair<String, Object> key = new Pair<>( entityName, id );
		if ( entityStateCache.containsKey( key ) ) {
			throw new AuditException( "The entity [" + entityName + "] with id [" + id + "] is already cached." );
		}
		entityStateCache.put( key, snapshot );
	}

	public Object[] getCachedEntityState(Object id, String entityName) {
		final Pair<String, Object> key = new Pair<>( entityName, id );
		final Object[] entityState = entityStateCache.get( key );
		if ( entityState != null ) {
			entityStateCache.remove( key );
		}
		return entityState;
	}

	private void removeWorkUnit(AuditWorkUnit vwu) {
		workUnits.remove( vwu );
		if ( vwu.isPerformed() ) {
			// If this work unit has already been performed, it must be deleted (undone) first.
			undoQueue.offer( vwu );
		}
	}

	public void addWorkUnit(AuditWorkUnit vwu) {
		if ( vwu.containsWork() ) {
			final Object entityId = vwu.getEntityId();

			if ( entityId == null ) {
				// Just adding the work unit - it's not associated with any persistent entity.
				workUnits.offer( vwu );
			}
			else {
				final String entityName = vwu.getEntityName();
				final Pair<String, Object> usedIdsKey = Pair.make( entityName, entityId );

				if ( usedIds.containsKey( usedIdsKey ) ) {
					final AuditWorkUnit other = usedIds.get( usedIdsKey );
					final AuditWorkUnit result = vwu.dispatch( other );

					if ( result != other ) {
						removeWorkUnit( other );

						if ( result != null ) {
							usedIds.put( usedIdsKey, result );
							workUnits.offer( result );
						}
						// else: a null result means that no work unit should be kept
					}
					// else: the result is the same as the work unit already added. No need to do anything.
				}
				else {
					usedIds.put( usedIdsKey, vwu );
					workUnits.offer( vwu );
				}
			}
		}
	}

	public Object getCurrentRevisionData(SharedSessionContractImplementor session, boolean persist) {
		// Generating the revision data if not yet generated
		if ( revisionData == null ) {
			revisionData = revisionInfoGenerator.generate();
		}

		// Saving the revision data, if not yet saved and persist is true
		if ( session instanceof SessionImplementor statefulSession ) {
			if ( persist && !statefulSession.contains( revisionData ) ) {
				revisionInfoGenerator.saveRevisionData( session, revisionData );
			}
		}
		else if ( session instanceof StatelessSessionImplementor statelessSession ) {
			if ( persist && !revisionDataSaved ) {
				revisionInfoGenerator.saveRevisionData( session, revisionData );
				revisionDataSaved = true;
			}
		}

		return revisionData;
	}

	@Override
	public void doBeforeTransactionCompletion(SharedSessionContractImplementor session) {
		if ( workUnits.isEmpty() && undoQueue.isEmpty() ) {
			return;
		}

		if ( !session.getTransactionCoordinator().isActive() ) {
			log.debug( "Skipping envers transaction hook due to non-active (most likely marked-rollback-only) transaction" );
			return;
		}

		if ( session instanceof StatelessSessionImplementor statelessSession ) {
			if ( statelessSession.isClosed() ) {
				try (StatelessSessionImplementor temporarySession = (StatelessSessionImplementor) statelessSession.statelessWithOptions()
						.connection()
						.noSessionInterceptorCreation()
						.open()) {
					executeInStatelessSession( temporarySession );
				}
			}
			else {
				executeInStatelessSession( statelessSession );
			}
		}
		else if ( FlushMode.MANUAL.equals( session.getHibernateFlushMode() ) || session.isClosed() ) {
			assert session instanceof SessionImplementor;
			final SessionImplementor statefulSession = (SessionImplementor) session;
			try (SessionImplementor temporarySession = (SessionImplementor) statefulSession.sessionWithOptions()
					.connection()
					.autoClose( false )
					.connectionHandling( AS_NEEDED, AFTER_TRANSACTION )
					.noSessionInterceptorCreation()
					.openSession()) {
				executeInSession( temporarySession );
				temporarySession.flush();
			}
		}
		else {
			executeInSession( (SessionImplementor) session );

			// Explicitly flushing the session, as the auto-flush may have already happened.
			session.flush();
		}
	}

	private void executeInSession(SessionImplementor statefulSession) {
		// Making sure the revision data is persisted.
		final Object currentRevisionData = getCurrentRevisionData( statefulSession, true );

		AuditWorkUnit vwu;

		// First undoing any performed work units
		while ( (vwu = undoQueue.poll()) != null ) {
			vwu.undo( statefulSession );
		}

		while ( (vwu = workUnits.poll()) != null ) {
			vwu.perform( statefulSession, revisionData );
			entityChangeNotifier.entityChanged( statefulSession, currentRevisionData, vwu );
		}
	}

	private void executeInStatelessSession(StatelessSessionImplementor statelessSession) {
		// Making sure the revision data is persisted.
		final Object currentRevisionData = getCurrentRevisionData( statelessSession, true );

		AuditWorkUnit vwu;

		// First undoing any performed work units
		while ( (vwu = undoQueue.poll()) != null ) {
			vwu.undo( statelessSession );
		}

		while ( (vwu = workUnits.poll()) != null ) {
			vwu.perform( statelessSession, revisionData );
			entityChangeNotifier.entityChanged( statelessSession, currentRevisionData, vwu );
		}
	}
}
