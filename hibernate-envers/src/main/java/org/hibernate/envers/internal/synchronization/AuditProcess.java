/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.synchronization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.persistence.FlushModeType;

import org.hibernate.Session;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.tools.Pair;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.jboss.logging.Logger;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class AuditProcess implements BeforeTransactionCompletionProcess {
	private static final Logger log = Logger.getLogger( AuditProcess.class );

	private final RevisionInfoGenerator revisionInfoGenerator;
	private final SessionImplementor session;

	private final LinkedList<AuditWorkUnit> workUnits;
	private final Queue<AuditWorkUnit> undoQueue;
	private final Map<Pair<String, Object>, AuditWorkUnit> usedIds;
	private final Map<Pair<String, Object>, Object[]> entityStateCache;
	private final EntityChangeNotifier entityChangeNotifier;
	private Object revisionData;

	public AuditProcess(RevisionInfoGenerator revisionInfoGenerator, SessionImplementor session) {
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

	private void executeInSession(Session session) {
		// Making sure the revision data is persisted.
		final Object currentRevisionData = getCurrentRevisionData( session, true );

		AuditWorkUnit vwu;

		// First undoing any performed work units
		while ( (vwu = undoQueue.poll()) != null ) {
			vwu.undo( session );
		}

		while ( (vwu = workUnits.poll()) != null ) {
			vwu.perform( session, revisionData );
			entityChangeNotifier.entityChanged( session, currentRevisionData, vwu );
		}
	}

	public Object getCurrentRevisionData(Session session, boolean persist) {
		// Generating the revision data if not yet generated
		if ( revisionData == null ) {
			revisionData = revisionInfoGenerator.generate();
		}

		// Saving the revision data, if not yet saved and persist is true
		if ( !session.contains( revisionData ) && persist ) {
			revisionInfoGenerator.saveRevisionData( session, revisionData );
		}

		return revisionData;
	}

	@Override
	public void doBeforeTransactionCompletion(SessionImplementor session) {
		if ( workUnits.size() == 0 && undoQueue.size() == 0 ) {
			return;
		}

		if ( !session.getTransactionCoordinator().isActive() ) {
			log.debug( "Skipping envers transaction hook due to non-active (most likely marked-rollback-only) transaction" );
			return;
		}

		// see: http://www.jboss.com/index.html?module=bb&op=viewtopic&p=4178431
		if ( FlushModeType.COMMIT.equals( session.getFlushMode() ) || session.isClosed() ) {
			Session temporarySession = null;
			try {
				temporarySession = session.sessionWithOptions()
						.connection()
						.autoClose( false )
						.connectionHandlingMode( PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION )
						.openSession();
				executeInSession( temporarySession );
				temporarySession.flush();
			}
			finally {
				if ( temporarySession != null ) {
					temporarySession.close();
				}
			}
		}
		else {
			executeInSession( session );

			// Explicitly flushing the session, as the auto-flush may have already happened.
			session.flush();
		}
	}
}
