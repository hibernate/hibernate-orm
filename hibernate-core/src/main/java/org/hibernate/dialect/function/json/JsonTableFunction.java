/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmJsonTableFunction;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.JsonTableErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonTableExistsColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableNestedColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableValueColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.JSON;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * Standard json_table function.
 */
public class JsonTableFunction extends AbstractSqmSelfRenderingSetReturningFunctionDescriptor {

	public JsonTableFunction(TypeConfiguration typeConfiguration) {
		this(
				new JsonTableSetReturningFunctionTypeResolver(),
				typeConfiguration
		);
	}

	protected JsonTableFunction(SetReturningFunctionTypeResolver setReturningFunctionTypeResolver, TypeConfiguration typeConfiguration) {
		super(
				"json_table",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.between( 1, 2 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.STRING
				),
				setReturningFunctionTypeResolver,
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, JSON, STRING )
		);
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(List<? extends SqmTypedNode<?>> arguments, QueryEngine queryEngine) {
		//noinspection unchecked
		return new SqmJsonTableFunction<>(
				this,
				this,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				(SqmExpression<?>) arguments.get( 0 ),
				arguments.size() > 1 ? (SqmExpression<String>) arguments.get( 1 ) : null
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		renderJsonTable( sqlAppender, JsonTableArguments.extract( sqlAstArguments ), tupleType, tableIdentifierVariable, walker );
	}

	protected void renderJsonTable(
			SqlAppender sqlAppender,
			JsonTableArguments arguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_table(" );
		arguments.jsonDocument().accept( walker );
		if ( arguments.jsonPath() != null ) {
			sqlAppender.appendSql( ',' );
			arguments.jsonPath().accept( walker );
			final JsonPathPassingClause passingClause = arguments.passingClause();
			if ( passingClause != null ) {
				sqlAppender.appendSql( " passing " );
				final Map<String, Expression> passingExpressions = passingClause.getPassingExpressions();
				final Iterator<Map.Entry<String, Expression>> iterator = passingExpressions.entrySet().iterator();
				Map.Entry<String, Expression> entry = iterator.next();
				entry.getValue().accept( walker );
				sqlAppender.appendSql( " as " );
				sqlAppender.appendDoubleQuoteEscapedString( entry.getKey() );
				while ( iterator.hasNext() ) {
					entry = iterator.next();
					sqlAppender.appendSql( ',' );
					entry.getValue().accept( walker );
					sqlAppender.appendSql( " as " );
					sqlAppender.appendDoubleQuoteEscapedString( entry.getKey() );
				}
			}
		}
		renderColumns( sqlAppender, arguments.columnsClause(), 0, walker );
		// Default behavior is NULL ON ERROR
		if ( arguments.errorBehavior() == JsonTableErrorBehavior.ERROR ) {
			sqlAppender.appendSql( " error on error" );
		}
		sqlAppender.appendSql( ')' );
	}

	protected String determineColumnType(CastTarget castTarget, SqlAstTranslator<?> walker) {
		return determineColumnType( castTarget, walker.getSessionFactory().getTypeConfiguration() );
	}

	protected static String determineColumnType(CastTarget castTarget, TypeConfiguration typeConfiguration) {
		final String columnDefinition = castTarget.getColumnDefinition();
		if ( columnDefinition != null ) {
			return columnDefinition;
		}
		else {
			final String typeName = DdlTypeHelper.getTypeName(
					castTarget.getJdbcMapping(),
					castTarget.toSize(),
					typeConfiguration
			);
			final int parenthesisIndex = typeName.indexOf( '(' );
			if ( parenthesisIndex != -1 && typeName.charAt( parenthesisIndex + 1 ) == '$' ) {
				// Remove length/precision and scale arguments if it contains unresolved variables
				return typeName.substring( 0, parenthesisIndex );
			}
			else {
				return typeName;
			}
		}
	}

	protected int renderColumns(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( " columns" );
		int nextClauseLevel = renderColumnDefinitions( sqlAppender, jsonTableColumnsClause, '(', clauseLevel, walker );
		sqlAppender.appendSql( ')' );
		return nextClauseLevel;
	}

	protected int renderColumnDefinitions(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, char separator, int clauseLevel, SqlAstTranslator<?> walker) {
		int nextClauseLevel = clauseLevel + 1;
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			sqlAppender.appendSql( separator );
			if ( columnDefinition instanceof JsonTableExistsColumnDefinition definition ) {
				renderJsonExistsColumnDefinition( sqlAppender, definition, clauseLevel, walker );
			}
			else if ( columnDefinition instanceof JsonTableQueryColumnDefinition definition ) {
				renderJsonQueryColumnDefinition( sqlAppender, definition, clauseLevel, walker );
			}
			else if ( columnDefinition instanceof JsonTableValueColumnDefinition definition ) {
				renderJsonValueColumnDefinition( sqlAppender, definition, clauseLevel, walker );
			}
			else if ( columnDefinition instanceof JsonTableOrdinalityColumnDefinition definition ) {
				renderJsonOrdinalityColumnDefinition( sqlAppender, definition, clauseLevel, walker );
			}
			else {
				nextClauseLevel = renderJsonNestedColumnDefinition( sqlAppender, (JsonTableNestedColumnDefinition) columnDefinition, nextClauseLevel, walker );
			}
			separator = ',';
		}
		return nextClauseLevel;
	}

	protected int renderJsonNestedColumnDefinition(SqlAppender sqlAppender, JsonTableNestedColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "nested " );
		sqlAppender.appendSingleQuoteEscapedString( definition.jsonPath() );
		return renderColumns( sqlAppender, definition.columns(), clauseLevel, walker );
	}

