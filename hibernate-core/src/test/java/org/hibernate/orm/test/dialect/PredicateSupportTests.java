/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.List;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.function.EveryAnyEmulation;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.predicate.ThruthnessPredicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable predicate-support provider contract, maintained Dialect
/// profiles, and every selected consumer path.
///
/// @author Steve Ebersole
public class PredicateSupportTests {
	private static final PredicateSupport MIXED = PredicateSupport.builder( PredicateSupport.NONE )
			.caseInsensitiveLikeOperator( "ilike" )
			.capabilities(
					PredicateSupport.Capability.DISTINCT_FROM,
					PredicateSupport.Capability.TRUTHNESS
			)
			.build();

	@Test
	void constantsAndBuildersExposeImmutableIndependentDimensions() {
		assertProfile( PredicateSupport.NONE, null );
		assertProfile(
				PredicateSupport.STANDARD,
				null,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);

		final PredicateSupport copied = PredicateSupport.builder( MIXED )
				.noCaseInsensitiveLikeOperator()
				.capability( PredicateSupport.Capability.TRUTHNESS, false )
				.capability( PredicateSupport.Capability.EXPRESSION_PLACEMENT, true )
				.build();
		assertProfile(
				copied,
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				MIXED,
				"ilike",
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.TRUTHNESS
		);
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> copied.getCapabilities().add( PredicateSupport.Capability.TRUTHNESS ) );
	}

	@Test
	@SuppressWarnings("NullAway")
	void buildersRejectNullAndBlankInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> PredicateSupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> PredicateSupport.builder().caseInsensitiveLikeOperator( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> PredicateSupport.builder().caseInsensitiveLikeOperator( "  " ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> PredicateSupport.builder().capabilities( (PredicateSupport.Capability[]) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> PredicateSupport.builder().capabilities( PredicateSupport.Capability.DISTINCT_FROM, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> PredicateSupport.builder().capability( null, true ) );
		assertThatIllegalArgumentException().isThrownBy( () -> PredicateSupport.STANDARD.supports( null ) );
	}

	@Test
	void maintainedDialectsPreserveEveryPredicateDimension() {
		assertProfile(
				new H2Dialect().getPredicateSupport(),
				"ilike",
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new PostgreSQLDialect().getPredicateSupport(),
				"ilike",
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new CockroachDialect().getPredicateSupport(),
				"ilike",
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new DB2Dialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new MySQLDialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new HSQLDialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SQLServerDialect( DatabaseVersion.make( 15 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SQLServerDialect( DatabaseVersion.make( 16 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SybaseASEDialect( DatabaseVersion.make( 16, 2 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SybaseASEDialect( DatabaseVersion.make( 16, 3 ) ).getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
		assertProfile(
				new SpannerPostgreSQLDialect().getPredicateSupport(),
				null,
				PredicateSupport.Capability.DISTINCT_FROM,
				PredicateSupport.Capability.TRUTHNESS,
				PredicateSupport.Capability.EXPRESSION_PLACEMENT
		);
	}

	@Test
	void mixedProviderProfileSelectsNativeRenderingAndWrappedAggregateEmulation() {
		assertThat( renderLike( MIXED, true ) ).isEqualTo( "lhs not ilike rhs escape esc" );
		assertThat( renderComparison( MIXED, ComparisonOperator.DISTINCT_FROM ) )
				.isEqualTo( "lhs is distinct from rhs" );
		assertThat( renderComparison( MIXED, ComparisonOperator.NOT_DISTINCT_FROM ) )
				.isEqualTo( "lhs is not distinct from rhs" );
		assertThat( renderTruthness( MIXED, true, true ) ).isEqualTo( "lhs is not true" );
		assertThat( renderEvery( MIXED ) )
				.isEqualTo( "case when (sum(case when predicate then 0 else 1 end)=0) then true else false end" );
	}

	@Test
	void standardProfileSelectsEveryEmulatedPredicatePath() {
		assertThat( renderLike( PredicateSupport.STANDARD, true ) )
				.isEqualTo( "lower(lhs) not like lower(rhs) escape esc" );
		assertThat( renderComparison( PredicateSupport.STANDARD, ComparisonOperator.DISTINCT_FROM ) )
				.isEqualTo( "case when lhs=rhs or lhs is null and rhs is null then 0 else 1 end=1" );
		assertThat( renderComparison( PredicateSupport.STANDARD, ComparisonOperator.NOT_DISTINCT_FROM ) )
				.isEqualTo( "case when lhs=rhs or lhs is null and rhs is null then 0 else 1 end=0" );
		assertThat( renderTruthness( PredicateSupport.STANDARD, false, true ) )
				.isEqualTo( "(case lhs when 0 then 0 when 1 then 1 else 1 end = 1)" );
		assertThat( renderEvery( PredicateSupport.STANDARD ) )
				.isEqualTo( "(sum(case when predicate then 0 else 1 end)=0)" );
	}

	@Test
	void scalarDistinctCapabilityRemainsARowValuePrerequisite() {
		assertThat( supportsRowDistinctness( predicateDialect( PredicateSupport.STANDARD ) ) )
				.isFalse();
		assertThat( supportsRowDistinctness( predicateDialect(
				PredicateSupport.builder().capability( PredicateSupport.Capability.DISTINCT_FROM, true ).build()
		) ) ).isTrue();
	}

	private static String renderLike(PredicateSupport support, boolean negated) {
		final TestingTranslator translator = translator( support );
		new LikePredicate(
				expression( "lhs" ),
				expression( "rhs" ),
				expression( "esc" ),
				negated,
				false,
				null
		).accept( translator );
		return translator.renderedSql();
	}

	private static String renderComparison(PredicateSupport support, ComparisonOperator operator) {
		final TestingTranslator translator = translator( support );
		new ComparisonPredicate( expression( "lhs" ), operator, expression( "rhs" ) ).accept( translator );
		return translator.renderedSql();
	}

	private static String renderTruthness(
			PredicateSupport support,
			boolean value,
			boolean negated) {
		final TestingTranslator translator = translator( support );
		new ThruthnessPredicate( expression( "lhs" ), value, negated, null ).accept( translator );
		return translator.renderedSql();
	}

	private static String renderEvery(PredicateSupport support) {
		final StringBuilder sql = new StringBuilder();
		final SqlAppender appender = new StringBuilderSqlAppender( sql );
		final SqlAstTranslator<?> walker = mock( SqlAstTranslator.class );
		final SqlAstNode predicate = mock( SqlAstNode.class );
		doAnswer( invocation -> {
			appender.appendSql( "predicate" );
			return null;
		} ).when( predicate ).accept( walker );
		doAnswer( invocation -> {
			final QueryLiteral<?> literal = invocation.getArgument( 0 );
			appender.appendSql( literal.getLiteralValue().toString() );
			return null;
		} ).when( walker ).visitQueryLiteral( any() );

		new EveryAnyEmulation(
				new TypeConfiguration(),
				true,
				support.supports( PredicateSupport.Capability.EXPRESSION_PLACEMENT )
		).render( appender, List.of( predicate ), null, walker );
		return sql.toString();
	}

	private static TestingTranslator translator(PredicateSupport support) {
		final Dialect dialect = predicateDialect( support );
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final SessionFactoryOptions sessionFactoryOptions = mock( SessionFactoryOptions.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		when( sessionFactory.getSessionFactoryOptions() ).thenReturn( sessionFactoryOptions );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingTranslator( new SqlAstTranslationRequest.Select(
				sessionFactory,
				new SelectStatement( new QuerySpec( true ) )
		) );
	}

	private static Dialect predicateDialect(PredicateSupport support) {
		return new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public PredicateSupport getPredicateSupport() {
				return support;
			}
		};
	}

	private static boolean supportsRowDistinctness(Dialect dialect) {
		return dialect.getRowValueSupport().supports( RowValueSupport.Feature.DISTINCTNESS_COMPARISON )
				&& dialect.getPredicateSupport().supports( PredicateSupport.Capability.DISTINCT_FROM );
	}

	private static Expression expression(String sql) {
		return new Expression() {
			@Override
			public void accept(SqlAstWalker sqlTreeWalker) {
				( (SqlAppender) sqlTreeWalker ).appendSql( sql );
			}

			@Override
			public org.hibernate.metamodel.mapping.JdbcMappingContainer getExpressionType() {
				return null;
			}
		};
	}

	private static void assertProfile(
			PredicateSupport profile,
			String operator,
			PredicateSupport.Capability... capabilities) {
		if ( operator == null ) {
			assertThat( profile.getCaseInsensitiveLikeOperator() ).isEmpty();
		}
		else {
			assertThat( profile.getCaseInsensitiveLikeOperator() ).contains( operator );
		}
		assertThat( profile.getCapabilities() ).containsExactlyInAnyOrder( capabilities );
	}

	private static class TestingTranslator extends AbstractSqlAstTranslator<JdbcSelect> {
		private TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}

		private String renderedSql() {
			return getSql();
		}
	}
}
