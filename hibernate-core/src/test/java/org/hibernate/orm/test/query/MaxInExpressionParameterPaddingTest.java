/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12469")
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = { MaxInExpressionParameterPaddingTest.Person.class },
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SQL_COMMENTS, value = "true"),
				@Setting(name = AvailableSettings.IN_CLAUSE_PARAMETER_PADDING, value = "true"),
				@Setting(name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "false"),
		},
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.DIALECT,
						provider = MaxInExpressionParameterPaddingTest.DialectProvider.class
				)
		},
		useCollectingStatementInspector = true
)
public class MaxInExpressionParameterPaddingTest {

	public static class DialectProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return MaxInExpressionParameterPaddingTest.MaxCountInExpressionH2Dialect.class.getName();
		}
	}

	public static final int MAX_COUNT = 15;

	@BeforeAll
	protected void afterEntityManagerFactoryBuilt(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			for ( int i = 0; i < MAX_COUNT; i++ ) {
				Person person = new Person();
				person.setId( i );
				person.setName( String.format( "Person nr %d", i ) );

				entityManager.persist( person );
			}
		} );
	}

	@Test
	public void testInClauseParameterPadding(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id in :ids" )
				.setParameter( "ids", integerRangeList( 0, 5 ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where p1_0.id in " );
		appendInClause( expectedInClause, 8 );

		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( expectedInClause.toString() );
	}

	static Stream<Arguments> testInClauseParameterPaddingUpToLimit() {
		return Stream.of(
				arguments( 1, 1 ),
				arguments( 2, 2 ),
				arguments( 3, 4 ),
				arguments( 4, 4 ),
				arguments( 5, 8 ),
				arguments( 6, 8 ),
				arguments( 7, 8 ),
				arguments( 8, 8 ),
				arguments( 9, 15 ),
				arguments( 10, 15 ), // Test for HHH-14109
				arguments( 11, 15 ),
				arguments( 12, 15 ),
				arguments( 13, 15 ),
				arguments( 14, 15 ),
				arguments( 15, 15 ) );
	}

	@JiraKey(value = "HHH-16826")
	@ParameterizedTest
	@MethodSource
	public void testInClauseParameterPaddingUpToLimit(int parameters, int expectedBindVariables, EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id in :ids" )
				.setParameter( "ids", integerRangeList( 0, parameters ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where p1_0.id in " );
		appendInClause( expectedInClause, expectedBindVariables );

		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( expectedInClause.toString() );
	}

	static Stream<Arguments> testInClauseParameterPaddingAfterLimit() {
		return Stream.of(
				arguments( 16, 2 ),
				arguments( 18, 2 ),
				arguments( 33, 4 ),
				arguments( 39, 4 ), // Test for HHH-16589
				arguments( 4 * MAX_COUNT, 4 ),
				arguments( 4 * MAX_COUNT + 1, 8 ),
				arguments( 8 * MAX_COUNT, 8 ),
				arguments( 8 * MAX_COUNT + 1, 16 ) );
	}

	@JiraKey(value = "HHH-16826")
	@ParameterizedTest
	@MethodSource
	public void testInClauseParameterPaddingAfterLimit(int parameters, int expectedInClauses, EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id in :ids" )
				.setParameter( "ids", integerRangeList( 0, parameters ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where (p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );
		for ( int i = 1; i < expectedInClauses; i++ ) {
			expectedInClause.append( " or p1_0.id in " );
			appendInClause( expectedInClause, MAX_COUNT );
		}
		expectedInClause.append( ')' );

		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( expectedInClause.toString() );
	}

	@Test
	public void testInClauseParameterSplittingAfterLimitNotIn(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id not in :ids" )
				.setParameter( "ids", integerRangeList( 0, 16 ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where p1_0.id not in " );
		appendInClause( expectedInClause, MAX_COUNT );
		expectedInClause.append( " and p1_0.id not in " );
		appendInClause( expectedInClause, MAX_COUNT );

		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( expectedInClause.toString() );
	}

	private static List<Integer> integerRangeList(int start, int end) {
		return IntStream.range( start, end )
				.boxed()
				.collect( Collectors.toList() );
	}

	private void appendInClause(StringBuilder sql, int inClauseSize) {
		sql.append( "(?" );
		for ( int i = 1; i < inClauseSize; i++ ) {
			sql.append( ",?" );
		}
		sql.append( ')' );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class MaxCountInExpressionH2Dialect extends H2Dialect {

		public MaxCountInExpressionH2Dialect() {
		}

		@Override
		public int getInExpressionCountLimit() {
			return MAX_COUNT;
		}
	}

}
