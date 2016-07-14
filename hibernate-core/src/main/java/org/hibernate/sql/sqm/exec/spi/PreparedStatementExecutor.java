/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.spi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.sqm.convert.spi.Return;

/**
 * General contract for executing a PreparedStatement and consuming the "results" of that
 * execution.  That might mean reading the rows of a {@link ResultSet} obtained from executing
 * a query.  Or in the case of a {@link org.hibernate.ScrollableResults} e.g. it might just
 * mean holding the results open and handing them to the ScrollableResults object.
 * <p/>
 * todo : ideally this also caters to consuming ProcedureCall executions as well.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface PreparedStatementExecutor<R,T> {
	/**
	 * Do the consumption
	 *
	 * @param ps The PreparedStatement that the ResultSet was obtained from (mainly
	 * used to interact with the ResourceRegistry)
	 * @param returns
	 * @param rowTransformer
	 * @param queryOptions
	 * @param session
	 *
	 * @return
	 *
	 * @throws SQLException
	 */
	R execute(
			PreparedStatement ps,
			QueryOptions queryOptions,
			List<Return> returns,
			RowTransformer<T> rowTransformer,
			SharedSessionContractImplementor session) throws SQLException;
}
