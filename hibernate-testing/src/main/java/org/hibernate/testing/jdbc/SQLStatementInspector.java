/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.jdbc;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.sql.JoinType;

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

	public void assertNumberOfJoins(int queryNumber, JoinType joinType, int expectedNumberOfOccurrences) {
		String query = sqlQueries.get( queryNumber );
		String[] parts = query.split( " join " );
		int actual = getCount( parts, joinType );
		assertThat( "number of " + joinType + "join", actual, is( expectedNumberOfOccurrences ) );
	}

	private int getCount(String[] parts, JoinType joinType) {
		final int end = parts.length - 1;
		int count = 0;
		for ( int i = 0; i < end; i++ ) {
			if ( parts[i].endsWith( " " + joinType.getSqlText() ) ) {
				count++;
			}
		}
		return count;
	}

	public void assertNumberOfOccurrenceInQuery(int queryNumber, String toCheck, int expectedNumberOfOccurrences) {
		String query = sqlQueries.get( queryNumber );
		int actual = query.split( " " + toCheck + " ", -1 ).length - 1;
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

	public static SQLStatementInspector extractFromSession(SessionImplementor session) {
		return (SQLStatementInspector) session.getJdbcSessionContext().getStatementInspector();
	}
}
