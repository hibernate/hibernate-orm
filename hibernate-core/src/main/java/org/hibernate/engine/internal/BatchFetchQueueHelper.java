/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

import static java.util.Arrays.asList;

/**
 * @author Gail Badner
 */
public class BatchFetchQueueHelper {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			BatchFetchQueueHelper.class.getName()
	);

	private BatchFetchQueueHelper() {
	}

	/**
	 * Finds the IDs for entities that were not found when the batch was loaded, and removes
	 * the corresponding entity keys from the {@link BatchFetchQueue}.
	 *
	 * @param ids - the IDs for the entities that were batch loaded
	 * @param results - the results from loading the batch
	 * @param persister - the entity persister for the entities in batch
	 * @param session - the session
	 */
	public static void removeNotFoundBatchLoadableEntityKeys(
			Serializable[] ids,
			List<?> results,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		if (  ids.length != results.size()
				&& session.getLoadQueryInfluencers().effectivelyBatchLoadable( persister ) ) {
			LOG.debug( "Not all entities were loaded." );
			final Set<Serializable> idSet = new HashSet<>( asList( ids ) );
			for ( Object result : results ) {
				// All results should be in the PersistenceContext
				idSet.remove( session.getContextEntityIdentifier( result ) );
			}
			if ( LOG.isDebugEnabled() ) {
				LOG.debug( "Entities of type [" + persister.getEntityName() + "] not found; IDs: " + idSet );
			}
			for ( Object id : idSet ) {
				removeBatchLoadableEntityKey( id, persister, session );
			}
		}
	}

	/**
	 * Remove the entity key with the specified {@code id} and {@code persister} from
	 * the batch loadable entities {@link BatchFetchQueue}.
	 */
	public static void removeBatchLoadableEntityKey(
			Object id,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		session.getPersistenceContextInternal().getBatchFetchQueue()
				.removeBatchLoadableEntityKey( session.generateEntityKey( id, persister ) );
	}

	/**
	 * Remove the entity key with the specified {@code id} and {@code persister} from
	 * the batch loadable entities {@link BatchFetchQueue}.
	 */
	public static void removeBatchLoadableEntityKey(
			EntityKey entityKey,
			SharedSessionContractImplementor session) {
		session.getPersistenceContextInternal().getBatchFetchQueue().removeBatchLoadableEntityKey( entityKey );
	}

	public static void removeBatchLoadableEntityKey(
			Object id,
			EntityMappingType entityMappingType,
			SharedSessionContractImplementor session) {
		removeBatchLoadableEntityKey( id, entityMappingType.getEntityPersister(), session );
	}
}
