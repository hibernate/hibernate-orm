/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import jakarta.persistence.criteria.Nulls;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.SortDirection;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.sql.ast.spi.NullOrderingSupport.Capability.NULLS_FIRST_LAST;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable null-ordering provider contract, maintained Dialect
/// profiles, and native, elided, and emulated SQL AST ordering.
///
/// @author Steve Ebersole
public class NullOrderingSupportTests {
	@Test
	void standardAndBuildersExposeIndependentImmutableState() {
		assertProfile( NullOrderingSupport.STANDARD, NullOrdering.GREATEST, true );

		final NullOrderingSupport lastWithoutSyntax = NullOrderingSupport.builder()
				.defaultOrdering( NullOrdering.LAST )
				.capability( NULLS_FIRST_LAST, false )
				.build();
		assertProfile( lastWithoutSyntax, NullOrdering.LAST, false );

		final NullOrderingSupport copied = NullOrderingSupport.builder( lastWithoutSyntax )
				.defaultOrdering( NullOrdering.FIRST )
				.capabilities( NULLS_FIRST_LAST )
				.build();
		assertProfile( copied, NullOrdering.FIRST, true );
		assertProfile( lastWithoutSyntax, NullOrdering.LAST, false );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> copied.getCapabilities().remove( NULLS_FIRST_LAST ) );
	}

	@Test
	@SuppressWarnings("NullAway")
	void buildersAndQueriesRejectNullInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> NullOrderingSupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> NullOrderingSupport.builder().defaultOrdering( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> NullOrderingSupport.builder().capabilities( (NullOrderingSupport.Capability[]) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> NullOrderingSupport.builder().capabilities( NULLS_FIRST_LAST, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> NullOrderingSupport.builder().capability( null, true ) );
		assertThatIllegalArgumentException().isThrownBy( () -> NullOrderingSupport.STANDARD.supports( null ) );
	}

	@Test
	void maintainedCoreProfilesPreserveProviderValues() {
		assertProfile( new Dialect( DatabaseVersion.make( 1 ) ) {
		}, NullOrdering.GREATEST, true );
		assertProfile( new H2Dialect(), NullOrdering.SMALLEST, true );
		assertProfile( new HANADialect(), NullOrdering.SMALLEST, true );
		assertProfile( new HSQLDialect(), NullOrdering.FIRST, true );
		assertProfile( new CockroachDialect(), NullOrdering.SMALLEST, false );
		assertProfile( new DB2Dialect(), NullOrdering.GREATEST, false );
		assertProfile( new MySQLDialect(), NullOrdering.SMALLEST, false );
		assertProfile( new SQLServerDialect(), NullOrdering.SMALLEST, false );
		assertProfile( new SpannerDialect(), NullOrdering.SMALLEST, false );
		assertProfile( new SybaseDialect(), NullOrdering.SMALLEST, false );
	}

	@Test
	void everyDefaultOrderingElidesBothAscendingAndDescendingDefaults() {
		assertRendered( NullOrdering.SMALLEST, true, SortDirection.ASCENDING, Nulls.FIRST, "x" );
		assertRendered( NullOrdering.SMALLEST, true, SortDirection.DESCENDING, Nulls.LAST, "x desc" );
		assertRendered( NullOrdering.GREATEST, true, SortDirection.ASCENDING, Nulls.LAST, "x" );
		assertRendered( NullOrdering.GREATEST, true, SortDirection.DESCENDING, Nulls.FIRST, "x desc" );
		assertRendered( NullOrdering.FIRST, true, SortDirection.ASCENDING, Nulls.FIRST, "x" );
		assertRendered( NullOrdering.FIRST, true, SortDirection.DESCENDING, Nulls.FIRST, "x desc" );
		assertRendered( NullOrdering.LAST, true, SortDirection.ASCENDING, Nulls.LAST, "x" );
		assertRendered( NullOrdering.LAST, true, SortDirection.DESCENDING, Nulls.LAST, "x desc" );
	}

	@Test
	void explicitPrecedenceUsesNativeSyntaxOrCaseEmulation() {
		assertRendered(
				NullOrdering.GREATEST,
				true,
				SortDirection.ASCENDING,
				Nulls.FIRST,
				"x asc nulls first"
		);
		assertRendered(
				NullOrdering.GREATEST,
				true,
				SortDirection.DESCENDING,
				Nulls.LAST,
				"x desc nulls last"
		);
		assertRendered(
				NullOrdering.GREATEST,
				false,
				SortDirection.ASCENDING,
				Nulls.FIRST,
				"case when (x) is null then 0 else 1 end,x"
		);
		assertRendered(
				NullOrdering.GREATEST,
				false,
				SortDirection.DESCENDING,
				Nulls.LAST,
				"case when (x) is null then 1 else 0 end,x desc"
		);
	}

	@Test
	void ordinalSelectItemReferenceUsesPositionOrUnderlyingExpression() {
		assertOrdinalRendered( true, "3" );
		assertOrdinalRendered( false, "x" );
	}

	private static void assertProfile(Dialect dialect, NullOrdering ordering, boolean explicitSyntax) {
		assertProfile( dialect.getNullOrderingSupport(), ordering, explicitSyntax );
	}

	private static void assertProfile(
			NullOrderingSupport support,
			NullOrdering ordering,
			boolean explicitSyntax) {
		assertThat( support.getDefaultOrdering() ).isEqualTo( ordering );
		assertThat( support.supports( NULLS_FIRST_LAST ) ).isEqualTo( explicitSyntax );
		assertThat( support.getCapabilities() ).containsExactlyInAnyOrder(
				explicitSyntax ? new NullOrderingSupport.Capability[] { NULLS_FIRST_LAST }
						: new NullOrderingSupport.Capability[0]
		);
	}

	private static void assertRendered(
			NullOrdering ordering,
			boolean explicitSyntax,
			SortDirection direction,
			Nulls nulls,
			String expectedSql) {
		final NullOrderingSupport support = NullOrderingSupport.builder()
				.defaultOrdering( ordering )
				.capability( NULLS_FIRST_LAST, explicitSyntax )
				.build();
		final TestingTranslator translator = createTranslator( support );
		final Expression expression = mock( Expression.class );
		doAnswer( invocation -> {
			translator.appendSql( "x" );
			return null;
		} ).when( expression ).accept( translator );
		translator.renderSort( expression, direction, nulls );
		assertThat( translator.renderedSql() ).isEqualTo( expectedSql );
	}

	private static void assertOrdinalRendered(boolean ordinalSupported, String expectedSql) {
		final TestingTranslator translator = createTranslator( NullOrderingSupport.STANDARD, ordinalSupported );
		final Expression expression = mock( Expression.class );
		doAnswer( invocation -> {
			translator.appendSql( "x" );
			return null;
		} ).when( expression ).accept( translator );
		final SqlSelection selection = mock( SqlSelection.class );
		when( selection.getJdbcResultSetIndex() ).thenReturn( 3 );
		when( selection.getExpression() ).thenReturn( expression );
		translator.renderSelectionExpression( new SqlSelectionExpression( selection ) );
		assertThat( translator.renderedSql() ).isEqualTo( expectedSql );
	}

	private static TestingTranslator createTranslator(NullOrderingSupport support) {
		return createTranslator( support, true );
	}

	private static TestingTranslator createTranslator(NullOrderingSupport support, boolean ordinalSupported) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		when( sessionFactory.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public NullOrderingSupport getNullOrderingSupport() {
				return support;
			}

			@Override
			public boolean supportsOrdinalSelectItemReference() {
				return ordinalSupported;
			}
		};
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingTranslator( new SqlAstTranslationRequest.Select(
				sessionFactory,
				new SelectStatement( new QuerySpec( true ) )
		) );
	}

	private static class TestingTranslator extends StandardSqlAstTranslator<JdbcSelect> {
		private TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}

		private void renderSort(Expression expression, SortDirection direction, Nulls nulls) {
			visitSortSpecification( expression, direction, nulls, false );
		}

		private void renderSelectionExpression(SqlSelectionExpression expression) {
			visitSqlSelectionExpression( expression );
		}

		@Override
		protected Expression resolveAliasedExpression(Expression expression) {
			return expression;
		}

		private String renderedSql() {
			return getSql();
		}
	}
}
