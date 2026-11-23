package org.hibernate.orm.test.sql.ast;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.PostgreSQLSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.common.FrameExclusion;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.common.FrameMode;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Step 2 of the SQL AST translator convention work: the visitors which own a
/// scope are final and customization happens in a rendering callback entered
/// within that scope.
class SqlAstTranslatorScopeOwnerTests {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Surface

	@Test
	void scopeOwningVisitorsAreFinal() throws NoSuchMethodException {
		assertFinal( "visitParameter", JdbcParameter.class );
		assertFinal( "renderBoundParameter", JdbcParameter.class );
		assertFinal( "visitOptionalTableUpdate", OptionalTableUpdate.class );
		assertFinal( "visitOverClause", List.class, List.class );
		assertFinal( "visitOverClause", List.class, List.class, FrameMode.class, FrameKind.class,
				Expression.class, FrameKind.class, Expression.class, FrameExclusion.class, boolean.class );
	}

	/// Every walker callback the base implements is final: a subclass customizes
	/// rendering through a callback, never by replacing a traversal entry point.
	@Test
	void everyWalkerCallbackIsFinal() {
		final List<String> overridable = Stream.of( SqlAstWalker.class.getMethods() )
				.map( declared -> {
					try {
						return AbstractSqlAstTranslator.class.getMethod( declared.getName(), declared.getParameterTypes() );
					}
					catch (NoSuchMethodException e) {
						throw new AssertionError( e );
					}
				} )
				.filter( implemented -> implemented.getDeclaringClass() == AbstractSqlAstTranslator.class )
				.filter( implemented -> !Modifier.isFinal( implemented.getModifiers() ) )
				.map( Method::toString )
				.toList();

		assertThat( overridable ).isEmpty();
		assertThat( Stream.of( SqlAstWalker.class.getMethods() )
				.filter( declared -> !Modifier.isStatic( declared.getModifiers() ) ) )
				.as( "walker callbacks checked" ).hasSizeGreaterThan( 70 );
	}

