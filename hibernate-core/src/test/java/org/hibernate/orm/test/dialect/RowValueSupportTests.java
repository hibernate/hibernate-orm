/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.lang.reflect.Method;
import java.util.List;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.DISTINCTNESS_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.EQUALITY_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.IN_LIST;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.IN_SUBQUERY;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.ORDERING_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.QUANTIFIED_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.ROW_CONSTRUCTOR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable row-value-support provider contract, maintained Dialect
/// profiles, and comparison-emulation selection.
///
/// @author Steve Ebersole
public class RowValueSupportTests {
	@Test
	void constantsAndBuildersExposeOneImmutableFeatureSet() {
		assertProfile( RowValueSupport.NONE );
		assertProfile(
				RowValueSupport.STANDARD,
				EQUALITY_COMPARISON,
				ORDERING_COMPARISON,
				DISTINCTNESS_COMPARISON,
				IN_LIST,
				IN_SUBQUERY,
				QUANTIFIED_COMPARISON
		);

		final RowValueSupport copied = RowValueSupport.builder( RowValueSupport.STANDARD )
				.feature( ORDERING_COMPARISON, false )
				.feature( DISTINCTNESS_COMPARISON, false )
				.feature( EQUALITY_COMPARISON, false )
				.feature( IN_LIST, false )
				.feature( ROW_CONSTRUCTOR, true )
				.build();
		assertProfile( copied, ROW_CONSTRUCTOR, IN_SUBQUERY, QUANTIFIED_COMPARISON );
		assertProfile(
				RowValueSupport.STANDARD,
				EQUALITY_COMPARISON,
				ORDERING_COMPARISON,
				DISTINCTNESS_COMPARISON,
				IN_LIST,
				IN_SUBQUERY,
				QUANTIFIED_COMPARISON
		);
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> copied.getFeatures().add( IN_LIST ) );
	}

	@Test
	@SuppressWarnings("NullAway")
	void buildersRejectNullAndInvalidInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> RowValueSupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> RowValueSupport.builder().features( (RowValueSupport.Feature[]) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> RowValueSupport.builder().features( IN_LIST, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> RowValueSupport.builder().feature( null, true ) );
		assertThatIllegalArgumentException().isThrownBy( () -> RowValueSupport.STANDARD.supports( null ) );

		assertThatIllegalArgumentException().isThrownBy( () -> RowValueSupport.builder( RowValueSupport.NONE )
				.feature( ORDERING_COMPARISON, true )
				.build() );
		assertThatIllegalArgumentException().isThrownBy( () -> RowValueSupport.builder( RowValueSupport.NONE )
				.feature( DISTINCTNESS_COMPARISON, true )
				.build() );
	}

	@Test
	void maintainedDialectsPreserveEveryRowValueFeature() {
		assertProfile( new Dialect( DatabaseVersion.make( 1 ) ) {
		}.getRowValueSupport(), EQUALITY_COMPARISON, ORDERING_COMPARISON, DISTINCTNESS_COMPARISON,
				IN_LIST, IN_SUBQUERY, QUANTIFIED_COMPARISON );
		for ( Dialect dialect : List.of( new H2Dialect(), new PostgreSQLDialect() ) ) {
			assertProfile( dialect.getRowValueSupport(), ROW_CONSTRUCTOR, EQUALITY_COMPARISON,
					ORDERING_COMPARISON, DISTINCTNESS_COMPARISON, IN_LIST, IN_SUBQUERY,
					QUANTIFIED_COMPARISON );
		}
		assertProfile( new CockroachDialect().getRowValueSupport(), ROW_CONSTRUCTOR, EQUALITY_COMPARISON,
				ORDERING_COMPARISON, DISTINCTNESS_COMPARISON, IN_LIST, IN_SUBQUERY );
		assertProfile( new SpannerPostgreSQLDialect().getRowValueSupport(), IN_LIST );
		assertProfile( new DB2Dialect().getRowValueSupport(), IN_SUBQUERY );
		assertProfile( new HANADialect().getRowValueSupport(), EQUALITY_COMPARISON,
				DISTINCTNESS_COMPARISON, IN_LIST, IN_SUBQUERY );
		for ( Dialect dialect : List.of( new MySQLDialect(), new MariaDBDialect() ) ) {
			assertProfile( dialect.getRowValueSupport(), EQUALITY_COMPARISON,
					ORDERING_COMPARISON, DISTINCTNESS_COMPARISON, IN_LIST, IN_SUBQUERY );
		}
		assertProfile( new OracleDialect().getRowValueSupport(), IN_LIST, IN_SUBQUERY );
		for ( Dialect dialect : List.of(
				new HSQLDialect(),
				new SQLServerDialect(),
				new SpannerDialect(),
				new SybaseDialect() ) ) {
			assertProfile( dialect.getRowValueSupport() );
		}
	}

	@Test
	void comparisonFeaturesAndScalarDistinctnessSelectEmulationIndependently() throws Exception {
		assertThat( needsTupleComparisonEmulation(
				RowValueSupport.STANDARD,
				PredicateSupport.STANDARD,
				ComparisonOperator.EQUAL
		) ).isFalse();
		assertThat( needsTupleComparisonEmulation(
				RowValueSupport.NONE,
				PredicateSupport.STANDARD,
				ComparisonOperator.EQUAL
		) ).isTrue();

		final RowValueSupport equalityOnly = RowValueSupport.builder( RowValueSupport.NONE )
				.feature( EQUALITY_COMPARISON, true )
				.build();
		assertThat( needsTupleComparisonEmulation(
				equalityOnly,
				PredicateSupport.STANDARD,
				ComparisonOperator.LESS_THAN
		) ).isTrue();

		final PredicateSupport scalarDistinct = PredicateSupport.builder()
				.capability( PredicateSupport.Capability.DISTINCT_FROM, true )
				.build();
		assertThat( needsTupleComparisonEmulation(
				RowValueSupport.STANDARD,
				scalarDistinct,
				ComparisonOperator.DISTINCT_FROM
		) ).isFalse();
		assertThat( needsTupleComparisonEmulation(
				RowValueSupport.STANDARD,
				PredicateSupport.STANDARD,
				ComparisonOperator.DISTINCT_FROM
		) ).isTrue();
		assertThat( needsTupleComparisonEmulation(
				equalityOnly,
				scalarDistinct,
				ComparisonOperator.DISTINCT_FROM
		) ).isTrue();
		assertThat( needsTupleComparisonEmulation(
				equalityOnly,
				PredicateSupport.STANDARD,
				ComparisonOperator.DISTINCT_FROM
		) ).isTrue();
	}

	private static boolean needsTupleComparisonEmulation(
			RowValueSupport rowValueSupport,
			PredicateSupport predicateSupport,
			ComparisonOperator operator) throws Exception {
		final Method method = AbstractSqlAstTranslator.class
				.getDeclaredMethod( "needsTupleComparisonEmulation", ComparisonOperator.class );
		method.setAccessible( true );
		return (boolean) method.invoke( translator( rowValueSupport, predicateSupport ), operator );
	}

	private static TestingTranslator translator(
			RowValueSupport rowValueSupport,
			PredicateSupport predicateSupport) {
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public RowValueSupport getRowValueSupport() {
				return rowValueSupport;
			}

			@Override
			public PredicateSupport getPredicateSupport() {
				return predicateSupport;
			}
		};
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

	private static void assertProfile(RowValueSupport profile, RowValueSupport.Feature... features) {
		assertThat( profile.getFeatures() ).containsExactlyInAnyOrder( features );
	}

	private static class TestingTranslator extends AbstractSqlAstTranslator<JdbcSelect> {
		private TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}
	}
}
