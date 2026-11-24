package org.hibernate.community.dialect;

import org.hibernate.dialect.function.CastFunction;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static java.sql.Types.INTEGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies typed results for CASE expressions without WHEN branches on Informix.
///
/// @author Steve Ebersole
class InformixCaseExpressionTest {
	private final TypeConfiguration types = new TypeConfiguration();
	private final BasicType<Integer> integerType = types.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );

	@Test
	void emptySearchedCaseRendersTypedNull() {
		assertThat( render( new CaseSearchedExpression( integerType ) ) ).isEqualTo( "cast(null as integer)" );
	}

	@Test
	void emptySimpleCaseRendersTypedNull() {
		assertThat( render( new CaseSimpleExpression( integerType, new QueryLiteral<>( 1, integerType ) ) ) )
				.isEqualTo( "cast(null as integer)" );
	}

	@Test
	void emptyCasesPreserveOtherwise() {
		final var searched = new CaseSearchedExpression( integerType );
		searched.otherwise( new QueryLiteral<>( 2, integerType ) );
		assertThat( render( searched ) ).isEqualTo( "2" );
		final var simple = new CaseSimpleExpression( integerType, new QueryLiteral<>( 1, integerType ) );
		simple.otherwise( new QueryLiteral<>( 3, integerType ) );
		assertThat( render( simple ) ).isEqualTo( "3" );
	}

	private String render(Expression expression) {
		final var dialect = new InformixDialect();
		types.getDdlTypeRegistry().addDescriptor( new DdlTypeImpl( INTEGER, "integer", dialect ) );
		final var functions = new SqmFunctionRegistry();
		functions.register( "cast", new CastFunction( dialect, dialect.getPreferredSqlTypeCodeForBoolean() ) );
		final var factory = mock( SessionFactoryImplementor.class );
		final var jdbcServices = mock( JdbcServices.class );
		final var queryEngine = mock( QueryEngine.class );
		when( factory.getJdbcServices() ).thenReturn( jdbcServices );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		when( factory.getTypeConfiguration() ).thenReturn( types );
		when( factory.getQueryEngine() ).thenReturn( queryEngine );
		when( queryEngine.getSqmFunctionRegistry() ).thenReturn( functions );
		return new TestingTranslator( new SqlAstTranslationRequest.Select(
				factory, new SelectStatement( new QuerySpec( true ) ) ) ).renderExpression( expression );
	}

	private static class TestingTranslator extends InformixSqlAstTranslator<JdbcSelect> {
		TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}

		String renderExpression(Expression expression) {
			renderSelectExpression( expression );
			return getSql();
		}
	}
}
