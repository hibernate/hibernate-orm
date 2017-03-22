/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.test.util.jdbc.BasicPreparedStatementObserver;

/**
 * @author Gail Badner
 */
class BatchCountingPreparedStatementObserver extends BasicPreparedStatementObserver {
	private final Map<PreparedStatement, Integer> batchesAddedByPreparedStatement = new LinkedHashMap<PreparedStatement, Integer>();

	@Override
	public void preparedStatementCreated(PreparedStatement preparedStatement, String sql) {
		super.preparedStatementCreated( preparedStatement, sql );
		batchesAddedByPreparedStatement.put( preparedStatement, 0 );
	}

	@Override
	public void preparedStatementMethodInvoked(
			PreparedStatement preparedStatement,
			Method method,
			Object[] args,
			Object invocationReturnValue) {
		super.preparedStatementMethodInvoked( preparedStatement, method, args, invocationReturnValue );
		if ( "addBatch".equals( method.getName() ) ) {
			batchesAddedByPreparedStatement.put(
					preparedStatement,
					batchesAddedByPreparedStatement.get( preparedStatement ) + 1
			);
		}
	}

	public int getNumberOfBatchesAdded(PreparedStatement preparedStatement) {
		return batchesAddedByPreparedStatement.get( preparedStatement );
	}

	public void connectionProviderStopped() {
		super.connectionProviderStopped();
		batchesAddedByPreparedStatement.clear();
	}
}
