package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Merge coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 * <p>
 * An upsert does not reveal whether it inserted or updated the row of
 * the table holding current data. The upserted entity is reconciled
 * with its history table in the same way either way: the open revision
 * is ended if there is one, and the row is copied to a new revision.
 *
 * @author Gavin King
 */
@org.hibernate.Internal
public class MergeCoordinatorHistory extends UpdateCoordinatorHistory {
	public MergeCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull UpdateCoordinator currentMergeCoordinator) {
		super( entityPersister, factory, currentMergeCoordinator );
	}
}