	protected int countNestedColumnDefinitions(JsonTableColumnsClause jsonTableColumnsClause) {
		int count = 0;
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
				count = count + 1 + countNestedColumnDefinitions( nestedColumnDefinition.columns() );
			}
		}
		return count;
	}

	protected void renderJsonOrdinalityColumnDefinition(SqlAppender sqlAppender, JsonTableOrdinalityColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( " for ordinality" );
	}

	protected void renderJsonValueColumnDefinition(SqlAppender sqlAppender, JsonTableValueColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( definition.type(), walker ) );

		renderColumnPath( definition.name(), definition.jsonPath(), sqlAppender, walker );

		if ( definition.errorBehavior() != null ) {
			if ( definition.errorBehavior() == JsonValueErrorBehavior.ERROR ) {
				sqlAppender.appendSql( " error on error" );
			}
			else if ( definition.errorBehavior() != JsonValueErrorBehavior.NULL ) {
				final Expression defaultExpression = definition.errorBehavior().getDefaultExpression();
				assert defaultExpression != null;
				sqlAppender.appendSql( " default " );
				defaultExpression.accept( walker );
				sqlAppender.appendSql( " on error" );
			}
		}
		if ( definition.emptyBehavior() != null ) {
			if ( definition.emptyBehavior() == JsonValueEmptyBehavior.ERROR ) {
				sqlAppender.appendSql( " error on empty" );
			}
			else if ( definition.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
				final Expression defaultExpression = definition.emptyBehavior().getDefaultExpression();
				assert defaultExpression != null;
				sqlAppender.appendSql( " default " );
				defaultExpression.accept( walker );
				sqlAppender.appendSql( " on empty" );
			}
		}
		// todo: mismatch clause?
	}

	protected void renderColumnPath(String name, @Nullable String jsonPath, SqlAppender sqlAppender, SqlAstTranslator<?> walker) {
		if ( jsonPath != null ) {
			sqlAppender.appendSql( " path " );
			sqlAppender.appendSingleQuoteEscapedString( jsonPath );
		}
		else {
			// Always append implicit path to avoid identifier case sensitivity issues
			sqlAppender.appendSql( " path '$." );
			sqlAppender.appendSql( name );
			sqlAppender.appendSql( '\'' );
		}
	}

	protected void renderJsonQueryColumnDefinition(SqlAppender sqlAppender, JsonTableQueryColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( new CastTarget( definition.type() ), walker ) );
		if ( definition.type().getJdbcType().getDdlTypeCode() != SqlTypes.JSON ) {
			sqlAppender.appendSql( " format json" );
		}

		if ( definition.wrapMode() != null ) {
			switch ( definition.wrapMode() ) {
				case WITH_WRAPPER -> sqlAppender.appendSql( " with wrapper" );
				case WITHOUT_WRAPPER -> sqlAppender.appendSql( " without wrapper" );
				case WITH_CONDITIONAL_WRAPPER -> sqlAppender.appendSql( " with conditional wrapper" );
			}
		}

		renderColumnPath( definition.name(), definition.jsonPath(), sqlAppender, walker );

		if ( definition.errorBehavior() != null ) {
			switch ( definition.errorBehavior() ) {
				case ERROR -> sqlAppender.appendSql( " error on error" );
				case NULL -> sqlAppender.appendSql( " null on error" );
				case EMPTY_OBJECT -> sqlAppender.appendSql( " empty object on error" );
				case EMPTY_ARRAY -> sqlAppender.appendSql( " empty array on error" );
			}
		}

		if ( definition.emptyBehavior() != null ) {
			switch ( definition.emptyBehavior() ) {
				case ERROR -> sqlAppender.appendSql( " error on empty" );
				case NULL -> sqlAppender.appendSql( " null on empty" );
				case EMPTY_OBJECT -> sqlAppender.appendSql( " empty object on empty" );
				case EMPTY_ARRAY -> sqlAppender.appendSql( " empty array on empty" );
			}
		}
	}

	protected void renderJsonExistsColumnDefinition(SqlAppender sqlAppender, JsonTableExistsColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( new CastTarget( definition.type() ), walker ) );

		sqlAppender.appendSql( " exists" );
		renderColumnPath( definition.name(), definition.jsonPath(), sqlAppender, walker );
		final JsonExistsErrorBehavior errorBehavior = definition.errorBehavior();
		if ( errorBehavior != null && errorBehavior != JsonExistsErrorBehavior.FALSE ) {
			if ( errorBehavior == JsonExistsErrorBehavior.TRUE ) {
				sqlAppender.appendSql( " true on error" );
			}
			else {
				sqlAppender.appendSql( " error on error" );
			}
		}
	}

	protected record JsonTableArguments(
			Expression jsonDocument,
			@Nullable Expression jsonPath,
			boolean isJsonType,
			@Nullable JsonPathPassingClause passingClause,
			@Nullable JsonTableErrorBehavior errorBehavior,
			JsonTableColumnsClause columnsClause
	){
		public static JsonTableArguments extract(List<? extends SqlAstNode> sqlAstArguments) {
			final Expression jsonDocument = (Expression) sqlAstArguments.get( 0 );
			Expression jsonPath = null;
			JsonPathPassingClause passingClause = null;
			JsonTableErrorBehavior errorBehavior = null;
			JsonTableColumnsClause columnsClause = null;
			int nextIndex = 1;
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof Expression expression ) {
					jsonPath = expression;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonPathPassingClause jsonPathPassingClause ) {
					passingClause = jsonPathPassingClause;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonTableErrorBehavior jsonTableErrorBehavior) {
					errorBehavior = jsonTableErrorBehavior;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonTableColumnsClause jsonTableColumnsClause ) {
					columnsClause = jsonTableColumnsClause;
				}
			}
			return new JsonTableArguments(
					jsonDocument,
					jsonPath,
					jsonDocument.getExpressionType() != null
							&& jsonDocument.getExpressionType().getSingleJdbcMapping().getJdbcType().isJson(),
					passingClause,
					errorBehavior,
					columnsClause
			);
		}
	}
}
