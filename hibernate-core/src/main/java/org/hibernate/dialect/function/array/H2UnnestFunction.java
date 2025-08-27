/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.dialect.function.UnnestSetReturningFunctionTypeResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * H2 unnest function.
 * <p>
 * H2 does not support "lateral" i.e. the use of a from node within another,
 * but we can apply the same trick that we already applied everywhere else for H2,
 * which is to join a sequence table to emulate array element rows
 * and eliminate non-existing array elements by checking the index against array length.
 * Finally, we rewrite the selection expressions to access the array by joined sequence index.
 */
public class H2UnnestFunction extends UnnestFunction {

	private final int maximumArraySize;

	public H2UnnestFunction(int maximumArraySize) {
		super( new H2UnnestSetReturningFunctionTypeResolver() );
		this.maximumArraySize = maximumArraySize;
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			QueryEngine queryEngine) {
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
				// Register a transformer that adds a join predicate "array_length(array) <= index"
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
				// Can only do this transformation if the argument is a column reference
				final ColumnReference columnReference = ( (Expression) sqlArguments.get( 0 ) ).getColumnReference();
				if ( columnReference != null ) {
					final String tableQualifier = columnReference.getQualifier();
					// Find the table group which the unnest argument refers to
					final FromClauseAccess fromClauseAccess = walker.getFromClauseAccess();
					final TableGroup sourceTableGroup =
							fromClauseAccess.findTableGroupByIdentificationVariable( tableQualifier );
					if ( sourceTableGroup != null ) {
						// Register a query transformer to register a join predicate
						walker.registerQueryTransformer( (cteContainer, querySpec, converter) -> {
							final TableGroup parentTableGroup = querySpec.getFromClause().queryTableGroups(
									tg -> tg.findTableGroupJoin( functionTableGroup ) == null ? null : tg
							);
							final TableGroupJoin join = parentTableGroup.findTableGroupJoin( functionTableGroup );
							final BasicType<Integer> integerType = walker.getSqmCreationContext()
									.getNodeBuilder()
									.getIntegerType();
							final Expression lhs = new SelfRenderingExpression() {
								@Override
								public void renderToSql(
										SqlAppender sqlAppender,
										SqlAstTranslator<?> walker,
										SessionFactoryImplementor sessionFactory) {
									sqlAppender.append( "coalesce(array_length(" );
									columnReference.accept( walker );
									sqlAppender.append( "),0)" );
								}

								@Override
								public JdbcMappingContainer getExpressionType() {
									return integerType;
								}
							};
							final Expression rhs = new ColumnReference(
									functionTableGroup.getPrimaryTableReference().getIdentificationVariable(),
									// The default column name for the system_range function
									"x",
									false,
									null,
									integerType
							);
							join.applyPredicate( new ComparisonPredicate( lhs, ComparisonOperator.GREATER_THAN_OR_EQUAL, rhs ) );
							return querySpec;
						} );
					}
				}
				return functionTableGroup;
			}
		};
	}

	@Override
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		renderUnnest( sqlAppender, array, pluralType, sqlTypedMapping, tupleType, tableIdentifierVariable, walker );
	}

	@Override
	protected void renderUnnest(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final ColumnReference columnReference = array.getColumnReference();
		if ( columnReference != null ) {
			sqlAppender.append( "system_range(1," );
			sqlAppender.append( Integer.toString( maximumArraySize ) );
			sqlAppender.append( ")" );
		}
		else {
			super.renderUnnest( sqlAppender, array, pluralType, sqlTypedMapping, tupleType, tableIdentifierVariable, walker );
		}
	}

	private static class H2UnnestSetReturningFunctionTypeResolver extends UnnestSetReturningFunctionTypeResolver {

		public H2UnnestSetReturningFunctionTypeResolver() {
			// c1 is the default column name for the "unnest()" function
			super( "c1", "nord" );
		}

		@Override
		public SelectableMapping[] resolveFunctionReturnType(
				List<? extends SqlAstNode> arguments,
				String tableIdentifierVariable,
				boolean lateral,
				boolean withOrdinality,
				SqmToSqlAstConverter converter) {
			final Expression expression = (Expression) arguments.get( 0 );
			final JdbcMappingContainer expressionType = expression.getExpressionType();
			if ( expressionType == null ) {
				throw new IllegalArgumentException( "Couldn't determine array type of argument to function 'unnest'" );
			}
			if ( !( expressionType.getSingleJdbcMapping() instanceof BasicPluralType<?,?> pluralType ) ) {
				throw new IllegalArgumentException( "Argument passed to function 'unnest' is not a BasicPluralType. Found: " + expressionType );
			}

			final SelectableMapping indexMapping = withOrdinality ? new SelectableMappingImpl(
					"",
					expression.getColumnReference() != null ? "x" : defaultIndexSelectionExpression,
					new SelectablePath( CollectionPart.Nature.INDEX.getName() ),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					false,
					false,
					false,
					false,
					false,
					false,
					converter.getCreationContext().getTypeConfiguration().getBasicTypeForJavaType( Long.class )
			) : null;

			final BasicType<?> elementType = pluralType.getElementType();
			final SelectableMapping[] returnType;
			if ( elementType.getJdbcType() instanceof AggregateJdbcType aggregateJdbcType
					&& aggregateJdbcType.getEmbeddableMappingType() != null ) {
				final ColumnReference arrayColumnReference = expression.getColumnReference();
				if ( arrayColumnReference == null ) {
					throw new IllegalArgumentException( "Argument passed to function 'unnest' is not a column reference, but an aggregate type, which is not yet supported." );
				}
				// For column references we render an emulation through system_range(),
				// so we need to render an array access to get to the element
				final String elementReadExpression = "array_get(" + arrayColumnReference.getExpressionText() + "," + Template.TEMPLATE + ".x)";
				final String arrayReadExpression = NullnessUtil.castNonNull( arrayColumnReference.getReadExpression() );
				final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
				final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
				returnType = new SelectableMapping[jdbcValueCount + (indexMapping == null ? 0 : 1)];
				for ( int i = 0; i < jdbcValueCount; i++ ) {
					final SelectableMapping selectableMapping = embeddableMappingType.getJdbcValueSelectable( i );
					// The array expression has to be replaced with the actual array_get read expression in this emulation
					final String customReadExpression = selectableMapping.getCustomReadExpression()
							.replace( arrayReadExpression, elementReadExpression );
					returnType[i] = new SelectableMappingImpl(
							selectableMapping.getContainingTableExpression(),
							selectableMapping.getSelectablePath().getSelectableName(),
							new SelectablePath( selectableMapping.getSelectablePath().getSelectableName() ),
							customReadExpression,
							selectableMapping.getCustomWriteExpression(),
							selectableMapping.getColumnDefinition(),
							selectableMapping.getLength(),
							selectableMapping.getPrecision(),
							selectableMapping.getScale(),
							selectableMapping.getTemporalPrecision(),
							selectableMapping.isLob(),
							true,
							false,
							false,
							false,
							selectableMapping.isFormula(),
							selectableMapping.getJdbcMapping()
					);
				}
				if ( indexMapping != null ) {
					returnType[jdbcValueCount] = indexMapping;
				}
			}
			else {
				final String elementSelectionExpression;
				final String elementReadExpression;
				final ColumnReference columnReference = expression.getColumnReference();
				if ( columnReference != null ) {
					// For column references we render an emulation through system_range(),
					// so we need to render an array access to get to the element
					elementSelectionExpression = columnReference.getColumnExpression();
					elementReadExpression = "array_get(" + columnReference.getExpressionText() + "," + Template.TEMPLATE + ".x)";
				}
				else {
					elementSelectionExpression = defaultBasicArrayColumnName;
					elementReadExpression = null;
				}
				final SelectableMapping elementMapping;
				if ( expressionType instanceof SqlTypedMapping typedMapping ) {
					elementMapping = new SelectableMappingImpl(
							"",
							elementSelectionExpression,
							new SelectablePath( CollectionPart.Nature.ELEMENT.getName() ),
							elementReadExpression,
							null,
							typedMapping.getColumnDefinition(),
							typedMapping.getLength(),
							typedMapping.getPrecision(),
							typedMapping.getScale(),
							typedMapping.getTemporalPrecision(),
							typedMapping.isLob(),
							true,
							false,
							false,
							false,
							false,
							elementType
					);
				}
				else {
					elementMapping = new SelectableMappingImpl(
							"",
							elementSelectionExpression,
							new SelectablePath( CollectionPart.Nature.ELEMENT.getName() ),
							elementReadExpression,
							null,
							null,
							null,
							null,
							null,
							null,
							false,
							true,
							false,
							false,
							false,
							false,
							elementType
					);
				}
				if ( indexMapping == null ) {
					returnType = new SelectableMapping[]{ elementMapping };
				}
				else {
					returnType = new SelectableMapping[] {elementMapping, indexMapping};
				}
			}
			return returnType;
		}
	}
}
