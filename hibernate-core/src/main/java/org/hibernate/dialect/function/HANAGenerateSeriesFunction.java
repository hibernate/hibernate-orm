/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
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
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;


/**
 * HANA generate_series function.
 */
public class HANAGenerateSeriesFunction extends NumberSeriesGenerateSeriesFunction {

	public HANAGenerateSeriesFunction(int maxSeriesSize, TypeConfiguration typeConfiguration) {
		super(
				new CteGenerateSeriesSetReturningFunctionTypeResolver(),
				// Treat durations like intervals to avoid conversions
				typeConfiguration.getBasicTypeRegistry().resolve(
						java.time.Duration.class,
						SqlTypes.DURATION
				),
				false,
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
			public TableGroup convertToSqlAst(NavigablePath navigablePath, String identifierVariable, boolean lateral, boolean canUseInnerJoins, boolean withOrdinality, SqmToSqlAstConverter walker) {
				final FunctionTableGroup tableGroup = (FunctionTableGroup) super.convertToSqlAst(
						navigablePath,
						identifierVariable,
						lateral,
						canUseInnerJoins,
						withOrdinality,
						walker
				);
				walker.registerQueryTransformer( new HANAGenerateSeriesQueryTransformer(
						tableGroup,
						tableGroup,
						maxSeriesSize,
						"i",
						coerceToTimestamp
				) );
				return tableGroup;
			}
		};
	}

	protected static class HANAGenerateSeriesQueryTransformer extends CteGenerateSeriesFunction.CteGenerateSeriesQueryTransformer {

		public HANAGenerateSeriesQueryTransformer(FunctionTableGroup functionTableGroup, TableGroup targetTableGroup, int maxSeriesSize, String positionColumnName, boolean coerceToTimestamp) {
			super( functionTableGroup, targetTableGroup, maxSeriesSize, positionColumnName, coerceToTimestamp );
		}

		@Override
		protected CteStatement createSeriesCte(SqmToSqlAstConverter converter) {
			final BasicType<String> stringType = converter.getCreationContext().getTypeConfiguration()
					.getBasicTypeForJavaType( String.class );
			final List<CteColumn> cteColumns = List.of( new CteColumn( "v", stringType ) );

			final QuerySpec query = new QuerySpec( false );
			query.getSelectClause().addSqlSelection( new SqlSelectionImpl( new SelfRenderingExpression() {
				@Override
				public void renderToSql(SqlAppender sqlAppender, SqlAstTranslator<?> walker, SessionFactoryImplementor sessionFactory) {
					sqlAppender.appendSql( "'<r>'||lpad(''," );
					sqlAppender.appendSql( maxSeriesSize * 4 );
					sqlAppender.appendSql( ",'<a/>')||'</r>'" );
				}

				@Override
				public JdbcMappingContainer getExpressionType() {
					return stringType;
				}
			} ) );
			return new CteStatement( new CteTable( CteGenerateSeriesFunction.CteGenerateSeriesQueryTransformer.NAME, cteColumns ), new SelectStatement( query ) );
		}
	}

	static class CteGenerateSeriesSetReturningFunctionTypeResolver extends NumberSeriesGenerateSeriesSetReturningFunctionTypeResolver {

		public CteGenerateSeriesSetReturningFunctionTypeResolver() {
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
		final boolean startNeedsVariable = needsVariable( start );
		final boolean stepNeedsVariable = step != null && needsVariable( step );
		if ( startNeedsVariable || stepNeedsVariable ) {
			sqlAppender.appendSql( "((select" );
			char separator = ' ';
			if ( startNeedsVariable ) {
				sqlAppender.appendSql( separator );
				start.accept( walker );
				sqlAppender.appendSql( " b" );
				separator = ',';
			}
			if ( stepNeedsVariable ) {
				sqlAppender.appendSql( separator );
				if ( step instanceof Duration duration ) {
					duration.getMagnitude().accept( walker );
				}
				else {
					step.accept( walker );
				}
				sqlAppender.appendSql( " s" );
			}
			sqlAppender.appendSql( " from sys.dummy) " );
			sqlAppender.appendSql( tableIdentifierVariable );
			sqlAppender.appendSql( "_" );
			sqlAppender.appendSql( " join " );
		}
		sqlAppender.appendSql( "xmltable('/r/a' passing " );
		sqlAppender.appendSql( CteGenerateSeriesFunction.CteGenerateSeriesQueryTransformer.NAME );
		sqlAppender.appendSql( ".v columns i for ordinality) " );
		sqlAppender.appendSql( tableIdentifierVariable );
		if ( startNeedsVariable || stepNeedsVariable ) {
			sqlAppender.appendSql( " on 1=1)" );
		}
	}
}
