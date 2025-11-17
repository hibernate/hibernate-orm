/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * Sybase ASE generate_series function.
 *
 * This implementation first replicates an XML tag with a specified maximum size with the {@code replicate} function
 * and then uses {@code xmltable} to produce rows for every generated element.
 * References to the value are replaced with expressions of the form {@code start + step * iterationVariable}
 * and a condition is added either to the query or join where the function is used to ensure that the value is
 * less than or equal to the stop value.
 */
public class SybaseASEGenerateSeriesFunction extends NumberSeriesGenerateSeriesFunction {

	public SybaseASEGenerateSeriesFunction(int maxSeriesSize, TypeConfiguration typeConfiguration) {
		super(
				new SybaseASEGenerateSeriesSetReturningFunctionTypeResolver(),
				// Treat durations like intervals to avoid conversions
				typeConfiguration.getBasicTypeRegistry().resolve( java.time.Duration.class, SqlTypes.DURATION ),
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
				// Register a query transformer to register a join predicate
				walker.registerQueryTransformer( new NumberSeriesQueryTransformer(
						functionTableGroup,
						functionTableGroup,
						"i",
						coerceToTimestamp
				) );
				return functionTableGroup;
			}
		};
	}

	private static class SybaseASEGenerateSeriesSetReturningFunctionTypeResolver extends NumberSeriesGenerateSeriesSetReturningFunctionTypeResolver {

		public SybaseASEGenerateSeriesSetReturningFunctionTypeResolver() {
			super( "v", "i" );
		}

		@Override
		public SelectableMapping[] resolveFunctionReturnType(
				List<? extends SqlAstNode> arguments,
				String tableIdentifierVariable,
				boolean lateral,
				boolean withOrdinality,
				SqmToSqlAstConverter converter) {
			return resolveIterationVariableBasedFunctionReturnType( arguments, tableIdentifierVariable, lateral, withOrdinality, converter );
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
		final boolean startNeedsEmulation = needsVariable( start );
		final boolean stepNeedsEmulation = step != null && needsVariable( step );
		if ( startNeedsEmulation || stepNeedsEmulation ) {
			sqlAppender.appendSql( "((select" );
			char separator = ' ';
			if ( startNeedsEmulation ) {
				sqlAppender.appendSql( separator );
				start.accept( walker );
				separator = ',';
			}
			if ( stepNeedsEmulation ) {
				sqlAppender.appendSql( separator );
				if ( step instanceof Duration duration ) {
					duration.getMagnitude().accept( walker );
				}
				else {
					step.accept( walker );
				}
			}
			sqlAppender.appendSql( ") " );
			sqlAppender.appendSql( tableIdentifierVariable );
			sqlAppender.appendSql( "_" );
			separator = '(';
			if ( startNeedsEmulation ) {
				sqlAppender.appendSql( separator );
				sqlAppender.appendSql( "b" );
				separator = ',';
			}
			if ( stepNeedsEmulation ) {
				sqlAppender.appendSql( separator );
				sqlAppender.appendSql( "s" );
			}
			sqlAppender.appendSql( ") join " );
		}
		sqlAppender.appendSql( "xmltable('/r/a' passing '<r>'+replicate('<a/>'," );
		sqlAppender.appendSql( maxSeriesSize );
		sqlAppender.appendSql( ")+'</r>' columns i bigint for ordinality, v varchar(255) path '.') " );
		sqlAppender.appendSql( tableIdentifierVariable );
		if ( startNeedsEmulation || stepNeedsEmulation ) {
			sqlAppender.appendSql( " on 1=1)" );
		}
	}

}
