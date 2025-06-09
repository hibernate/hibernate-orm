/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.sql.ast.SqlAstJoinType;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.assertj.core.api.Assertions;

/**
 * @author Andrea Boriero
 */
public class SQLStatementInspector implements StatementInspector {
	private final List<String> sqlQueries = new LinkedList<>();

	public SQLStatementInspector() {
	}

	@Override
	public String inspect(String sql) {
		sqlQueries.add( sql );
		return sql;
	}

	public List<String> getSqlQueries() {
		return sqlQueries;
	}

	public void clear() {
		sqlQueries.clear();
	}

	public int getNumberOfJoins(int position) {
		final String sql = sqlQueries.get( position );
		String fromPart = sql.toLowerCase( Locale.ROOT ).split( " from " )[1].split( " where " )[0];
		return fromPart.split( "(\\sjoin\\s|,\\s)", -1 ).length - 1;
	}

	public void assertExecuted(String expected) {
		assertTrue( sqlQueries.contains( expected ) );
	}

	public void assertNumberOfJoins(int queryNumber, int expectedNumberOfJoins) {
		assertNumberOfOccurrenceInQuery( queryNumber, "join", expectedNumberOfJoins );
	}

	public void assertExecutedCount(int expected) {
		assertEquals( "Number of executed statements ",expected, sqlQueries.size() );
	}

	public void assertNumberOfJoins(int queryNumber, SqlAstJoinType joinType, int expectedNumberOfOccurrences) {
		String query = sqlQueries.get( queryNumber );
		String[] parts = query.split( " join " );
		int actual = getCount( parts, joinType );
		assertThat( "number of " + joinType.getText() + "join", actual, is( expectedNumberOfOccurrences ) );
	}

	private int getCount(String[] parts, SqlAstJoinType joinType) {
		final int end = parts.length - 1;
		int count = 0;
		for ( int i = 0; i < end; i++ ) {
			if ( parts[i].endsWith( " left" ) ) {
				count += joinType == SqlAstJoinType.LEFT ? 1 : 0;
			}
			else if ( parts[i].endsWith( " right" ) ) {
				count += joinType == SqlAstJoinType.RIGHT ? 1 : 0;
			}
			else if ( parts[i].endsWith( " full" ) ) {
				count += joinType == SqlAstJoinType.FULL ? 1 : 0;
			}
			else if ( parts[i].endsWith( " cross" ) ) {
				count += joinType == SqlAstJoinType.CROSS ? 1 : 0;
			}
			else {
				count += joinType == SqlAstJoinType.INNER ? 1 : 0;
			}
		}
		return count;
	}

	public void assertNumberOfOccurrenceInQuery(int queryNumber, String toCheck, int expectedNumberOfOccurrences) {
		assertNumberOfOccurrenceInQueryNoSpace( queryNumber, " " + toCheck + " ", expectedNumberOfOccurrences );
	}

	public void assertNumberOfOccurrenceInQueryNoSpace(int queryNumber, String toCheck, int expectedNumberOfOccurrences) {
		String query = sqlQueries.get( queryNumber );
		int actual = query.split( toCheck, -1 ).length - 1;
		assertThat( "number of " + toCheck, actual, is( expectedNumberOfOccurrences ) );
	}

	public void assertIsSelect(int queryNumber) {
		String query = sqlQueries.get( queryNumber );
		assertTrue( query.toLowerCase( Locale.ROOT ).startsWith( "select" ) );
	}

	public void assertIsInsert(int queryNumber) {
		String query = sqlQueries.get( queryNumber );
		assertTrue( query.toLowerCase( Locale.ROOT ).startsWith( "insert" ) );
	}

	public void assertIsUpdate(int queryNumber) {
		String query = sqlQueries.get( queryNumber );
		assertTrue( query.toLowerCase( Locale.ROOT ).startsWith( "update" ) );
	}

	public void assertNoUpdate() {
		Assertions.assertThat( sqlQueries )
				.isNotEmpty()
				.allSatisfy( sql -> Assertions.assertThat( sql.toLowerCase( Locale.ROOT ) ).doesNotStartWith( "update" ) );
	}

	public void assertUpdate() {
		Assertions.assertThat( sqlQueries )
				.isNotEmpty()
				.anySatisfy( sql -> Assertions.assertThat( sql.toLowerCase( Locale.ROOT ) ).startsWith( "update" ) );
	}

	public void assertInsert() {
		Assertions.assertThat( sqlQueries )
				.isNotEmpty()
				.anySatisfy( sql -> Assertions.assertThat( sql.toLowerCase( Locale.ROOT ) ).startsWith( "insert" ) );
	}

	public static SQLStatementInspector extractFromSession(SessionImplementor session) {
		return (SQLStatementInspector) session.getJdbcSessionContext().getStatementInspector();
	}

	public void assertHasQueryMatching(String queryPattern) {
		Assertions.assertThat( sqlQueries )
				.isNotEmpty()
				.anySatisfy( sql -> Assertions.assertThat( sql.toLowerCase( Locale.ROOT ) ).matches( queryPattern ) );
	}
}
