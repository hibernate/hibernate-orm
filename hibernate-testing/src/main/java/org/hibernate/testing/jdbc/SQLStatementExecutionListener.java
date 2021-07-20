/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementExecutionListener;

public class SQLStatementExecutionListener {

	private final List<StatementExecution> statementExecutions = new ArrayList<>();

	public SQLStatementExecutionListener(SessionFactoryBuilder sessionFactoryBuilder) {
		sessionFactoryBuilder.applyStatementExecutionListener( (sql, startTimeNanos) -> {
			statementExecutions.add( new StatementExecution( sql, startTimeNanos ) );
		} );
	}

	public SQLStatementExecutionListener(Map settings) {
		settings.put( AvailableSettings.STATEMENT_EXECUTION_LISTENER, (StatementExecutionListener) (sql, startTimeNanos) -> {
			statementExecutions.add( new StatementExecution( sql, startTimeNanos ) );
		} );
	}

	public List<StatementExecution> getStatementExecutions() {
		return statementExecutions;
	}

	public void clear() {
		statementExecutions.clear();
	}

	public static class StatementExecution {
		
		private final String query;
		
		private final long startTimeNanos;

		StatementExecution(String query, long startTimeNanos) {
			this.query = query;
			this.startTimeNanos = startTimeNanos;
		}

		public String getQuery() {
			return query;
		}

		public long getStartTimeNanos() {
			return startTimeNanos;
		}
		
	}

}
