/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.transform.ResultTransformer;

/**
 * Contract for processing JDBC ResultSets.  Separated because ResultSets can be chained and we'd really like to
 * reuse this logic across all result sets.
 * <p/>
 * todo : investigate having this work with non-JDBC results; maybe just typed as Object? or a special Result contract?
 *
 * @author Steve Ebersole
 */
public interface ResultSetProcessor {

	/**
	 * Make this go somewhere else.  These aren't really linked this way anymore.  ScrollableResultSetProcessor is
	 * not tied in yet, so not sure yet exactly how that will play out.
	 *
	 * @deprecated Going away!
	 */
	@Deprecated
	public ScrollableResultSetProcessor toOnDemandForm();

	/**
	 * Process an entire ResultSet, performing all extractions.
	 *
	 * Semi-copy of {@link org.hibernate.loader.Loader#doQuery}, with focus on just the ResultSet processing bit.
	 *
	 * @param resultSet The result set being processed.
	 * @param session The originating session
	 * @param queryParameters The "parameters" used to build the query
	 * @param returnProxies Can proxies be returned (not the same as can they be created!)
	 * @param forcedResultTransformer My old "friend" ResultTransformer...
	 * @param afterLoadActions Actions to be performed after loading an entity.
	 *
	 * @return The extracted results list.
	 *
	 * @throws java.sql.SQLException Indicates a problem access the JDBC ResultSet
	 */
	public List extractResults(
			ResultSet resultSet,
			SessionImplementor session,
			QueryParameters queryParameters,
			NamedParameterContext namedParameterContext,
			boolean returnProxies,
			boolean readOnly,
			ResultTransformer forcedResultTransformer,
			List<AfterLoadAction> afterLoadActions) throws SQLException;
}
