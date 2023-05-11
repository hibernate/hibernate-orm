/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12469")
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

	@Test
	public void testInClauseParameterNoPaddingAtLimit(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id in :ids" )
				.setParameter( "ids", integerRangeList( 0, MAX_COUNT ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );

		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( expectedInClause.toString() );
	}

	@TestForIssue(jiraKey = "HHH-14109")
	@Test
	public void testInClauseParameterPaddingToLimit(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id in :ids" )
				.setParameter( "ids", integerRangeList( 0, 10 ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );

		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( expectedInClause.toString() );
	}

	@Test
	public void testInClauseParameterSplittingAfterLimit(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id in :ids" )
				.setParameter( "ids", integerRangeList( 0, 16 ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where (p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );
		expectedInClause.append( " or p1_0.id in " );
		appendInClause( expectedInClause, 1 );
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
		appendInClause( expectedInClause, 1 );

		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( expectedInClause.toString() );
	}

	@Test
	public void testInClauseParameterSplittingAfterLimit2(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id in :ids" )
				.setParameter( "ids", integerRangeList( 0, 18 ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where (p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );
		expectedInClause.append( " or p1_0.id in " );
		appendInClause( expectedInClause, 4 );
		expectedInClause.append( ')' );

		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( expectedInClause.toString() );
	}

	@Test
	public void testInClauseParameterSplittingAfterLimit3(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id in :ids" )
				.setParameter( "ids", integerRangeList( 0, 33 ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where (p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );
		expectedInClause.append( " or p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );
		expectedInClause.append( " or p1_0.id in " );
		appendInClause( expectedInClause, 4 );
		expectedInClause.append( ')' );

		assertThat( statementInspector.getSqlQueries().get( 0 ) ).endsWith( expectedInClause.toString() );
	}

	@TestForIssue(jiraKey = "HHH-16589")
	@Test
	public void testInClauseParameterSplittingAfterLimit4(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> entityManager
				.createQuery( "select p from Person p where p.id in :ids" )
				.setParameter( "ids", integerRangeList( 0, 39 ) )
				.getResultList() );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "where (p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );
		expectedInClause.append( " or p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );
		expectedInClause.append( " or p1_0.id in " );
		appendInClause( expectedInClause, MAX_COUNT );
		expectedInClause.append( ')' );

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
