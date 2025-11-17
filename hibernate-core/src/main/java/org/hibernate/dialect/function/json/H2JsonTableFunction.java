/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmJsonTableFunction;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.JsonTableErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonTableExistsColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableNestedColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableValueColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateContainer;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;


/**
 * H2 json_table function.
 * <p>
 * H2 does not support "lateral" i.e. the use of a from node within another,
 * but we can apply the same trick that we already applied everywhere else for H2,
 * which is to join a sequence table to emulate array element rows
 * and eliminate non-existing array elements by checking the index against array length.
 * Finally, we rewrite the selection expressions to access the array by joined sequence index.
 */
public class H2JsonTableFunction extends JsonTableFunction {

	private final int maximumArraySize;

	public H2JsonTableFunction(int maximumArraySize, TypeConfiguration typeConfiguration) {
		super( new H2JsonTableSetReturningFunctionTypeResolver(), typeConfiguration );
		this.maximumArraySize = maximumArraySize;
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(
			List<? extends SqmTypedNode<?>> sqmArguments,
			QueryEngine queryEngine) {
		//noinspection unchecked
		return new SqmJsonTableFunction<>(
				this,
				this,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				(SqmExpression<?>) sqmArguments.get( 0 ),
				sqmArguments.size() > 1 ? (SqmExpression<String>) sqmArguments.get( 1 ) : null
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
				final JsonTableArguments arguments = JsonTableArguments.extract(
						functionTableGroup.getPrimaryTableReference().getFunctionExpression().getArguments()
				);
				// Register a query transformer to register a join predicate
				walker.registerQueryTransformer(
						new JsonTableQueryTransformer( functionTableGroup, arguments, maximumArraySize ) );
				return functionTableGroup;
			}
		};
	}

	private static class JsonTableQueryTransformer implements QueryTransformer {
		private final FunctionTableGroup functionTableGroup;
		private final JsonTableArguments arguments;
		private final int maximumArraySize;

		public JsonTableQueryTransformer(FunctionTableGroup functionTableGroup, JsonTableArguments arguments, int maximumArraySize) {
			this.functionTableGroup = functionTableGroup;
			this.arguments = arguments;
			this.maximumArraySize = maximumArraySize;
		}

