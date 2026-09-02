/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.List;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.spi.Stack;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Distinct;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.function.spi.TupleCountSupport.Syntax.ARGUMENT_LIST;
import static org.hibernate.dialect.function.spi.TupleCountSupport.Syntax.PARENTHESIZED_TUPLE;
import static org.hibernate.dialect.function.spi.TupleCountSupport.Syntax.UNSUPPORTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable tuple-count-syntax provider contract, maintained
/// Dialect profiles, and every native or emulated CountFunction path.
///
/// @author Steve Ebersole
public class TupleCountSupportTests {
	@Test
	void constantsAndBuildersExposeIndependentImmutableSyntaxChoices() {
		assertProfile( TupleCountSupport.NONE, UNSUPPORTED, UNSUPPORTED );
		assertProfile( TupleCountSupport.STANDARD, UNSUPPORTED, ARGUMENT_LIST );

		final TupleCountSupport copied = TupleCountSupport.builder( TupleCountSupport.STANDARD )
				.nonDistinctSyntax( PARENTHESIZED_TUPLE )
				.distinctSyntax( UNSUPPORTED )
				.build();
		assertProfile( copied, PARENTHESIZED_TUPLE, UNSUPPORTED );
		assertProfile( TupleCountSupport.STANDARD, UNSUPPORTED, ARGUMENT_LIST );
	}

