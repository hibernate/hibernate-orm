/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * H2 generate_series function.
 *
 * When possible, the {@code system_range} function is used directly.
 * If ordinality is requested, the arguments are temporals or anything other than literals,
 * this emulation comes into play.
 * It essentially renders a {@code system_range} with a specified maximum size that serves as "iteration variable".
 * References to the value are replaced with expressions of the form {@code start + step * iterationVariable}
 * and a condition is added either to the query or join where the function is used to ensure that the value is
 * less than or equal to the stop value.
 */
public class H2GenerateSeriesFunction extends NumberSeriesGenerateSeriesFunction {

	public H2GenerateSeriesFunction(int maxSeriesSize, TypeConfiguration typeConfiguration) {
		super(
				new H2GenerateSeriesSetReturningFunctionTypeResolver(),
				// Treat durations like intervals to avoid conversions
				typeConfiguration.getBasicTypeRegistry().resolve( java.time.Duration.class, SqlTypes.INTERVAL_SECOND ),
				maxSeriesSize
		);
	}

	@Override
	public boolean rendersIdentifierVariable(List<SqlAstNode> arguments, SessionFactoryImplementor sessionFactory) {
		// To make our lives simpler during emulation
		return true;
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(List<? extends SqmTypedNode<?>> arguments, QueryEngine queryEngine) {
		//noinspection unchecked
		return new SelfRenderingSqmSetReturningFunction<>(
				this,
				this,
				arguments,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		) {
			@Override
			public TableGroup convertToSqlAst(
					NavigablePath navigablePath,
					String identifierVariable,
					boolean lateral,
					boolean canUseInnerJoins,
					boolean withOrdinality,
					SqmToSqlAstConverter walker) {
				// Register a transformer that adds a join predicate "start+(step*(ordinal-1))<=stop"
				final FunctionTableGroup functionTableGroup = (FunctionTableGroup) super.convertToSqlAst(
						navigablePath,
						identifierVariable,
						lateral,
						canUseInnerJoins,
						withOrdinality,
						walker
				);
				//noinspection unchecked
				final List<SqlAstNode> sqlArguments = (List<SqlAstNode>) functionTableGroup.getPrimaryTableReference()
						.getFunctionExpression()
						.getArguments();
				final Expression startExpression = (Expression) sqlArguments.get( 0 );
				final Expression stopExpression = (Expression) sqlArguments.get( 1 );
				final Expression explicitStepExpression = sqlArguments.size() > 2
						? (Expression) sqlArguments.get( 2 )
						: null;
				final boolean needsEmulation = needsEmulation( startExpression )
						|| needsEmulation( stopExpression )
						|| explicitStepExpression != null && needsEmulation( explicitStepExpression );
				final ModelPart elementPart = functionTableGroup.getModelPart()
						.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null );
				final boolean isTemporal = elementPart.getSingleJdbcMapping().getJdbcType().isTemporal();
				// Only do this transformation if one of the arguments is anything but a literal or parameter,
				// ordinality is requested or the result is a temporal (H2 only supports numerics in system_range)
				if ( needsEmulation || withOrdinality || isTemporal ) {
					// Register a query transformer to register a join predicate
					walker.registerQueryTransformer( new NumberSeriesQueryTransformer(
							functionTableGroup,
							functionTableGroup,
							"x",
							coerceToTimestamp
					) );
				}
				return functionTableGroup;
			}
		};
	}

	private static class H2GenerateSeriesSetReturningFunctionTypeResolver extends NumberSeriesGenerateSeriesSetReturningFunctionTypeResolver {

		public H2GenerateSeriesSetReturningFunctionTypeResolver() {
			super( "x", "x" );
		}

		@Override
		public SelectableMapping[] resolveFunctionReturnType(
				List<? extends SqlAstNode> arguments,
				String tableIdentifierVariable,
				boolean lateral,
				boolean withOrdinality,
				SqmToSqlAstConverter converter) {
			final Expression start = (Expression) arguments.get( 0 );
			final Expression stop = (Expression) arguments.get( 1 );
			final JdbcMappingContainer expressionType = NullnessHelper.coalesce(
					start.getExpressionType(),
					stop.getExpressionType()
			);
			final Expression explicitStep = arguments.size() > 2
					? (Expression) arguments.get( 2 )
					: null;
			final ColumnReference joinBaseColumnReference = NullnessHelper.coalesce(
					start.getColumnReference(),
					stop.getColumnReference(),
					explicitStep != null
							? explicitStep.getColumnReference()
							: null
			);
			final JdbcMapping type = expressionType.getSingleJdbcMapping();
			if ( type == null ) {
				throw new IllegalArgumentException( "Couldn't determine types of arguments to function 'generate_series'" );
			}

			if ( joinBaseColumnReference != null || withOrdinality || type.getJdbcType().isTemporal() ) {
				return resolveIterationVariableBasedFunctionReturnType( arguments, tableIdentifierVariable, lateral, withOrdinality, converter );
			}
			else {
				return super.resolveFunctionReturnType( arguments, tableIdentifierVariable, lateral, withOrdinality, converter );
			}
		}
	}

	@Override
	protected void renderGenerateSeries(
			SqlAppender sqlAppender,
			Expression start,
			Expression stop,
			@Nullable Expression step,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final boolean needsEmulation = needsEmulation( start )
				|| needsEmulation( stop )
				|| step != null && needsEmulation( step );
		final ModelPart elementPart = tupleType.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null );
		final ModelPart ordinalityPart = tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null );
		final boolean isTemporal = elementPart.getSingleJdbcMapping().getJdbcType().isTemporal();

		if ( needsEmulation || ordinalityPart != null || isTemporal ) {
			final boolean startNeedsVariable = needsVariable( start );
			final boolean stepNeedsVariable = step != null && needsVariable( step );
			if ( startNeedsVariable || stepNeedsVariable ) {
				sqlAppender.appendSql( "((values " );
				char separator = '(';
				if ( startNeedsVariable ) {
					sqlAppender.appendSql( separator );
					start.accept( walker );
					separator = ',';
				}
				if ( stepNeedsVariable ) {
					sqlAppender.appendSql( separator );
					step.accept( walker );
				}
				sqlAppender.appendSql( ")) " );
				sqlAppender.appendSql( tableIdentifierVariable );
				sqlAppender.appendSql( "_" );
				separator = '(';
				if ( startNeedsVariable ) {
					sqlAppender.appendSql( separator );
					sqlAppender.appendSql( "b" );
					separator = ',';
				}
				if ( stepNeedsVariable ) {
					sqlAppender.appendSql( separator );
					sqlAppender.appendSql( "s" );
				}
				sqlAppender.appendSql( ") join " );
			}
			sqlAppender.appendSql( "system_range(1," );
			sqlAppender.appendSql( maxSeriesSize );
			sqlAppender.appendSql( ") " );
			sqlAppender.appendSql( tableIdentifierVariable );
			if ( startNeedsVariable || stepNeedsVariable ) {
				sqlAppender.appendSql( " on true)" );
			}
		}
		else {
			sqlAppender.appendSql( "system_range(" );
			start.accept( walker );
			sqlAppender.appendSql( ',' );
			stop.accept( walker );
			if ( step != null ) {
				sqlAppender.appendSql( ',' );
				step.accept( walker );
			}
			sqlAppender.appendSql( ") " );
			sqlAppender.appendSql( tableIdentifierVariable );
		}
	}

	private static boolean needsEmulation(Expression expression) {
		return !( expression instanceof Literal || AbstractSqlAstTranslator.isParameter( expression ) );
	}
}
