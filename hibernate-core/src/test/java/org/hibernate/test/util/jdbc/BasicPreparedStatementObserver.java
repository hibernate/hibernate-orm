/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util.jdbc;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 * @author Gail Badner
 */
public class BasicPreparedStatementObserver implements PreparedStatementObserver {
	private final Map<PreparedStatement, String> sqlByPreparedStatement = new LinkedHashMap<PreparedStatement, String>();

	@Override
	public void preparedStatementCreated(PreparedStatement preparedStatement, String sql) {
		sqlByPreparedStatement.put( preparedStatement, sql );
	}

	@Override
	public void preparedStatementMethodInvoked(
			PreparedStatement preparedStatement,
			Method method,
			Object[] args,
			Object invocationReturnValue) {
		// do nothing by default
	}

	@Override
	public PreparedStatement getPreparedStatement(String sql) {
		List<PreparedStatement> preparedStatements = getPreparedStatements( sql );
		if ( preparedStatements.isEmpty() ) {
			throw new IllegalArgumentException(
					"There is no PreparedStatement for this SQL statement " + sql );
		}
		else if ( preparedStatements.size() > 1 ) {
			throw new IllegalArgumentException( "There are " + preparedStatements
					.size() + " PreparedStatements for this SQL statement " + sql );
		}
		return preparedStatements.get( 0 );
	}

	@Override
	public List<PreparedStatement> getPreparedStatements(String sql) {
		final List<PreparedStatement> preparedStatements = new ArrayList<PreparedStatement>();
		for ( Map.Entry<PreparedStatement,String> entry : sqlByPreparedStatement.entrySet() ) {
			if ( entry.getValue().equals( sql ) ) {
				preparedStatements.add( entry.getKey() );
			}
		}
		return preparedStatements;
	}

	@Override
	public List<PreparedStatement> getPreparedStatements() {
		return new ArrayList<PreparedStatement>( sqlByPreparedStatement.keySet() );
	}

	@Override
	public void connectionProviderStopped() {
		sqlByPreparedStatement.clear();
	}
}