	@Test
	@SuppressWarnings("NullAway")
	void buildersRejectNullInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> TupleCountSupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> TupleCountSupport.builder().nonDistinctSyntax( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> TupleCountSupport.builder().distinctSyntax( null ) );
	}

	@Test
	void maintainedDialectsPreserveEveryTupleCountForm() {
		assertProfile(
				new Dialect( DatabaseVersion.make( 1 ) ) {
				}.getTupleCountSupport(),
				UNSUPPORTED,
				ARGUMENT_LIST
		);
		assertProfile( new HSQLDialect().getTupleCountSupport(), PARENTHESIZED_TUPLE, ARGUMENT_LIST );
		for ( Dialect dialect : List.of( new H2Dialect(), new PostgreSQLDialect(), new CockroachDialect() ) ) {
			assertProfile( dialect.getTupleCountSupport(), PARENTHESIZED_TUPLE, PARENTHESIZED_TUPLE );
		}
		for ( Dialect dialect : List.of(
				new SQLServerDialect(),
				new DB2Dialect(),
				new OracleDialect(),
				new SpannerDialect(),
				new SpannerPostgreSQLDialect() ) ) {
			assertProfile( dialect.getTupleCountSupport(), UNSUPPORTED, UNSUPPORTED );
		}
		for ( Dialect dialect : List.of( new MySQLDialect(), new MariaDBDialect() ) ) {
			assertProfile( dialect.getTupleCountSupport(), UNSUPPORTED, ARGUMENT_LIST );
		}
	}

	@Test
	void countFunctionConsumesAllThreeSyntaxChoicesForBothForms() {
		for ( TupleCountSupport.Syntax syntax : TupleCountSupport.Syntax.values() ) {
			final TupleCountSupport ordinary = TupleCountSupport.builder()
					.nonDistinctSyntax( syntax )
					.build();
			final String ordinarySql = render( ordinary, false, false );
			switch ( syntax ) {
				case UNSUPPORTED -> assertThat( ordinarySql )
						.isEqualTo( "count(case when a is not null and b is not null then 1 else null end)" );
				case ARGUMENT_LIST -> assertThat( ordinarySql ).isEqualTo( "count(a,b)" );
				case PARENTHESIZED_TUPLE -> assertThat( ordinarySql ).isEqualTo( "count((a,b))" );
			}

			final TupleCountSupport distinct = TupleCountSupport.builder()
					.distinctSyntax( syntax )
					.build();
			final String distinctSql = render( distinct, true, false );
			switch ( syntax ) {
				case UNSUPPORTED -> assertThat( distinctSql )
						.startsWith( "count(distinct coalesce(nullif(coalesce(a||''" )
						.contains( "b||''" )
						.endsWith( "))" );
				case ARGUMENT_LIST -> assertThat( distinctSql ).isEqualTo( "count(distinct a,b)" );
				case PARENTHESIZED_TUPLE -> assertThat( distinctSql ).isEqualTo( "count(distinct (a,b))" );
			}
		}
	}

	@Test
	void tupleSyntaxComposesWithNativeAggregateFiltering() {
		final TupleCountSupport support = TupleCountSupport.builder()
				.nonDistinctSyntax( PARENTHESIZED_TUPLE )
				.build();
		assertThat( render( support, false, true ) ).isEqualTo( "count((a,b)) filter (where p)" );
	}

	@Test
	void tupleSyntaxComposesWithEmulatedAggregateFiltering() {
		final TupleCountSupport support = TupleCountSupport.builder()
				.nonDistinctSyntax( PARENTHESIZED_TUPLE )
				.build();
		assertThat( render( support, false, true, false ) )
				.isEqualTo( "count((case when p then 1 else null end,a,b))" );
	}

	@SuppressWarnings("unchecked")
	private static String render(TupleCountSupport support, boolean distinct, boolean filtered) {
		return render( support, distinct, filtered, true );
	}

	@SuppressWarnings("unchecked")
	private static String render(
			TupleCountSupport support,
			boolean distinct,
			boolean filtered,
			boolean nativeFilterClause) {
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public TupleCountSupport getTupleCountSupport() {
				return support;
			}

			@Override
			public boolean supportsFilterClause() {
				return nativeFilterClause;
			}
		};
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		final CountFunction countFunction = new CountFunction(
				dialect,
				typeConfiguration,
				SqlAstNodeRenderingMode.DEFAULT,
				"||"
		);
		final Expression first = mock( Expression.class );
		final Expression second = mock( Expression.class );
		final SqlTuple tuple = new SqlTuple( List.of( first, second ), null );
		final SqlAstNode argument = distinct ? new Distinct( tuple ) : tuple;
		final Predicate filter = filtered ? mock( Predicate.class ) : null;
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		final SqlAstTranslator<?> translator = mock( SqlAstTranslator.class );
		final Stack<Clause> clauseStack = mock( Stack.class );
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		final QueryEngine queryEngine = mock( QueryEngine.class );
		final SqmFunctionRegistry functionRegistry = new SqmFunctionRegistry();
		functionRegistry.patternDescriptorBuilder( "chr", "chr(?1)" ).setExactArgumentCount( 1 ).register();
		when( translator.getSessionFactory() ).thenReturn( sessionFactory );
		when( translator.getCurrentClauseStack() ).thenReturn( clauseStack );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		when( sessionFactory.getQueryEngine() ).thenReturn( queryEngine );
		when( queryEngine.getSqmFunctionRegistry() ).thenReturn( functionRegistry );
		when( sessionFactory.getTypeConfiguration() ).thenReturn( typeConfiguration );
		doAnswer( invocation -> {
			final SqlAstNode node = invocation.getArgument( 0 );
			if ( node == tuple ) {
				appender.appendSql( "(a,b)" );
			}
			else if ( node == first ) {
				appender.appendSql( "a" );
			}
			else if ( node == second ) {
				appender.appendSql( "b" );
			}
			else if ( node instanceof QueryLiteral<?> literal ) {
				appender.appendSql( literal.getLiteralValue().toString() );
			}
			return null;
		} ).when( translator ).render( any( SqlAstNode.class ), eq( SqlAstNodeRenderingMode.DEFAULT ) );
		if ( filter != null ) {
			doAnswer( invocation -> {
				appender.appendSql( "p" );
				return null;
			} ).when( filter ).accept( translator );
		}

		countFunction.render( appender, List.of( argument ), filter, null, translator );
		return appender.toString();
	}

	private static void assertProfile(
			TupleCountSupport profile,
			TupleCountSupport.Syntax nonDistinct,
			TupleCountSupport.Syntax distinct) {
		assertThat( profile.getNonDistinctSyntax() ).isEqualTo( nonDistinct );
		assertThat( profile.getDistinctSyntax() ).isEqualTo( distinct );
	}
}
