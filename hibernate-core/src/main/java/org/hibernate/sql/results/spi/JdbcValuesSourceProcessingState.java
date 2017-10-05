/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.function.Function;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;

/**
 * Provides a context for processing the processing of the complete
 * set of rows from a JdbcValuesSource.  Holds in-flight state
 * and provides access to environmental information needed to perform the
 * processing.
 * <p/>
 * todo (6.0) : look at making these hierarchical for nested loads.
 * 		- The reason being that the perf team had once identified TwoPhaseLoad#addUninitializedEntity
 * 			as a hot-spot.  That method is used to add a placeholder into the PC indicating that the
 * 			entity is being loaded (effectively we put the entity twice into a Map).  An alternative
 * 			would be to keep that relative to the ResultSetProcessingState and have load look here
 * 			for entities that are still in the process of being loaded; not sure that is any more efficient
 * 			however.
 * AFAIR, this ^^ is actually handled vis `#registerLoadingEntity` and
 * 		`org.hibernate.sql.results.internal.JdbcValuesSourceProcessingStateStandardImpl#loadingEntityMap`.
 *
 * @author Steve Ebersole
 */
public interface JdbcValuesSourceProcessingState {
	SharedSessionContractImplementor getPersistenceContext();

	QueryOptions getQueryOptions();

	JdbcValuesSourceProcessingOptions getProcessingOptions();

	void registerLoadingEntity(
			EntityKey entityKey,
			Function<EntityKey,LoadingEntityEntry> entryProducer);

	void finishUp();
}
