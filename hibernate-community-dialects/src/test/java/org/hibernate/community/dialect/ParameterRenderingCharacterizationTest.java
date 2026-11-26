package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.spi.query.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Characterizes how the Firebird and Altibase translators dispatch a JDBC
/// parameter under each requested rendering mode, so that moving their
/// customization from `visitParameter` to a rendering callback can be shown to
/// preserve it.
///
/// The cast is replaced by a marker which records the rendering mode in
/// effect when the cast is rendered and then renders its argument in that
/// mode. That holds the cast function constant and exposes exactly the thing
/// at risk: which path a parameter takes, how casts nest, and which mode the
/// nested rendering sees.
///
/// The expectations record what the translators do today, including casts
/// that nest twice: Firebird casts a function argument again when the
/// requested mode already asks for a cast, and Altibase casts again whenever
/// the nested parameter is rendered in the SELECT clause, which a wrapped
/// parameter always is. Whether those double casts are intended is not
/// decided here.
///
/// The inline modes are not covered: they need parameter bindings, which exist
/// only during a full translation.
class ParameterRenderingCharacterizationTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			DEFAULT             | cast[DEFAULT](?)
			NO_PLAIN_PARAMETER  | cast[NO_PLAIN_PARAMETER](cast[DEFAULT](?))
			NO_UNTYPED          | cast[NO_UNTYPED](cast[DEFAULT](?))
			WRAP_ALL_PARAMETERS | cast[WRAP_ALL_PARAMETERS]((select cast[DEFAULT](?) from rdb$database))
			""")
	void firebirdCastsAParameterRenderedAsAFunctionArgument(SqlAstNodeRenderingMode mode, String expected) {
		final var fixture = new Fixture( new FirebirdDialect() );
		final var translator = new RecordingFirebird<>( fixture.request() );
		final var function = mock( SelfRenderingExpression.class );
		doAnswer( invocation -> {
			invocation.<SqlAstTranslator<?>>getArgument( 1 ).render( fixture.parameter, mode );
			return null;
		} ).when( function ).renderToSql( any(), any(), any() );

		translator.visitSelfRenderingPredicate( new SelfRenderingPredicate( function ) );

		assertThat( translator.sql() ).isEqualTo( expected );
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			DEFAULT             | ?
			NO_PLAIN_PARAMETER  | cast[DEFAULT](?)
			NO_UNTYPED          | cast[DEFAULT](?)
			WRAP_ALL_PARAMETERS | (select cast[DEFAULT](?) from rdb$database)
			""")
	void firebirdRendersAParameterOutsideAFunctionAsTheBaseDoes(SqlAstNodeRenderingMode mode, String expected) {
		final var fixture = new Fixture( new FirebirdDialect() );
		final var translator = new RecordingFirebird<>( fixture.request() );

		translator.render( fixture.parameter, mode );

		assertThat( translator.sql() ).isEqualTo( expected );
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			DEFAULT             | cast[DEFAULT](?)
			NO_PLAIN_PARAMETER  | cast[DEFAULT](cast[DEFAULT](?))
			NO_UNTYPED          | cast[DEFAULT](cast[DEFAULT](?))
			WRAP_ALL_PARAMETERS | (select cast[DEFAULT](cast[DEFAULT](?)) from dual)
			""")
	void altibaseCastsAPlainParameterInTheSelectClause(SqlAstNodeRenderingMode mode, String expected) {
		final var fixture = new Fixture( new AltibaseDialect() );
		final var translator = new RecordingAltibase<>( fixture.request() );

		translator.withClause( Clause.SELECT, () -> translator.render( fixture.parameter, mode ) );

		assertThat( translator.sql() ).isEqualTo( expected );
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			DEFAULT             | ?
			NO_PLAIN_PARAMETER  | cast[DEFAULT](?)
			NO_UNTYPED          | cast[DEFAULT](?)
			WRAP_ALL_PARAMETERS | (select cast[DEFAULT](cast[DEFAULT](?)) from dual)
			""")
	void altibaseRendersAParameterOutsideTheSelectClauseAsTheBaseDoes(SqlAstNodeRenderingMode mode, String expected) {
		final var fixture = new Fixture( new AltibaseDialect() );
		final var translator = new RecordingAltibase<>( fixture.request() );

		translator.withClause( Clause.WHERE, () -> translator.render( fixture.parameter, mode ) );

		assertThat( translator.sql() ).isEqualTo( expected );
	}

	static final class Fixture {
		final SessionFactoryImplementor factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		final TypeConfiguration types = new TypeConfiguration();
		final JdbcParameter parameter =
				new JdbcParameterImpl( types.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ) );

		Fixture(Dialect dialect) {
			when( factory.getTypeConfiguration() ).thenReturn( types );
			when( factory.getJdbcServices().getDialect() ).thenReturn( dialect );
			when( factory.getJdbcServices().getParameterMarkerStrategy() )
					.thenReturn( ParameterMarkerStrategyStandard.INSTANCE );
		}

		SqlAstTranslationRequest.Select request() {
			return new SqlAstTranslationRequest.Select( factory, new SelectStatement( new QuerySpec( true ) ) );
		}
	}

	static final class RecordingFirebird<T extends JdbcOperation> extends FirebirdSqlAstTranslator<T> {
		RecordingFirebird(SqlAstTranslationRequest<? extends Statement, T> request) {
			super( request );
		}

		String sql() {
			return getSql();
		}

		@Override
		protected void renderCasted(Expression expression) {
			appendSql( "cast[" + getParameterRenderingMode() + "](" );
			expression.accept( this );
			appendSql( ')' );
		}
	}

	static final class RecordingAltibase<T extends JdbcOperation> extends AltibaseSqlAstTranslator<T> {
		RecordingAltibase(SqlAstTranslationRequest<? extends Statement, T> request) {
			super( request );
		}

		String sql() {
			return getSql();
		}

		@Override
		protected void renderCasted(Expression expression) {
			appendSql( "cast[" + getParameterRenderingMode() + "](" );
			expression.accept( this );
			appendSql( ')' );
		}
	}
}
