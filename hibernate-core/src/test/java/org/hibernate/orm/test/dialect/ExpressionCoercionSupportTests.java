/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.lang.reflect.Method;
import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.function.ExtractFunction;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmExtractUnit;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.FunctionExpression;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.function.spi.ExpressionCoercionSupport.Requirement.CAST_INTEGER_DIVISION_TO_FLOAT;
import static org.hibernate.dialect.function.spi.ExpressionCoercionSupport.Requirement.CAST_NON_STRING_CONCATENATION_ARGUMENTS;
import static org.hibernate.query.sqm.CastType.INTEGER;
import static org.hibernate.query.sqm.CastType.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests the immutable expression-coercion provider contract, maintained
/// Dialect profiles, and both focused rendering decisions.
///
/// @author Steve Ebersole
public class ExpressionCoercionSupportTests {
	@Test
	void constantsAndBuildersExposeIndependentImmutableRequirements() {
		assertRequirements( ExpressionCoercionSupport.NONE );
		assertRequirements( ExpressionCoercionSupport.STANDARD );

		final ExpressionCoercionSupport concatenation = ExpressionCoercionSupport.builder()
				.requirements( CAST_NON_STRING_CONCATENATION_ARGUMENTS )
				.build();
		assertRequirements( concatenation, CAST_NON_STRING_CONCATENATION_ARGUMENTS );

		final ExpressionCoercionSupport division = ExpressionCoercionSupport.builder()
				.requirements( CAST_INTEGER_DIVISION_TO_FLOAT )
				.build();
		assertRequirements( division, CAST_INTEGER_DIVISION_TO_FLOAT );

		final ExpressionCoercionSupport both = ExpressionCoercionSupport.builder()
				.requirements( CAST_NON_STRING_CONCATENATION_ARGUMENTS, CAST_INTEGER_DIVISION_TO_FLOAT )
				.build();
		assertRequirements( both, CAST_NON_STRING_CONCATENATION_ARGUMENTS, CAST_INTEGER_DIVISION_TO_FLOAT );

		final ExpressionCoercionSupport copied = ExpressionCoercionSupport.builder( both )
				.requirement( CAST_NON_STRING_CONCATENATION_ARGUMENTS, false )
				.build();
		assertRequirements( copied, CAST_INTEGER_DIVISION_TO_FLOAT );
		assertRequirements( both, CAST_NON_STRING_CONCATENATION_ARGUMENTS, CAST_INTEGER_DIVISION_TO_FLOAT );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> both.getRequirements().remove( CAST_INTEGER_DIVISION_TO_FLOAT ) );
	}

	@Test
	@SuppressWarnings("NullAway")
	void buildersAndQueriesRejectNullInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> ExpressionCoercionSupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> ExpressionCoercionSupport.builder()
						.requirements( (ExpressionCoercionSupport.Requirement[]) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> ExpressionCoercionSupport.builder()
						.requirements( CAST_INTEGER_DIVISION_TO_FLOAT, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> ExpressionCoercionSupport.builder().requirement( null, true ) );
		assertThatIllegalArgumentException().isThrownBy( () -> ExpressionCoercionSupport.STANDARD.requires( null ) );
	}

	@Test
	void maintainedProfilesPreserveEveryFamilyValue() {
		assertRequirements( new Dialect( DatabaseVersion.make( 1 ) ) {
		}.getExpressionCoercionSupport() );
		for ( Dialect dialect : List.of(
				new DB2Dialect(),
				new DB2iDialect(),
				new DB2zDialect(),
				new SQLServerDialect(),
				new SybaseDialect(),
				new SybaseASEDialect() ) ) {
			assertRequirements( dialect.getExpressionCoercionSupport(), CAST_NON_STRING_CONCATENATION_ARGUMENTS );
		}
		assertRequirements( new HSQLDialect().getExpressionCoercionSupport(), CAST_INTEGER_DIVISION_TO_FLOAT );
	}

	@Test
	void stringConcatenationIsUnchangedAndNonStringRenderingUsesTheExactRequirement() throws Exception {
		final TestingTranslator direct = createTranslator( ExpressionCoercionSupport.STANDARD );
		final Expression stringExpression = expression( STRING );
		assertThat( direct.wrapEqualityPreservingConcatenationArgument( stringExpression ) )
				.isSameAs( stringExpression );
		assertThat( (FunctionExpression) direct.wrapEqualityPreservingConcatenationArgument( expression( INTEGER ) ) )
				.extracting( FunctionExpression::getFunctionName )
				.isEqualTo( "concat" );

		final TestingTranslator casting = createTranslator(
				ExpressionCoercionSupport.builder().requirements( CAST_NON_STRING_CONCATENATION_ARGUMENTS ).build()
		);
		assertThat( (FunctionExpression) casting.wrapEqualityPreservingConcatenationArgument( expression( INTEGER ) ) )
				.extracting( FunctionExpression::getFunctionName )
				.isEqualTo( "cast" );
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void weekExtractionSelectsDirectOrCastedIntegerDivisionIndependently() {
		final TypeConfiguration typeConfiguration = mock( TypeConfiguration.class );
		final BasicType<Integer> integerType = mock( BasicType.class );
		final BasicType<Float> floatType = mock( BasicType.class );
		final JavaType<Integer> integerJavaType = mock( JavaType.class );
		final JavaType<Float> floatJavaType = mock( JavaType.class );
		when( integerType.getExpressibleJavaType() ).thenReturn( integerJavaType );
		when( floatType.getExpressibleJavaType() ).thenReturn( floatJavaType );
		when( integerJavaType.isInstance( any() ) ).thenReturn( true );
		when( floatJavaType.isInstance( any() ) ).thenReturn( true );
		when( typeConfiguration.getBasicTypeForJavaType( Integer.class ) ).thenReturn( integerType );
		when( typeConfiguration.getBasicTypeForJavaType( Float.class ) ).thenReturn( floatType );
		final NodeBuilder nodeBuilder = mock( NodeBuilder.class );
		final SqmExpression<?> temporalExpression = mock( SqmExpression.class );
		when( temporalExpression.nodeBuilder() ).thenReturn( nodeBuilder );
		final SqmExtractUnit<Integer> week = new SqmExtractUnit<>(
				TemporalUnit.WEEK_OF_YEAR,
				integerType,
				nodeBuilder
		);

		assertIntegerDivisionPath(
				ExpressionCoercionSupport.STANDARD,
				typeConfiguration,
				week,
				temporalExpression,
				false
		);
		assertIntegerDivisionPath(
				ExpressionCoercionSupport.builder().requirements( CAST_INTEGER_DIVISION_TO_FLOAT ).build(),
				typeConfiguration,
				week,
				temporalExpression,
				true
		);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void assertIntegerDivisionPath(
			ExpressionCoercionSupport support,
			TypeConfiguration typeConfiguration,
			SqmExtractUnit<Integer> week,
			SqmExpression<?> temporalExpression,
			boolean castExpected) {
		final QueryEngine queryEngine = mock( QueryEngine.class );
		final SqmFunctionRegistry functionRegistry = mock( SqmFunctionRegistry.class );
		final SqmFunctionDescriptor extract = mock( SqmFunctionDescriptor.class );
		final SqmFunctionDescriptor cast = mock( SqmFunctionDescriptor.class );
		final SqmFunctionDescriptor ceiling = mock( SqmFunctionDescriptor.class );
		when( queryEngine.getTypeConfiguration() ).thenReturn( typeConfiguration );
		when( queryEngine.getSqmFunctionRegistry() ).thenReturn( functionRegistry );
		when( functionRegistry.findFunctionDescriptor( "extract" ) ).thenReturn( extract );
		when( functionRegistry.findFunctionDescriptor( "cast" ) ).thenReturn( cast );
		when( functionRegistry.findFunctionDescriptor( "ceiling" ) ).thenReturn( ceiling );
		when( extract.generateSqmExpression( anyList(), any(), any() ) )
				.thenReturn( mock( SelfRenderingSqmFunction.class ) );
		when( cast.generateSqmExpression( anyList(), any(), any() ) )
				.thenReturn( mock( SelfRenderingSqmFunction.class ) );
		when( ceiling.generateSqmExpression( anyList(), any(), any() ) )
				.thenReturn( mock( SelfRenderingSqmFunction.class ) );

		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public ExpressionCoercionSupport getExpressionCoercionSupport() {
				return support;
			}
		};
		new ExtractFunction( dialect, typeConfiguration )
				.generateSqmExpression( List.of( week, temporalExpression ), null, queryEngine );
		if ( castExpected ) {
			verify( cast ).generateSqmExpression( anyList(), any(), any() );
		}
		else {
			verify( cast, never() ).generateSqmExpression( anyList(), any(), any() );
		}
	}

	private static TestingTranslator createTranslator(ExpressionCoercionSupport support) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		final QueryEngine queryEngine = mock( QueryEngine.class );
		final SqmFunctionRegistry functionRegistry = mock( SqmFunctionRegistry.class );
		final SqmFunctionDescriptor renderer = mock(
				SqmFunctionDescriptor.class,
				org.mockito.Mockito.withSettings().extraInterfaces( org.hibernate.query.sqm.function.FunctionRenderer.class )
		);
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		when( sessionFactory.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		when( sessionFactory.getQueryEngine() ).thenReturn( queryEngine );
		when( queryEngine.getSqmFunctionRegistry() ).thenReturn( functionRegistry );
		when( functionRegistry.findFunctionDescriptor( "cast" ) ).thenReturn( renderer );
		when( functionRegistry.findFunctionDescriptor( "concat" ) ).thenReturn( renderer );
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public ExpressionCoercionSupport getExpressionCoercionSupport() {
				return support;
			}
		};
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingTranslator( new SqlAstTranslationRequest.Select(
				sessionFactory,
				new SelectStatement( new QuerySpec( true ) )
		) );
	}

	private static Expression expression(org.hibernate.query.sqm.CastType castType) {
		final Expression expression = mock( Expression.class );
		final JdbcMappingContainer expressionType = mock( JdbcMappingContainer.class );
		final JdbcMapping jdbcMapping = mock( JdbcMapping.class );
		when( expression.getExpressionType() ).thenReturn( expressionType );
		when( expressionType.getSingleJdbcMapping() ).thenReturn( jdbcMapping );
		when( jdbcMapping.getCastType() ).thenReturn( castType );
		return expression;
	}

	private static void assertRequirements(
			ExpressionCoercionSupport support,
			ExpressionCoercionSupport.Requirement... requirements) {
		assertThat( support.getRequirements() ).containsExactlyInAnyOrder( requirements );
	}

	private static class TestingTranslator extends StandardSqlAstTranslator<JdbcSelect> {
		private TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}

		private Object wrapEqualityPreservingConcatenationArgument(Expression expression) throws Exception {
			final Method method = AbstractSqlAstTranslator.class.getDeclaredMethod(
					"wrapRowComponentAsEqualityPreservingConcatArgument",
					Expression.class
			);
			method.setAccessible( true );
			return method.invoke( this, expression );
		}
	}
}
