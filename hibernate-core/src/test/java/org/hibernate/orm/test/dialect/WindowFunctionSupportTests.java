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
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.common.FrameExclusion;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.common.FrameMode;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.FRAME_EXCLUSION;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.GROUPS_FRAME;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.PARTITION_BY;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.RANGE_FRAME;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.ROWS_FRAME;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.WINDOW_FUNCTIONS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable window-function provider contract, maintained Dialect
/// profiles, and exact SQL AST validation for partitioning, frame units, and
/// frame exclusion.
///
/// @author Steve Ebersole
public class WindowFunctionSupportTests {
	@Test
	void constantsAndBuildersExposeIndependentImmutableFeatures() {
		assertFeatures( WindowFunctionSupport.NONE );
		assertFeatures( WindowFunctionSupport.STANDARD );

		final WindowFunctionSupport baseline = WindowFunctionSupport.builder()
				.features( WINDOW_FUNCTIONS )
				.build();
		assertFeatures( baseline, WINDOW_FUNCTIONS );

		final WindowFunctionSupport full = WindowFunctionSupport.builder()
				.features( WindowFunctionSupport.Feature.values() )
				.build();
		assertFeatures( full, WindowFunctionSupport.Feature.values() );

		final WindowFunctionSupport copied = WindowFunctionSupport.builder( full )
				.feature( GROUPS_FRAME, false )
				.feature( FRAME_EXCLUSION, false )
				.build();
		assertFeatures( copied, WINDOW_FUNCTIONS, PARTITION_BY, ROWS_FRAME, RANGE_FRAME );
		assertFeatures( full, WindowFunctionSupport.Feature.values() );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> full.getFeatures().remove( ROWS_FRAME ) );
	}

	@Test
	void buildersRejectEveryInvalidPrerequisiteCategory() {
		for ( WindowFunctionSupport.Feature refinement : List.of(
				PARTITION_BY,
				ROWS_FRAME,
				RANGE_FRAME,
				GROUPS_FRAME,
				FRAME_EXCLUSION ) ) {
			assertThatIllegalArgumentException()
					.as( refinement.name() )
					.isThrownBy( () -> WindowFunctionSupport.builder().features( refinement ).build() );
		}
		assertThatIllegalArgumentException()
				.isThrownBy( () -> WindowFunctionSupport.builder()
						.features( WINDOW_FUNCTIONS, FRAME_EXCLUSION )
						.build() );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> WindowFunctionSupport.builder()
						.features( WindowFunctionSupport.Feature.values() )
						.feature( WINDOW_FUNCTIONS, false )
						.build() );
	}

	@Test
	@SuppressWarnings("NullAway")
	void buildersAndQueriesRejectNullInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> WindowFunctionSupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> WindowFunctionSupport.builder()
						.features( (WindowFunctionSupport.Feature[]) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> WindowFunctionSupport.builder().features( WINDOW_FUNCTIONS, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> WindowFunctionSupport.builder().feature( null, true ) );
		assertThatIllegalArgumentException().isThrownBy( () -> WindowFunctionSupport.STANDARD.supports( null ) );
	}

	@Test
	void maintainedProfilesReportDocumentedGrammar() {
		assertFeatures( new Dialect( DatabaseVersion.make( 1 ) ) {
		}.getWindowFunctionSupport() );
		assertFeatures( new MySQLDialect( DatabaseVersion.make( 8, 0, 1 ) ).getWindowFunctionSupport() );
		assertRowsAndRange( new MySQLDialect( DatabaseVersion.make( 8, 0, 2 ) ) );
		assertRowsAndRange( new MariaDBDialect() );
		assertRowsAndRange( new SQLServerDialect() );
		assertRowsAndRange( new OracleDialect() );
		assertRowsAndRange( new DB2Dialect() );
		assertRowsAndRange( new HANADialect() );
		assertFeatures( new H2Dialect().getWindowFunctionSupport(), WindowFunctionSupport.Feature.values() );
		assertFeatures(
				new PostgreSQLDialect().getWindowFunctionSupport(),
				WindowFunctionSupport.Feature.values()
		);
		assertFeatures( new CockroachDialect().getWindowFunctionSupport(), WindowFunctionSupport.Feature.values() );
		assertFeatures( new SpannerPostgreSQLDialect().getWindowFunctionSupport() );
		assertFeatures( new SybaseASEDialect().getWindowFunctionSupport() );
	}

	@Test
	void focusedFilterAndOrdinalitySupplyPointsPreserveMaintainedValues() {
		final Dialect baseline = new Dialect( DatabaseVersion.make( 1 ) ) {
		};
		assertThat( baseline.supportsFilterClause() ).isFalse();
		assertThat( new H2Dialect().supportsFilterClause() ).isTrue();
		assertThat( new HSQLDialect().supportsFilterClause() ).isTrue();
		assertThat( new PostgreSQLDialect().supportsFilterClause() ).isTrue();
		assertThat( new SpannerPostgreSQLDialect().supportsFilterClause() ).isFalse();

		assertThat( baseline.getDefaultOrdinalityColumnName() ).isNull();
		assertThat( new H2Dialect().getDefaultOrdinalityColumnName() ).isEqualTo( "nord" );
		assertThat( new HSQLDialect().getDefaultOrdinalityColumnName() ).isEqualTo( "c2" );
		assertThat( new PostgreSQLDialect().getDefaultOrdinalityColumnName() ).isEqualTo( "ordinality" );
		assertThat( new CockroachDialect().getDefaultOrdinalityColumnName() ).isEqualTo( "ordinality" );
	}

	@Test
	void translatorValidatesTheExactRequestedFeature() {
		final TestingTranslator none = createTranslator( WindowFunctionSupport.NONE );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( none::renderDefaultWindow )
				.withMessageContaining( "Window functions" );

		final TestingTranslator baseline = createTranslator(
				WindowFunctionSupport.builder().features( WINDOW_FUNCTIONS ).build()
		);
		assertThatCode( baseline::renderDefaultWindow ).doesNotThrowAnyException();
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( baseline::renderPartitionedWindow )
				.withMessageContaining( "PARTITION BY" );
		assertUnsupportedFrame( baseline, FrameMode.ROWS );
		assertUnsupportedFrame( baseline, FrameMode.RANGE );
		assertUnsupportedFrame( baseline, FrameMode.GROUPS );

		final TestingTranslator rows = createTranslator(
				WindowFunctionSupport.builder().features( WINDOW_FUNCTIONS, PARTITION_BY, ROWS_FRAME ).build()
		);
		assertThatCode( rows::renderPartitionedWindow ).doesNotThrowAnyException();
		assertThatCode( () -> rows.renderFrame( FrameMode.ROWS, FrameExclusion.NO_OTHERS ) )
				.doesNotThrowAnyException();
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> rows.renderFrame( FrameMode.ROWS, FrameExclusion.CURRENT_ROW ) )
				.withMessageContaining( "exclusion" );

		final TestingTranslator full = createTranslator(
				WindowFunctionSupport.builder().features( WindowFunctionSupport.Feature.values() ).build()
		);
		for ( FrameMode mode : FrameMode.values() ) {
			assertThatCode( () -> full.renderFrame( mode, FrameExclusion.CURRENT_ROW ) )
					.as( mode.name() )
					.doesNotThrowAnyException();
		}
	}

	private static void assertUnsupportedFrame(TestingTranslator translator, FrameMode mode) {
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> translator.renderFrame( mode, FrameExclusion.NO_OTHERS ) )
				.withMessageContaining( mode.name() );
	}

	private static TestingTranslator createTranslator(WindowFunctionSupport support) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		final QueryEngine queryEngine = mock( QueryEngine.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		when( sessionFactory.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		when( sessionFactory.getQueryEngine() ).thenReturn( queryEngine );
		when( queryEngine.getSqmFunctionRegistry() ).thenReturn( mock( SqmFunctionRegistry.class ) );
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public WindowFunctionSupport getWindowFunctionSupport() {
				return support;
			}
		};
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingTranslator( new SqlAstTranslationRequest.Select(
				sessionFactory,
				new SelectStatement( new QuerySpec( true ) )
		) );
	}

	private static void assertRowsAndRange(Dialect dialect) {
		assertFeatures(
				dialect.getWindowFunctionSupport(),
				WINDOW_FUNCTIONS,
				PARTITION_BY,
				ROWS_FRAME,
				RANGE_FRAME
		);
	}

	private static void assertFeatures(
			WindowFunctionSupport support,
			WindowFunctionSupport.Feature... features) {
		assertThat( support.getFeatures() ).containsExactlyInAnyOrder( features );
	}

	private static class TestingTranslator extends StandardSqlAstTranslator<JdbcSelect> {
		private TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}

		private void renderDefaultWindow() {
			visitOverClause( emptyList(), emptyList() );
		}

		private void renderPartitionedWindow() {
			final SqlSelection selection = mock( SqlSelection.class );
			when( selection.getExpression() ).thenReturn( mock( Expression.class ) );
			visitOverClause( List.of( new SqlSelectionExpression( selection ) ), emptyList() );
		}

		private void renderFrame(FrameMode mode, FrameExclusion exclusion) {
			visitOverClause(
					emptyList(),
					emptyList(),
					mode,
					FrameKind.UNBOUNDED_PRECEDING,
					null,
					FrameKind.UNBOUNDED_FOLLOWING,
					null,
					exclusion,
					false
			);
		}
	}
}
