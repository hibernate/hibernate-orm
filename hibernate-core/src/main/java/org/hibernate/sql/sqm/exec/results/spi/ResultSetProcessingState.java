/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.results.spi;

import java.sql.ResultSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Provides a context for processing a ResultSet.  Holds in-flight state
 * and provides access to environmental information needed to perform the
 * processing.
 * <p/>
 * todo : look at making these hierarchical for nested loads.
 * The reason being that the perf team had once identified TwoPhaseLoad#addUninitializedEntity
 * as a hot-spot.  That method is used to add a placeholder into the PC indicating that the
 * entity is being loaded (effectively we put the entity twice into a Map).  An alternative
 * would be to keep that relative to the ResultSetProcessingState and have load look here
 * for entities that are still in the process of being loaded; not sure that is any more efficient
 * however.
 *
 * @author Steve Ebersole
 */
public interface ResultSetProcessingState {
	ResultSet getResultSet();
	SharedSessionContractImplementor getSession();

	RowProcessingState getCurrentRowProcessingState();

	void finishResultSetProcessing();

	void release();
}
