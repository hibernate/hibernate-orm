/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.hibernate.StatementObserver;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class CollectingStatementObserver implements StatementObserver {
	private List<Statement> statements = new ArrayList<>();

	@Override
	public void performingSql(String sql, int batchPosition) {
		statements.add( new Statement( sql, batchPosition ) );
	}

	public List<Statement> getStatements() {
		return statements;
	}

	public List<String> getSqlQueries() {
		return statements.stream().map( Statement::sql ).toList();
	}

	public void clear() {
		statements.clear();
	}

	public record Statement(String sql, int batchPosition) {
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AssertJ

	public ListAssert<Statement> assertStatements() {
		return assertThat( statements );
	}

	public ObjectAssert<Statement> assertStatement(int position) {
		return assertThat( statements.get( position ) );
	}

	public ListAssert<String> assertQueries() {
		return assertThat( getSqlQueries() );
	}

	public QueryAssert assertQuery(int position) {
		return new QueryAssert( statements.get( position ).sql() );
	}

	public static class QueryAssert extends AbstractStringAssert<QueryAssert> {
		protected QueryAssert(String sql) {
			super( sql, QueryAssert.class );
		}

		public QueryAssert containsToken(String token) {
			return contains( token );
		}

		public QueryAssert containsToken(String token, int numberOfTimes) {
			var matches = actual.split( token, -1 );
			assertThat( matches.length - 1 )
					.as( "check `%s` contains `%s` %d times", actual, token, numberOfTimes )
					.isEqualTo( numberOfTimes );
			return myself;
		}


	}
}