		@Override
		public QuerySpec transform(CteContainer cteContainer, QuerySpec querySpec, SqmToSqlAstConverter converter) {
			final boolean isArray;
			if ( arguments.jsonPath() != null ) {
				if ( !( arguments.jsonPath() instanceof Literal literal) ) {
					throw new QueryException( "H2 json_table() only supports literal json paths, but got " + arguments.jsonPath() );
				}
				final String rawJsonPath = (String) literal.getLiteralValue();
				isArray = isArrayAccess( rawJsonPath );
			}
			else {
				// We have to assume this is an array
				isArray = true;
			}
			if ( isArray ) {
				final TableGroup parentTableGroup = querySpec.getFromClause().queryTableGroups(
						tg -> tg.findTableGroupJoin( functionTableGroup ) == null ? null : tg
				);
				final PredicateContainer predicateContainer;
				if ( parentTableGroup != null ) {
					predicateContainer = parentTableGroup.findTableGroupJoin( functionTableGroup );
				}
				else {
					predicateContainer = querySpec;
				}
				final BasicType<Integer> integerType = converter.getSqmCreationContext()
						.getNodeBuilder()
						.getIntegerType();
				final Expression jsonDocument;
				if ( arguments.jsonDocument().getColumnReference() == null ) {
					jsonDocument = new ColumnReference(
							functionTableGroup.getPrimaryTableReference().getIdentificationVariable() + "_",
							"d",
							false,
							null,
							arguments.jsonDocument().getExpressionType().getSingleJdbcMapping()
					);
				}
				else {
					jsonDocument = arguments.jsonDocument();
				}
				final Expression lhs = new ArrayLengthExpression( jsonDocument, integerType );
				final Expression rhs = new ColumnReference(
						functionTableGroup.getPrimaryTableReference().getIdentificationVariable(),
						// The default column name for the system_range function
						"x",
						false,
						null,
						integerType
				);
				predicateContainer.applyPredicate(
						new ComparisonPredicate( lhs, ComparisonOperator.GREATER_THAN_OR_EQUAL, rhs ) );
			}
			final int lastArrayIndex = getLastArrayIndex( arguments.columnsClause(), 0 );
			if ( lastArrayIndex != 0 ) {
				// Create a synthetic function table group which will render system_range() joins
				// for every nested path for arrays
				final String tableIdentifierVariable = functionTableGroup.getPrimaryTableReference()
						.getIdentificationVariable();
				final Expression jsonDocument;
				if ( arguments.jsonDocument().getColumnReference() == null ) {
					jsonDocument = new ColumnReference(
							tableIdentifierVariable + "_",
							"d",
							false,
							null,
							arguments.jsonDocument().getExpressionType().getSingleJdbcMapping()
					);
				}
				else {
					jsonDocument = arguments.jsonDocument();
				}
				final TableGroup tableGroup = new FunctionTableGroup(
						functionTableGroup.getNavigablePath().append( "{synthetic}" ),
						null,
						new SelfRenderingFunctionSqlAstExpression(
								"json_table_emulation",
								new NestedPathFunctionRenderer(
										tableIdentifierVariable,
										arguments,
										jsonDocument,
										maximumArraySize,
										lastArrayIndex
								),
								emptyList(),
								null,
								null
						),
						tableIdentifierVariable + "_synthetic_",
						emptyList(),
						Set.of( "" ),
						false,
						false,
						true,
						converter.getCreationContext().getSessionFactory()
				);
				final BasicType<Integer> integerType = converter.getSqmCreationContext()
						.getNodeBuilder()
						.getIntegerType();

				// The join predicate compares the length of the last array expression against system_range() index.
				// Since a table function expression can't render its own `on` clause, this split of logic is necessary
				final Expression lhs = new ArrayLengthExpression(
						determineLastArrayExpression( tableIdentifierVariable, arguments, jsonDocument ),
						integerType
				);
				final Expression rhs = new ColumnReference(
						tableIdentifierVariable + "_" + lastArrayIndex + "_",
						// The default column name for the system_range function
						"x",
						false,
						null,
						integerType
				);
				final Predicate predicate = new ComparisonPredicate( lhs, ComparisonOperator.GREATER_THAN_OR_EQUAL, rhs );
				functionTableGroup.addTableGroupJoin(
						new TableGroupJoin( tableGroup.getNavigablePath(), SqlAstJoinType.LEFT, tableGroup, predicate )
				);
			}
			return querySpec;
		}

		private static Expression determineLastArrayExpression(String tableIdentifierVariable, JsonTableArguments arguments, Expression jsonDocument) {
			final ArrayExpressionEntry arrayExpressionEntry = determineLastArrayExpression(
					tableIdentifierVariable,
					determineJsonElement( tableIdentifierVariable, arguments, jsonDocument ),
					arguments.columnsClause(),
					new ArrayExpressionEntry( 0, null )
			);
			return NullnessUtil.castNonNull( arrayExpressionEntry.expression() );
		}

		record ArrayExpressionEntry(int arrayIndex, @Nullable Expression expression) {
		}

