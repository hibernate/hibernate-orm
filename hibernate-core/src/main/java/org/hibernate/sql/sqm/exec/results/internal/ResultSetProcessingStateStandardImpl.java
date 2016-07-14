/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.results.internal;

import java.sql.ResultSet;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.sqm.convert.spi.Return;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingState;
import org.hibernate.sql.sqm.exec.results.spi.RowProcessingState;
import org.hibernate.sql.sqm.exec.spi.QueryOptions;

/**
 * @author Steve Ebersole
 */
public class ResultSetProcessingStateStandardImpl implements ResultSetProcessingState {
	private final ResultSet resultSet;
	private final SharedSessionContractImplementor session;

	private RowProcessingState currentRowState;

	public ResultSetProcessingStateStandardImpl(
			ResultSet resultSet,
			QueryOptions queryOptions,
			List<Return> returns,
			SharedSessionContractImplementor session) {
		this.resultSet = resultSet;
		this.session = session;

		currentRowState = new RowProcessingStateStandardImpl( this, returns, queryOptions );
	}

	@Override
	public ResultSet getResultSet() {
		return resultSet;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return session;
	}

	@Override
	public RowProcessingState getCurrentRowProcessingState() {
		return currentRowState;
	}

	@Override
	public void finishResultSetProcessing() {
		// for now, nothing to do...
	}

	@Override
	public void release() {
		// for now, nothing to do...
	}
}