	/// `visit` is reserved for walker callbacks and final scope owners.
	@Test
	void noOverridableProtectedMethodIsNamedVisit() {
		final List<String> offenders = Stream.of( AbstractSqlAstTranslator.class.getDeclaredMethods() )
				.filter( method -> method.getName().startsWith( "visit" ) )
				.filter( method -> Modifier.isProtected( method.getModifiers() ) )
				.filter( method -> !Modifier.isFinal( method.getModifiers() ) )
				.map( Method::getName )
				.toList();

		assertThat( offenders ).isEmpty();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameters

	@ParameterizedTest
	@EnumSource(value = SqlAstNodeRenderingMode.class,
			names = { "DEFAULT", "NO_PLAIN_PARAMETER", "NO_UNTYPED", "WRAP_ALL_PARAMETERS" })
	void parameterCallbackSeesTheRequestedModeWhileTheDefaultModeIsInEffect(SqlAstNodeRenderingMode mode) {
		final var translator = translator();
		translator.recordInsteadOfRendering = true;

		translator.render( parameter(), mode );

		assertThat( translator.requestedModes ).containsExactly( mode );
		assertThat( translator.effectiveModes ).containsExactly( SqlAstNodeRenderingMode.DEFAULT );
		assertThat( translator.currentMode() ).isEqualTo( SqlAstNodeRenderingMode.DEFAULT );
	}

	@Test
	void parameterModeIsRestoredWhenRenderingFails() {
		final var translator = translator();
		translator.failWhileRenderingParameter = true;
		final var parameter = parameter();

		translator.withMode( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER, () -> {
			assertThatExceptionOfType( IllegalStateException.class )
					.isThrownBy( () -> translator.visitParameter( parameter ) );
			assertThat( translator.currentMode() ).isEqualTo( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		} );
	}

	@Test
	void aMarkerRegistersItsBinderExactlyOnce() {
		final var translator = translator();

		translator.render( parameter(), SqlAstNodeRenderingMode.DEFAULT );

		assertThat( translator.sql() ).isEqualTo( "?" );
		assertThat( translator.binderCount() ).isOne();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Optional update

	@Test
	void optionalUpdateCallbackRunsWithinTheUpdateClause() {
		final var translator = translator();

		translator.visitOptionalTableUpdate( mock( OptionalTableUpdate.class ) );

		assertThat( translator.clausesSeen ).containsExactly( Clause.UPDATE );
		assertThat( translator.getCurrentClause() ).isNull();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Window clause

	@Test
	void overClauseCallbackRunsWithinTheOverClause() {
		final var translator = translator();

		translator.overClause();

		assertThat( translator.clausesSeen ).containsExactly( Clause.OVER );
		assertThat( translator.sql() ).isEqualTo( " over()" );
		assertThat( translator.getCurrentClause() ).isNull();
	}

	@Test
	void overClauseIsLeftWhenRenderingFails() {
		final var translator = translator();
		translator.failWhileRenderingOverClause = true;

		assertThatExceptionOfType( IllegalStateException.class ).isThrownBy( translator::overClause );

		assertThat( translator.clausesSeen ).containsExactly( Clause.OVER );
		assertThat( translator.getCurrentClause() ).isNull();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Support

	private static void assertFinal(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
		final Method method = AbstractSqlAstTranslator.class.getDeclaredMethod( name, parameterTypes );
		assertThat( Modifier.isFinal( method.getModifiers() ) ).as( method.toString() ).isTrue();
	}

	private static final TypeConfiguration TYPES = new TypeConfiguration();

	private static JdbcParameter parameter() {
		return new JdbcParameterImpl( TYPES.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ) );
	}

	private static RecordingTranslator<JdbcOperation> translator() {
		final var factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		when( factory.getTypeConfiguration() ).thenReturn( TYPES );
		when( factory.getJdbcServices().getDialect() ).thenReturn( new PostgreSQLDialect() );
		when( factory.getJdbcServices().getParameterMarkerStrategy() )
				.thenReturn( ParameterMarkerStrategyStandard.INSTANCE );
		return new RecordingTranslator<>(
				new SqlAstTranslationRequest.Select( factory, new SelectStatement( new QuerySpec( true ) ) ) );
	}

	static final class RecordingTranslator<T extends JdbcOperation> extends PostgreSQLSqlAstTranslator<T> {
		final List<SqlAstNodeRenderingMode> requestedModes = new ArrayList<>();
		final List<SqlAstNodeRenderingMode> effectiveModes = new ArrayList<>();
		final List<Clause> clausesSeen = new ArrayList<>();
		boolean recordInsteadOfRendering;
		boolean failWhileRenderingParameter;
		boolean failWhileRenderingOverClause;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		RecordingTranslator(SqlAstTranslationRequest request) {
			super( request );
		}

		String sql() {
			return getSql();
		}

		int binderCount() {
			return getParameterBinders().size();
		}

		SqlAstNodeRenderingMode currentMode() {
			return getParameterRenderingMode();
		}

		void withMode(SqlAstNodeRenderingMode mode, Runnable action) {
			withParameterRenderingMode( mode, action );
		}

		void overClause() {
			visitOverClause( List.<Expression>of(), List.<SortSpecification>of() );
		}

		@Override
		protected void renderParameter(JdbcParameter jdbcParameter, SqlAstNodeRenderingMode requestedMode) {
			if ( failWhileRenderingParameter ) {
				throw new IllegalStateException( "Expected test failure" );
			}
			if ( recordInsteadOfRendering ) {
				requestedModes.add( requestedMode );
				effectiveModes.add( getParameterRenderingMode() );
				return;
			}
			super.renderParameter( jdbcParameter, requestedMode );
		}

		@Override
		protected void renderOptionalTableUpdate(OptionalTableUpdate tableUpdate) {
			clausesSeen.add( getCurrentClause() );
		}

		@Override
		protected void renderOverClause(
				List<Expression> partitionExpressions,
				List<SortSpecification> sortSpecifications,
				FrameMode mode,
				FrameKind startKind,
				Expression startExpression,
				FrameKind endKind,
				Expression endExpression,
				FrameExclusion exclusion,
				boolean orderedSetAggregate) {
			clausesSeen.add( getCurrentClause() );
			if ( failWhileRenderingOverClause ) {
				throw new IllegalStateException( "Expected test failure" );
			}
			super.renderOverClause( partitionExpressions, sortSpecifications, mode, startKind, startExpression,
					endKind, endExpression, exclusion, orderedSetAggregate );
		}
	}
}