		private static ArrayExpressionEntry determineLastArrayExpression(String tableIdentifierVariable, Expression parentJson, JsonTableColumnsClause jsonTableColumnsClause, ArrayExpressionEntry parentEntry) {
			// Depth-first traversal to obtain the last nested path that refers to an array within this tree
			ArrayExpressionEntry currentArrayEntry = parentEntry;
			for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
				if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
					final String rawJsonPath = nestedColumnDefinition.jsonPath();
					final boolean isArray = isArrayAccess( rawJsonPath );
					final String jsonPath = isArray ? rawJsonPath.substring( 0, rawJsonPath.length() - 3 ) : rawJsonPath;

					final Expression jsonQueryResult = new JsonValueExpression( parentJson, jsonPath, null );
					final Expression jsonElement;
					final ArrayExpressionEntry nextArrayExpression;
					if ( isArray ) {
						final int nextArrayIndex = currentArrayEntry.arrayIndex() + 1;
						jsonElement = new ArrayAccessExpression( jsonQueryResult, ordinalityExpression( tableIdentifierVariable, nextArrayIndex ) );
						nextArrayExpression = new ArrayExpressionEntry( nextArrayIndex, jsonQueryResult );
					}
					else {
						jsonElement = jsonQueryResult;
						nextArrayExpression = currentArrayEntry;
					}
					currentArrayEntry = determineLastArrayExpression(
							tableIdentifierVariable,
							jsonElement,
							nestedColumnDefinition.columns(),
							nextArrayExpression
					);
				}
			}
			return currentArrayEntry;
		}

		private static Expression determineJsonElement(String tableIdentifierVariable, JsonTableArguments arguments, Expression jsonDocument) {
			// Applies the json path and array index access to obtain the "current" processing element

			final boolean isArray;
			final Expression jsonQueryResult;
			if ( arguments.jsonPath() != null ) {
				if ( !(arguments.jsonPath() instanceof Literal literal) ) {
					throw new QueryException(
							"H2 json_table() only supports literal json paths, but got " + arguments.jsonPath() );
				}
				final String rawJsonPath = (String) literal.getLiteralValue();
				isArray = isArrayAccess( rawJsonPath );
				final String jsonPath = isArray ? rawJsonPath.substring( 0, rawJsonPath.length() - 3 ) : rawJsonPath;

				jsonQueryResult = "$".equals( jsonPath )
						? jsonDocument
						: new JsonValueExpression( jsonDocument, arguments.isJsonType(), jsonPath, arguments.passingClause() );
			}
			else {
				// We have to assume this is an array
				isArray = true;
				jsonQueryResult = jsonDocument;
			}

			final Expression jsonElement;
			if ( isArray ) {
				jsonElement = new ArrayAccessExpression( jsonQueryResult, tableIdentifierVariable + ".x" );
			}
			else {
				jsonElement = jsonQueryResult;
			}
			return jsonElement;
		}

		private static class NestedPathFunctionRenderer implements FunctionRenderer {
			private final String tableIdentifierVariable;
			private final JsonTableArguments arguments;
			private final Expression jsonDocument;
			private final int maximumArraySize;
			private final int lastArrayIndex;

			public NestedPathFunctionRenderer(String tableIdentifierVariable, JsonTableArguments arguments, Expression jsonDocument, int maximumArraySize, int lastArrayIndex) {
				this.tableIdentifierVariable = tableIdentifierVariable;
				this.arguments = arguments;
				this.jsonDocument = jsonDocument;
				this.maximumArraySize = maximumArraySize;
				this.lastArrayIndex = lastArrayIndex;
			}

			@Override
			public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> walker) {
				final Expression jsonElement = determineJsonElement( tableIdentifierVariable, arguments, jsonDocument );
				renderNestedColumnJoins( sqlAppender, tableIdentifierVariable, jsonElement, arguments.columnsClause(), 0, lastArrayIndex, walker );
			}

			private int renderNestedColumnJoins(SqlAppender sqlAppender, String tableIdentifierVariable, Expression parentJson, JsonTableColumnsClause jsonTableColumnsClause, int arrayIndex, int lastArrayIndex, SqlAstTranslator<?> walker) {
				// H2 doesn't support lateral joins, so we have to emulate array flattening by joining against a
				// system_range() with a condition that checks if the array index is still within bounds
				int currentArrayIndex = arrayIndex;
				for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
					if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
						final String rawJsonPath = nestedColumnDefinition.jsonPath();
						final boolean isArray = isArrayAccess( rawJsonPath );
						final String jsonPath = isArray ? rawJsonPath.substring( 0, rawJsonPath.length() - 3 ) : rawJsonPath;
						final int nextArrayIndex = currentArrayIndex + ( isArray ? 1 : 0 );

						// The left join for the first element was already rendered via TableGroupJoin
						if ( isArray && currentArrayIndex != 0 ) {
							sqlAppender.appendSql( " left join " );
						}
						final Expression jsonQueryResult = new JsonValueExpression( parentJson, jsonPath, null );
						final Expression jsonElement;
						if ( isArray ) {
							// Only render system ranges for arrays
							sqlAppender.append( "system_range(1," );
							sqlAppender.append( Integer.toString( maximumArraySize ) );
							sqlAppender.append( ") " );
							sqlAppender.appendSql( tableIdentifierVariable );
							sqlAppender.appendSql( '_' );
							sqlAppender.appendSql( nextArrayIndex );
							sqlAppender.appendSql( '_' );

							final String ordinalityExpression = ordinalityExpression( tableIdentifierVariable, nextArrayIndex );
							// The join condition for the last array will be rendered via TableGroupJoin
							if ( nextArrayIndex != lastArrayIndex ) {
								sqlAppender.appendSql( " on coalesce(array_length(" );
								jsonQueryResult.accept( walker );
								sqlAppender.append( "),0)>=" );
								sqlAppender.appendSql( ordinalityExpression );
							}
							jsonElement = new ArrayAccessExpression( jsonQueryResult, ordinalityExpression );
						}
						else {
							jsonElement = jsonQueryResult;
						}
						currentArrayIndex = renderNestedColumnJoins(
								sqlAppender,
								tableIdentifierVariable,
								jsonElement,
								nestedColumnDefinition.columns(),
								nextArrayIndex,
								lastArrayIndex,
								walker
						);
					}
				}
				return currentArrayIndex;
			}
		}
	}

	@Override
	public boolean rendersIdentifierVariable(List<SqlAstNode> arguments, SessionFactoryImplementor sessionFactory) {
		// To make our lives simpler when supporting non-column JSON document arguments
		return true;
	}

	@Override
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			JsonTableArguments arguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		if ( arguments.errorBehavior() == JsonTableErrorBehavior.NULL ) {
			throw new QueryException( "Can't emulate null on error clause on H2" );
		}

		final Expression jsonPathExpression = arguments.jsonPath();
		final boolean isArray = isArrayAccess( jsonPathExpression, walker );

		if ( arguments.jsonDocument().getColumnReference() == null ) {
			sqlAppender.append( '(' );
		}
		if ( isArray ) {
			sqlAppender.append( "system_range(1," );
			sqlAppender.append( Integer.toString( maximumArraySize ) );
			sqlAppender.append( ") " );
		}
		else {
			sqlAppender.append( "system_range(1,1) " );
		}
		sqlAppender.append( tableIdentifierVariable );
		if ( arguments.jsonDocument().getColumnReference() == null ) {
			sqlAppender.append( " join (values (" );
			arguments.jsonDocument().accept( walker );
			if ( !arguments.isJsonType() ) {
				sqlAppender.append( " format json" );
			}
			sqlAppender.append( ")) " );
			sqlAppender.append( tableIdentifierVariable );
			sqlAppender.append( "_(d) on 1=1)" );
		}
	}

	private static boolean isArrayAccess(@Nullable Expression jsonPath, SqlAstTranslator<?> walker) {
		if ( jsonPath != null ) {
			try {
				return isArrayAccess( walker.getLiteralValue( jsonPath ) );
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		// Assume array by default
		return true;
	}

	private static boolean isArrayAccess(String jsonPath) {
		return jsonPath.endsWith( "[*]" );
	}

	private static int getLastArrayIndex(JsonTableColumnsClause jsonTableColumnsClause, int arrayIndex) {
		int currentArrayIndex = arrayIndex;
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
				currentArrayIndex = getLastArrayIndex(
						nestedColumnDefinition.columns(),
						arrayIndex + (isArrayAccess( nestedColumnDefinition.jsonPath() ) ? 1 : 0 )
				);
			}
		}
		return currentArrayIndex;
	}

	private static class JsonValueExpression implements SelfRenderingExpression {
		private final Expression jsonDocument;
		private final boolean isJsonType;
		private final String jsonPath;
		private final @Nullable JsonPathPassingClause passingClause;

		public JsonValueExpression(Expression jsonDocument, String jsonPath, @Nullable JsonPathPassingClause passingClause) {
			this.jsonDocument = jsonDocument;
			// This controls whether we put parenthesis around the document on dereference
			this.isJsonType = jsonDocument instanceof JsonValueExpression
					|| jsonDocument instanceof ArrayAccessExpression;
			this.jsonPath = jsonPath;
			this.passingClause = passingClause;
		}

		public JsonValueExpression(Expression jsonDocument, boolean isJsonType, String jsonPath, @Nullable JsonPathPassingClause passingClause) {
			this.jsonDocument = jsonDocument;
			this.isJsonType = isJsonType;
			this.jsonPath = jsonPath;
			this.passingClause = passingClause;
		}

		@Override
		public void renderToSql(SqlAppender sqlAppender, SqlAstTranslator<?> walker, SessionFactoryImplementor sessionFactory) {
			H2JsonValueFunction.renderJsonPath(
					sqlAppender,
					jsonDocument,
					isJsonType,
					walker,
					jsonPath,
					passingClause
			);
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return null;
		}
	}

	private static class ArrayAccessExpression implements SelfRenderingExpression {
		private final Expression array;
		private final String indexFragment;

		public ArrayAccessExpression(Expression array, String indexFragment) {
			this.array = array;
			this.indexFragment = indexFragment;
		}

		@Override
		public void renderToSql(SqlAppender sqlAppender, SqlAstTranslator<?> walker, SessionFactoryImplementor sessionFactory) {
			sqlAppender.appendSql( "array_get(" );
			array.accept( walker );
			sqlAppender.appendSql( ',' );
			sqlAppender.appendSql( indexFragment );
			sqlAppender.appendSql( ')' );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return null;
		}
	}

	private static class ArrayLengthExpression implements SelfRenderingExpression {
		private final Expression arrayExpression;
		private final BasicType<Integer> integerType;

		public ArrayLengthExpression(Expression arrayExpression, BasicType<Integer> integerType) {
			this.arrayExpression = arrayExpression;
			this.integerType = integerType;
		}

		@Override
		public void renderToSql(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> walker,
				SessionFactoryImplementor sessionFactory) {
			sqlAppender.append( "coalesce(array_length(" );
			arrayExpression.accept( walker );
			sqlAppender.append( "),0)" );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return integerType;
		}
	}

	private static String ordinalityExpression(String tableIdentifierVariable, int clauseLevel) {
		if ( clauseLevel == 0 ) {
			return tableIdentifierVariable + ".x";
		}
		return tableIdentifierVariable + "_" + clauseLevel + "_.x";
	}

	/**
	 * This type resolver essentially implements all the JSON path handling and casting via column read expressions
	 * instead of rendering to the {@code from} clause like other {@code json_table()} implementations.
	 * This is necessary because H2 does not support lateral joins.
	 * The rendering is tightly coupled to the {@code system_range()} joins that are rendered for nested paths
	 * that refer to arrays.
	 */
	private static class H2JsonTableSetReturningFunctionTypeResolver extends JsonTableSetReturningFunctionTypeResolver {
		public H2JsonTableSetReturningFunctionTypeResolver() {
		}

		@Override
		public SelectableMapping[] resolveFunctionReturnType(
				List<? extends SqlAstNode> sqlAstNodes,
				String tableIdentifierVariable,
				boolean lateral,
				boolean withOrdinality,
				SqmToSqlAstConverter converter) {
			final JsonTableArguments arguments = JsonTableArguments.extract( sqlAstNodes );
			final Expression jsonDocument = arguments.jsonDocument();
			final String documentPath;
			final ColumnReference columnReference = jsonDocument.getColumnReference();
			if ( columnReference != null ) {
				documentPath = columnReference.getExpressionText();
			}
			else {
				documentPath = tableIdentifierVariable + "_." + "d";
			}

			final String parentPath;
			final boolean isArray;
			if ( arguments.jsonPath() != null ) {
				if ( !( arguments.jsonPath() instanceof Literal literal) ) {
					throw new QueryException( "H2 json_table() only supports literal json paths, but got " + arguments.jsonPath() );
				}
				final String rawJsonPath = (String) literal.getLiteralValue();
				isArray = isArrayAccess( rawJsonPath );
				final String jsonPath = isArray ? rawJsonPath.substring( 0, rawJsonPath.length() - 3 ) : rawJsonPath;
				parentPath = H2JsonValueFunction.applyJsonPath( documentPath, true, arguments.isJsonType(), jsonPath, arguments.passingClause() );
			}
			else {
				// We have to assume this is an array
				isArray = true;
				parentPath = documentPath;
			}

			final String parentReadExpression;
			if ( isArray ) {
				parentReadExpression = "array_get(" + parentPath + "," + tableIdentifierVariable + ".x)";
			}
			else {
				parentReadExpression = '(' + parentPath + ')';
			}
			final List<JsonTableColumnDefinition> columnDefinitions = arguments.columnsClause().getColumnDefinitions();
			final List<SelectableMapping> selectableMappings = new ArrayList<>( columnDefinitions.size() );
			addSelectableMappings( selectableMappings, tableIdentifierVariable, arguments.columnsClause(), 0, parentReadExpression, converter );
			return selectableMappings.toArray( new SelectableMapping[0] );
		}

		protected int addSelectableMappings(List<SelectableMapping> selectableMappings, String tableIdentifierVariable, JsonTableColumnsClause columnsClause, int clauseLevel, String parentReadExpression, SqmToSqlAstConverter converter) {
			int currentClauseLevel = clauseLevel;
			for ( JsonTableColumnDefinition columnDefinition : columnsClause.getColumnDefinitions() ) {
				if ( columnDefinition instanceof JsonTableExistsColumnDefinition definition ) {
					addSelectableMappings( selectableMappings, definition, parentReadExpression, converter );
				}
				else if ( columnDefinition instanceof JsonTableQueryColumnDefinition definition ) {
					addSelectableMappings( selectableMappings, definition, parentReadExpression, converter );
				}
				else if ( columnDefinition instanceof JsonTableValueColumnDefinition definition ) {
					addSelectableMappings( selectableMappings, definition, parentReadExpression, converter );
				}
				else if ( columnDefinition instanceof JsonTableOrdinalityColumnDefinition definition ) {
					addSelectableMappings( selectableMappings, tableIdentifierVariable, definition, clauseLevel, converter );
				}
				else {
					final JsonTableNestedColumnDefinition definition = (JsonTableNestedColumnDefinition) columnDefinition;
					currentClauseLevel = addSelectableMappings(
							selectableMappings,
							tableIdentifierVariable,
							definition,
							currentClauseLevel,
							parentReadExpression,
							converter
					);
				}
			}
			return currentClauseLevel;
		}

		protected int addSelectableMappings(List<SelectableMapping> selectableMappings, String tableIdentifierVariable, JsonTableNestedColumnDefinition columnDefinition, int clauseLevel, String parentReadExpression, SqmToSqlAstConverter converter) {
			final String rawJsonPath = columnDefinition.jsonPath();
			final boolean isArray = isArrayAccess( rawJsonPath );
			final String jsonPath = isArray ? rawJsonPath.substring( 0, rawJsonPath.length() - 3 ) : rawJsonPath;
			final String parentPath = H2JsonValueFunction.applyJsonPath( parentReadExpression, false, true, jsonPath, null );

			final int nextClauseLevel;
			final String readExpression;
			if ( isArray ) {
				nextClauseLevel = clauseLevel + 1;
				readExpression = "array_get(" + parentPath + "," + ordinalityExpression( tableIdentifierVariable, nextClauseLevel ) + ")";
			}
			else {
				nextClauseLevel = clauseLevel;
				readExpression = parentPath;
			}
			return addSelectableMappings( selectableMappings, tableIdentifierVariable, columnDefinition.columns(), nextClauseLevel, readExpression, converter );
		}

		protected void addSelectableMappings(List<SelectableMapping> selectableMappings, String tableIdentifierVariable, JsonTableOrdinalityColumnDefinition definition, int clauseLevel, SqmToSqlAstConverter converter) {
			addSelectableMapping(
					selectableMappings,
					definition.name(),
					ordinalityExpression( tableIdentifierVariable, clauseLevel ),
					converter.getCreationContext().getTypeConfiguration().getBasicTypeForJavaType( Long.class )
			);
		}

		protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableValueColumnDefinition definition, String parentReadExpression, SqmToSqlAstConverter converter) {
			final JsonValueEmptyBehavior emptyBehavior = definition.emptyBehavior();
			final Literal defaultExpression;
			if ( emptyBehavior != null && emptyBehavior.getDefaultExpression() != null ) {
				if ( !( emptyBehavior.getDefaultExpression() instanceof Literal literal ) ) {
					throw new QueryException( "H2 json_table() only supports literal default expressions, but got " + emptyBehavior.getDefaultExpression() );
				}
				defaultExpression = literal;
			}
			else {
				defaultExpression = null;
			}
			final String baseReadExpression = determineElementReadExpression( definition.name(), definition.jsonPath(), parentReadExpression );
			final String elementReadExpression = castValueExpression( baseReadExpression, definition.type(), defaultExpression, converter );

			addSelectableMapping(
					selectableMappings,
					definition.name(),
					elementReadExpression,
					definition.type().getJdbcMapping()
			);
		}

		private String castValueExpression(String baseReadExpression, CastTarget castTarget, @Nullable Literal defaultExpression, SqmToSqlAstConverter converter) {
			final StringBuilder sb = new StringBuilder( baseReadExpression.length() + 200 );
			if ( defaultExpression != null ) {
				sb.append( "coalesce(" );
			}
			final boolean hexDecoding = H2JsonValueFunction.needsHexDecoding( castTarget.getJdbcMapping() );
			sb.append( "cast(" );
			if ( hexDecoding ) {
				// We encode binary data as hex, so we have to decode here
				sb.append( "hextoraw(regexp_replace(" );
			}
			sb.append( "stringdecode(regexp_replace(nullif(" );
			sb.append( baseReadExpression );
			sb.append( ",JSON'null'),'^\"(.*)\"$','$1'))" );
			if ( hexDecoding ) {
				sb.append( ",'([0-9a-f][0-9a-f])','00$1'))" );
			}
			sb.append( " as " );
			sb.append( determineColumnType( castTarget, converter.getCreationContext().getTypeConfiguration() ) );
			sb.append( ')' );
			if ( defaultExpression != null ) {
				sb.append( ',' );
				//noinspection unchecked
				final String sqlLiteral = defaultExpression.getJdbcMapping().getJdbcLiteralFormatter().toJdbcLiteral(
						defaultExpression.getLiteralValue(),
						converter.getCreationContext().getDialect(),
						converter.getCreationContext().getWrapperOptions()
				);
				sb.append( sqlLiteral );
				sb.append( ')' );
			}
			return sb.toString();
		}

		protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableQueryColumnDefinition definition, String parentReadExpression, SqmToSqlAstConverter converter) {
			final String baseReadExpression = determineElementReadExpression( definition.name(), definition.jsonPath(), parentReadExpression );
			final String elementReadExpression = castQueryExpression( baseReadExpression, definition.emptyBehavior(), definition.wrapMode(), converter );

			addSelectableMapping(
					selectableMappings,
					definition.name(),
					elementReadExpression,
					converter.getCreationContext().getTypeConfiguration().getBasicTypeRegistry()
							.resolve( String.class, SqlTypes.JSON )
			);
		}

		private String castQueryExpression(String baseReadExpression, JsonQueryEmptyBehavior emptyBehavior, JsonQueryWrapMode wrapMode, SqmToSqlAstConverter converter) {
			final StringBuilder sb = new StringBuilder( baseReadExpression.length() + 200 );
			if ( emptyBehavior == JsonQueryEmptyBehavior.EMPTY_ARRAY || emptyBehavior == JsonQueryEmptyBehavior.EMPTY_OBJECT ) {
				sb.append( "coalesce(" );
			}
			if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
				sb.append( "'['||" );
			}

			sb.append( "stringdecode(regexp_replace(nullif(" );
			sb.append( baseReadExpression );
			sb.append( ",JSON'null'),'^\"(.*)\"$','$1'))");
			if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
				sb.append( "||']'" );
			}
			if ( emptyBehavior == JsonQueryEmptyBehavior.EMPTY_ARRAY ) {
				sb.append( ",'[]')" );
			}
			else if ( emptyBehavior == JsonQueryEmptyBehavior.EMPTY_OBJECT ) {
				sb.append( ",'{}')" );
			}
			return sb.toString();
		}

		protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableExistsColumnDefinition definition, String parentReadExpression, SqmToSqlAstConverter converter) {
			final String baseReadExpression = determineElementReadExpression( definition.name(), definition.jsonPath(), parentReadExpression );
			final String elementReadExpression = parentReadExpression + " is not null and " + baseReadExpression + " is not null";

			addSelectableMapping(
					selectableMappings,
					definition.name(),
					elementReadExpression,
					converter.getCreationContext().getTypeConfiguration().getBasicTypeForJavaType( Boolean.class )
			);
		}

		protected String determineElementReadExpression(String name, @Nullable String jsonPath, String parentReadExpression) {
			return jsonPath == null
					? H2JsonValueFunction.applyJsonPath( parentReadExpression, false, true, "$." + name, null )
					: H2JsonValueFunction.applyJsonPath( parentReadExpression, false, true, jsonPath, null );
		}

		protected void addSelectableMapping(List<SelectableMapping> selectableMappings, String name, String elementReadExpression, JdbcMapping type) {
			selectableMappings.add( new SelectableMappingImpl(
					"",
					name,
					new SelectablePath( name ),
					elementReadExpression,
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
					type
			));
		}
	}
}
